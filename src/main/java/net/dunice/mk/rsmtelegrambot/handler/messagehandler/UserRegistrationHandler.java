package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.CANCEL;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.CANCEL_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.USER_REGISTRATION;
import static net.dunice.mk.rsmtelegrambot.handler.state.UserRegistrationState.UserRegistrationStep.FINISH;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Role;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.UserRegistrationState;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import net.dunice.mk.rsmtelegrambot.service.GoogleSheetDownloader;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserRegistrationHandler implements MessageHandler {

    private static final String USER_DATA_TEMPLATE = """
        Данные верны?
        ⬇
        ФИО: %s
        Дата рождения: %s
        Номер телефона: %s
        Номер членского билета: %s
        """;

    private final Map<Long, UserRegistrationState> userRegistrationStates;
    private final Map<Long, BasicState> basicStates;
    private final EnumMap<Menu, ReplyKeyboard> menus;
    private final MenuGenerator menuGenerator;
    private final UserRepository userRepository;
    private final GoogleSheetDownloader sheetDownloader;

    @Override
    public BasicStep getStep() {
        return USER_REGISTRATION;
    }

    @Override
    public SendMessage handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();

        UserRegistrationState state = userRegistrationStates.get(telegramId);
        if (state == null) {
            state = new UserRegistrationState();
            userRegistrationStates.put(telegramId, state);
        }

        if (CANCEL.equals(text)) {
            basicStates.remove(telegramId);
            userRegistrationStates.remove(telegramId);
            return generateSendMessage(telegramId,
                "Регистрация отменена, введите /start, если хотите попробовать заново:");
        }

        return switch (state.getStep()) {
            case REQUEST_MEMBERSHIP_NUMBER -> handleRequestMembershipNumber(telegramId, state);
            case VALIDATE_MEMBERSHIP_NUMBER -> handleValidateMembershipNumber(text, telegramId, state);
            case CHECK_CONFIRMATION        -> handleCheckConfirmation(text, telegramId);
            case VALIDATE_INFO             -> handleValidateInfo(text, telegramId, state);
            case FINISH                    -> handleFinish(text, telegramId);
        };
    }

    private SendMessage handleRequestMembershipNumber(Long telegramId, UserRegistrationState state) {
        state.setStep(UserRegistrationState.UserRegistrationStep.VALIDATE_MEMBERSHIP_NUMBER);
        return generateSendMessage(
            telegramId,
            "Введите номер членского билета РСМ:",
            menus.get(CANCEL_MENU)
        );
    }

    private SendMessage handleValidateMembershipNumber(String text,
                                                       Long telegramId,
                                                       UserRegistrationState state) {

        if(text == null) {
            return handleRequestMembershipNumber(telegramId, state);
        }

        List<String[]> sheet = sheetDownloader.downloadSheet();
        String[] userRow = findUserRowByMembershipNumber(sheet, text);

        if (userRow != null) {
            try {
                fillRegistrationState(state, userRow);
                state.setStep(UserRegistrationState.UserRegistrationStep.CHECK_CONFIRMATION);
                return generateSendMessage(
                    telegramId,
                    getUserData(state),
                    menus.get(SELECTION_MENU)
                );
            } catch (Exception e) {
                return generateSendMessage(
                    telegramId,
                    """
                        Не верный формат данных о пользователе.
                        Обратитесь к своему руководителю РСМ для исправления данных в таблице членов РСМ.
                        """,
                    menus.get(CANCEL_MENU)
                );
            }
        }
        else {
            return generateSendMessage(
                telegramId,
                """
                    Член РСМ не найден.
                    Если вы ввели правильный номер, обратитесь к своему руководителю РСМ для актуализации данных в таблице членов РСМ.
                    Если же номер неправильный — повторите ввод с правильным номером:
                    """,
                menus.get(CANCEL_MENU)
            );
        }
    }

    private SendMessage handleCheckConfirmation(String text, Long telegramId) {
        if ("Да".equalsIgnoreCase(text)) {
            userRegistrationStates.get(telegramId)
                .setStep(UserRegistrationState.UserRegistrationStep.VALIDATE_INFO);

            return generateSendMessage(
                telegramId,
                "Введите дополнительное описание (до 250 символов):",
                menus.get(CANCEL_MENU)
            );
        } else if ("Нет".equalsIgnoreCase(text)) {
            basicStates.remove(telegramId);
            userRegistrationStates.remove(telegramId);
            return generateSendMessage(telegramId,
                """
                    Регистрация отменена.
                    Если данные неверны, обратитесь к своему руководителю РСМ для актуализации данных в таблице членов РСМ.
                    Или введите /start, если хотите попробовать заново:
                    """);
        } else {
            return generateSendMessage(
                telegramId,
                "Неверная команда, выберите 'Да' или 'Нет'",
                menus.get(SELECTION_MENU)
            );
        }
    }

    private SendMessage handleValidateInfo(String text,
                                           Long telegramId,
                                           UserRegistrationState state) {
        if (text != null && text.length() <= 250) {
            state.setInfo(text);
            saveUser(state, telegramId);

            state.setStep(FINISH);
            return generateSendMessage(
                telegramId,
                "Вы успешно зарегистрированы!",
                menus.get(GO_TO_MAIN_MENU)
            );
        } else {
            return generateSendMessage(
                telegramId,
                "Описание слишком длинное, повторите ввод:",
                menus.get(CANCEL_MENU)
            );
        }
    }

    private SendMessage handleFinish(String text, Long telegramId) {
        if (TO_MAIN_MENU.equalsIgnoreCase(text)) {
            return goToMainMenu(telegramId);
        }
        return generateSendMessage(
            telegramId,
            "Неверная команда.",
            menus.get(GO_TO_MAIN_MENU)
        );
    }

    private void saveUser(UserRegistrationState state, long telegramId) {
        User user = new User();
        user.setTelegramId(telegramId);
        user.setFullName(state.getFullName());
        user.setName(state.getFirstName());
        user.setUserCard(state.getMembershipNumber());
        user.setPhoneNumber(state.getPhoneNumber());
        user.setInfo(state.getInfo());
        user.setUserRole(Role.USER);
        user.setBirthDate(state.getBirthDate());
        userRepository.save(user);
        basicStates.get(telegramId).setUser(user);
    }

    private String[] findUserRowByMembershipNumber(List<String[]> sheet, String text) {
        for (String[] row : sheet) {
            String membershipNumber = row[5].strip();
            if (membershipNumber.equals(text.strip())) {
                return row;
            }
        }
        return null;
    }

    private void fillRegistrationState(UserRegistrationState state, String[] row) {
        String lastName = row[1];
        String firstName = row[2];
        String patronymic = row[3];
        String phoneNumber = row[4];
        String fullName = lastName + " " + firstName + " " + patronymic;
        Integer membershipNumber = Integer.parseInt(row[5].strip());
        String birthDate = row[6];

        state.setFullName(fullName);
        state.setFirstName(firstName);
        state.setLastName(lastName);
        state.setPatronymic(patronymic);
        state.setPhoneNumber(phoneNumber);
        state.setMembershipNumber(membershipNumber);
        state.setBirthDate(birthDate);
    }

    private String getUserData(UserRegistrationState state) {
        String fullName = getHiddenFullName(state);
        String phoneNumber = getHiddenPhoneNumber(state);
        String birthDate = getHiddenBirthDate(state);
        String membershipNumber = getHiddenMembershipNumber(state);
        return USER_DATA_TEMPLATE.formatted(fullName, birthDate, phoneNumber, membershipNumber);
    }

    private String getHiddenPhoneNumber(UserRegistrationState state) {
        String phoneNumber = state.getPhoneNumber();
        if (isEmptyOrHasLetter(phoneNumber)) return phoneNumber;
        else {
            StringBuilder hiddenNumber = new StringBuilder(state.getPhoneNumber());
            int replaced = 0;
            int i = hiddenNumber.charAt(0) == '+' ? 2 : 1;
            while (i < hiddenNumber.length() && replaced < 5) {
                if (Character.isDigit(hiddenNumber.charAt(i))) {
                    hiddenNumber.setCharAt(i, '*');
                    replaced++;
                }
                i++;
            }
            return hiddenNumber.toString();
        }
    }

    private String getHiddenBirthDate(UserRegistrationState state) {
        String birthDate = state.getBirthDate();
        if (isEmptyOrHasLetter(birthDate)) return birthDate;
        else return birthDate.substring(0, birthDate.length() - 4)
            + "*".repeat(4);
    }

    private String getHiddenFullName(UserRegistrationState state) {
        return state.getLastName() + " " + state.getFirstName() + " "
            + "*".repeat(state.getPatronymic().length());
    }

    private String getHiddenMembershipNumber(UserRegistrationState state) {
        String membershipNumber = state.getMembershipNumber().toString();
        if (isEmptyOrHasLetter(membershipNumber)) return membershipNumber;
        else return "*".repeat(3)
                    + membershipNumber.substring(membershipNumber.length() - 3);
    }

    private boolean isEmptyOrHasLetter(String text) {
        return text.isEmpty() || text.matches("\".*[a-zA-Zа-яА-ЯёЁ].*\"");
    }

    private SendMessage goToMainMenu(Long telegramId) {
        BasicState state = basicStates.get(telegramId);
        userRegistrationStates.remove(telegramId);
        state.setStep(IN_MAIN_MENU);
        return menuGenerator.generateRoleSpecificMainMenu(telegramId, state.getUser().getUserRole());
    }
}
