package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TRY_AGAIN;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.TRY_AGAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.USER_REGISTRATION;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.UserRegistrationState.UserRegistrationStep.CHECK_CONFIRMATION;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.UserRegistrationState.UserRegistrationStep.REQUEST_MEMBERSHIP_NUMBER;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.UserRegistrationState.UserRegistrationStep.RETRY_REGISTRATION;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.UserRegistrationState.UserRegistrationStep.VALIDATE_INFO;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.UserRegistrationState.UserRegistrationStep.VALIDATE_MEMBERSHIP_NUMBER;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Role;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.UserRegistrationState;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import net.dunice.mk.rsmtelegrambot.service.GoogleSheetDownloader;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserRegistrationHandler implements MessageHandler {
    private final Map<Long, UserRegistrationState> userRegistrationStates;
    private final Map<Long, BasicState> basicStates;
    private final EnumMap<Menu, ReplyKeyboard> menus;
    private final MenuGenerator menuGenerator;
    private final UserRepository userRepository;
    private final GoogleSheetDownloader sheetDownloader;
    private final static String USER_DATA_TEMPLATE = """
        ФИО: %s
        Дата рождения: %s
        Номер телефона: %s
        Номер членского билета: %s
        """;


    @Override
    public SendMessage handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();
        UserRegistrationState state = userRegistrationStates.get(telegramId);
        if (state == null) {
            userRegistrationStates.put(telegramId, (state = new UserRegistrationState()));
        }

        if (TO_MAIN_MENU.equalsIgnoreCase(text)) {
            userRegistrationStates.remove(telegramId);
            basicStates.put(telegramId, IN_MAIN_MENU);
            return menuGenerator.generateRoleSpecificMainMenu(telegramId,
                userRepository.findByTelegramId(telegramId).get().getUserRole());
        }

        return switch (state.getStep()) {
            case REQUEST_MEMBERSHIP_NUMBER -> {
                state.setStep(VALIDATE_MEMBERSHIP_NUMBER);
                yield generateSendMessage(telegramId, "Введите номер членского билета РСМ:");
            }
            case VALIDATE_MEMBERSHIP_NUMBER -> {
                List<String[]> sheet = sheetDownloader.downloadSheet();
                String[] userRow = findUserRowByMembershipNumber(sheet, text);
                if (userRow == null) {
                    state.setStep(RETRY_REGISTRATION);
                    yield generateSendMessage(telegramId,
                        "Член РСМ не найден, если вы ввели неправильной номер попробуйте повторить регистрацию," +
                        " если же номер правильный обратитесь к своему руководителю РСМ для актуализации данных в таблице членов РСМ.",
                        menus.get(TRY_AGAIN_MENU));
                } else {
                    fillRegistrationState(state, userRow);
                    state.setStep(CHECK_CONFIRMATION);
                    yield generateSendMessage(telegramId, "Данные верны?\n" + getUserData(state),
                        menus.get(SELECTION_MENU));
                }
            }
            case CHECK_CONFIRMATION -> {
                if ("Да".equalsIgnoreCase(text)) {
                    state.setStep(VALIDATE_INFO);
                    yield generateSendMessage(telegramId, "Введите дополнительное описание (до 255 символов):");
                } else if ("Нет".equalsIgnoreCase(text)) {
                    state.setStep(VALIDATE_INFO);
                    yield generateSendMessage(telegramId,
                        "Если данные неверны обратитесь к своему руководителю РСМ для актуализации данных в таблице членов РСМ.");
                } else {
                    state.setStep(RETRY_REGISTRATION);
                    yield generateSendMessage(telegramId, "Неверная команда, регистрация отменена",
                        menus.get(TRY_AGAIN_MENU));
                }
            }
            case VALIDATE_INFO -> {
                if (text.length() <= 255) {
                    state.setInfo(text);
                    saveUser(state, telegramId);
                    yield generateSendMessage(telegramId, "Вы успешно зарегистрированы!", menus.get(GO_TO_MAIN_MENU));
                } else {
                    state.setStep(RETRY_REGISTRATION);
                    yield generateSendMessage(telegramId, "Описание слишком длинное, регистрация отменена.",
                        menus.get(TRY_AGAIN_MENU));
                }
            }
            case RETRY_REGISTRATION -> {
                if (TRY_AGAIN.equalsIgnoreCase(text)) {
                    state.setStep(REQUEST_MEMBERSHIP_NUMBER);
                    yield handle(messageDto, telegramId);
                } else {
                    yield generateSendMessage(telegramId, "Неверная команда");
                }
            }
        };
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
    }

    private String[] findUserRowByMembershipNumber(List<String[]> sheet, String text) {
        String[] targetRow = null;
        for (String[] row : sheet) {
            String membershipNumber = row[5].strip();
            if (membershipNumber.equals(text.strip())) {
                targetRow = row;
                break;
            }
        }
        return targetRow;
    }

    private void fillRegistrationState(UserRegistrationState state, String[] row) {
        String lastName = row[1];
        String firstName = row[2];
        String patronymic = row[3];
        String fullName = lastName + " " + firstName + " " + patronymic;
        String phoneNumber = row[4];
        Integer membershipNumber = Integer.parseInt(row[5].strip());
        LocalDate birthDate = LocalDate.parse(row[6], DateTimeFormatter.ofPattern("dd.MM.yyyy"));
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

    @Override
    public BasicState getState() {
        return USER_REGISTRATION;
    }

    private String getHiddenPhoneNumber(UserRegistrationState state) {
        StringBuilder hiddenNumber = new StringBuilder(state.getPhoneNumber());
        int replacedDigitsNum = 0;
        for (int i = 1; i < state.getPhoneNumber().length() && replacedDigitsNum < 5; i++) {
            if (Character.isDigit(hiddenNumber.charAt(i))) {
                hiddenNumber.setCharAt(i, '*');
                replacedDigitsNum++;
            }
        }
        return hiddenNumber.toString();
    }

    private String getHiddenBirthDate(UserRegistrationState state) {
        String birthDate = state.getBirthDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        birthDate = birthDate.substring(0, birthDate.length() - 4);
        birthDate = birthDate + "*".repeat(4);
        return birthDate;
    }

    private String getHiddenFullName(UserRegistrationState state) {
        return state.getLastName() + " " + state.getFirstName() + " " + "*".repeat(state.getPatronymic().length());
    }

    private String getHiddenMembershipNumber(UserRegistrationState state) {
        String membershipNumber = state.getMembershipNumber().toString();
        membershipNumber = membershipNumber.substring(membershipNumber.length() - 3);
        membershipNumber = "*".repeat(3) + membershipNumber;
        return membershipNumber;
    }

    private SendMessage goToMainMenu(Long telegramId) {
        userRegistrationStates.remove(telegramId);
        basicStates.put(telegramId, IN_MAIN_MENU);
        return menuGenerator.generateRoleSpecificMainMenu(telegramId,
            userRepository.findByTelegramId(telegramId).get().getUserRole());
    }
}
