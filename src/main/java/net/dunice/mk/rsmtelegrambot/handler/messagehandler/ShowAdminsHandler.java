package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.ADMINS_LIST;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.GRANT_ADMIN;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.NEXT_PAGE;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.PREVIOUS_PAGE;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.REVOKE_ADMIN;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.USERS_LIST;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.entity.Role.ADMIN;
import static net.dunice.mk.rsmtelegrambot.entity.Role.SUPER_USER;
import static net.dunice.mk.rsmtelegrambot.entity.Role.USER;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.BasicStep.SHOW_ADMINS;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowAdminsState.ShowAdminsStep.FINISH;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowAdminsState.ShowAdminsStep.GRANT_OR_REVOKE_ADMIN_RIGHTS;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowAdminsState.ShowAdminsStep.HANDLE_USER_ACTION;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowAdminsState.ShowAdminsStep.SHOW_ADMINS_LIST;
import static net.dunice.mk.rsmtelegrambot.handler.state.ShowAdminsState.ShowAdminsStep.SHOW_ADMIN_DETAILS;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Role;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.ShowAdminsState;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ShowAdminsHandler implements MessageHandler {

    private static final String USER_INFO_TEMPLATE = """
        Телеграм ID: %s
        ФИО: %s
        Номер телефона: %s
        Дата рождения: %s
        Информация: %s
        Забанен: %s
        Роль: %s
        """;

    private final UserRepository userRepository;
    private final MenuGenerator menuGenerator;
    private final Map<Long, BasicState> basicStates;
    private final Map<Long, ShowAdminsState> showAdminStates;
    private final EnumMap<Menu, ReplyKeyboard> menus;

    @Override
    public BasicStep getStep() {
        return SHOW_ADMINS;
    }

    @Override
    public SendMessage handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();
        ShowAdminsState state = showAdminStates.get(telegramId);
        if (state == null) {
            showAdminStates.put(telegramId, (state = new ShowAdminsState()));
        }

        if (TO_MAIN_MENU.equalsIgnoreCase(text)) {
            return goToMainMenu(telegramId);
        }

        return switch (state.getStep()) {
            case SHOW_ADMINS_LIST -> {
                state.setStep(SHOW_ADMIN_DETAILS);
                List<User> admins = userRepository.findAllByUserRole(ADMIN);
                yield generateSendMessage(telegramId, "Список администраторов:", generateAdminListKeyboard(admins));
            }
            case SHOW_ADMIN_DETAILS -> {
                if (TO_MAIN_MENU.equalsIgnoreCase(text)) {
                    yield goToMainMenu(telegramId);
                } else if (PREVIOUS_PAGE.equalsIgnoreCase(text)) {
                    state.decrementPage();
                    state.setStep(SHOW_ADMINS_LIST);
                    yield handle(messageDto, telegramId);
                } else if (NEXT_PAGE.equalsIgnoreCase(text)) {
                    state.incrementPage();
                    state.setStep(SHOW_ADMINS_LIST);
                    yield handle(messageDto, telegramId);
                } else if (Pattern.compile("^[а-яА-ЯёЁ]+$").matcher(text).matches()) {
                    if (text.length() > 2) {
                        List<User> users = filterUsersByFullNamePart(state.getAllUsers(), text);
                        if (!users.isEmpty()) {
                            state.setUsersToDisplay(users);
                            state.setPage(0);
                            yield generateSendMessage(telegramId,
                                """
                                    Найдено пользователей - (%s).
                                    Выберите пользователя из списка,
                                    либо введите часть ФИО для уточнения поиска
                                    (минимум 3 символа) :
                                    """.formatted(users.size()),
                                generateAdminListKeyboard(state.getUsersToDisplay()));
                        } else {
                            yield generateSendMessage(telegramId,
                                "Не найдено ни одного пользователя по заданному фильтру, повторите ввод: ",
                                generateAdminListKeyboard(state.getUsersToDisplay()));
                        }
                    } else {
                        yield generateSendMessage(telegramId,
                            """
                                Необходимо ввести минимум 3 символа для фильтрации пользователей.
                                Повторите ввод либо выберите пользователя из списка:
                                """,
                            generateAdminListKeyboard(state.getUsersToDisplay()));
                    }
                } else {
                    try {
                        Long userTgId = Long.valueOf(text.substring(text.lastIndexOf(' ') + 1));
                        Optional<User> userOptional = userRepository.findById(userTgId);
                        if (userOptional.isPresent()) {
                            User targetUser = userOptional.get();
                            String userDescription = getUserDescription(targetUser);
                            state.setTargetUser(targetUser);
                            state.setStep(HANDLE_USER_ACTION);
                            yield generateSendMessage(telegramId, userDescription,
                                generateUserActionKeyboard(basicStates.get(telegramId).getUser().getUserRole(),
                                    targetUser));
                        } else {
                            yield generateSendMessage(telegramId, "Пользователь не найден",
                                menus.get(GO_TO_MAIN_MENU));
                        }
                    } catch (NumberFormatException e) {
                        yield generateSendMessage(telegramId,
                            "Неверные данные, выберите пользователя из списка или повторите ввод: ");
                    }
                }
            }
            case HANDLE_USER_ACTION -> {
                if (TO_MAIN_MENU.equalsIgnoreCase(text)) {
                    yield goToMainMenu(telegramId);
                } else if (USERS_LIST.equalsIgnoreCase(text)) {
                    state.setStep(SHOW_ADMINS_LIST);
                    state.setUsersToDisplay(state.getAllUsers());
                    state.setPage(0);
                    yield handle(messageDto, telegramId);
                } else if (StringUtils.equalsAny(text, GRANT_ADMIN, REVOKE_ADMIN)) {
                    state.setStep(GRANT_OR_REVOKE_ADMIN_RIGHTS);
                    yield handle(messageDto, telegramId);
                } else {
                    yield goToMainMenu(telegramId);
                }
            }
            case GRANT_OR_REVOKE_ADMIN_RIGHTS -> {
                state.setStep(FINISH);
                User targetUser = state.getTargetUser();
                User currentUser = basicStates.get(telegramId).getUser();
                if (currentUser.getUserRole() != SUPER_USER) {
                    yield generateSendMessage(telegramId,
                        "Команда на назначение/отстранение админа доступна только для суперпользователя",
                        menus.get(GO_TO_MAIN_MENU));
                } else {
                    if (GRANT_ADMIN.equalsIgnoreCase(text)) {
                        String response;
                        if (targetUser.isBanned()) {
                            response =
                                "Невозможно дать права администратора пользователю %s, так как он находится в бан листе."
                                    .formatted(targetUser.getFullName());
                        } else {
                            targetUser.setUserRole(ADMIN);
                            userRepository.save(targetUser);
                            if (telegramId.equals(targetUser.getTelegramId())) {
                                currentUser.setUserRole(ADMIN);
                                response = """
                                    Вы понизили свои права до администратора, если вы сделали это по ошибке используйте команду /sustart, чтобы восстановить права.
                                    Или же сообщите эту команду другому пользователю который должен стать суперпользователем.
                                    """;
                            } else {
                                response = "Пользователю '%s' были даны права администратора."
                                    .formatted(targetUser.getFullName());
                            }
                        }
                        yield generateSendMessage(telegramId, response, menus.get(GO_TO_MAIN_MENU));
                    } else {
                        targetUser.setUserRole(USER);
                        userRepository.save(targetUser);
                        yield generateSendMessage(telegramId, "У пользователя '%s' были отозваны права администратора."
                                .formatted(targetUser.getFullName()),
                            menus.get(GO_TO_MAIN_MENU));
                    }
                }
            }
            case FINISH -> {
                if (TO_MAIN_MENU.equalsIgnoreCase(text)) {
                    yield goToMainMenu(telegramId);
                } else {
                    yield generateSendMessage(telegramId, "Неверная команда", menus.get(GO_TO_MAIN_MENU));
                }
            }
        };
    }

    private ReplyKeyboardMarkup generateAdminListKeyboard(List<User> admins) {
        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add(TO_MAIN_MENU);
        keyboard.add(firstRow);
        admins.sort(Comparator.comparing(User::getFullName));
        for (User admin : admins) {
            KeyboardRow row = new KeyboardRow();
            row.add("%s | Номер билета: %s | TelegramID: %s".formatted(admin.getFullName(), admin.getUserCard(),
                admin.getTelegramId()));
            keyboard.add(row);
        }
        replyMarkup.setKeyboard(keyboard);
        replyMarkup.setResizeKeyboard(true);
        replyMarkup.setOneTimeKeyboard(false);
        return replyMarkup;
    }

    private SendMessage goToMainMenu(Long telegramId) {
        BasicState state = basicStates.get(telegramId);
        showAdminStates.remove(telegramId);
        state.setStep(IN_MAIN_MENU);
        return menuGenerator.generateRoleSpecificMainMenu(telegramId,
            state.getUser().getUserRole());
    }

    private String getUserDescription(User targetUser) {
        return USER_INFO_TEMPLATE.formatted(
            targetUser.getTelegramId(),
            targetUser.getFullName(),
            targetUser.getPhoneNumber(),
            targetUser.getBirthDate(),
            targetUser.getInfo(),
            targetUser.isBanned() ? "Да" : "Нет",
            targetUser.getUserRole()
        );
    }

    private List<User> filterUsersByFullNamePart(List<User> allUsers, String fullNamePart) {
        return allUsers.stream()
            .filter(user -> StringUtils.containsIgnoreCase(user.getFullName(), fullNamePart))
            .sorted(Comparator.comparing(User::getFullName))
            .toList();
    }

    private ReplyKeyboard generateUserActionKeyboard(Role role, User targetUser) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton toMainMenuButton = new InlineKeyboardButton(TO_MAIN_MENU);
        toMainMenuButton.setCallbackData(toMainMenuButton.getText());
        InlineKeyboardButton toUsersButton = new InlineKeyboardButton(ADMINS_LIST);
        toUsersButton.setCallbackData(toUsersButton.getText());
        keyboard.add(List.of(toMainMenuButton, toUsersButton));
        if (role == SUPER_USER) {
            if (targetUser.getUserRole() != ADMIN) {
                InlineKeyboardButton grantAdminButton = new InlineKeyboardButton(GRANT_ADMIN);
                grantAdminButton.setCallbackData(grantAdminButton.getText());
                keyboard.add(List.of(grantAdminButton));
            } else {
                InlineKeyboardButton revokeAdminButton = new InlineKeyboardButton(REVOKE_ADMIN);
                revokeAdminButton.setCallbackData(revokeAdminButton.getText());
                keyboard.add(List.of(revokeAdminButton));
            }
        }
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }
}

