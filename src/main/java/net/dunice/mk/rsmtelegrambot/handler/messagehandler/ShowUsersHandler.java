package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.BAN;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.NEXT_PAGE;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.PREVIOUS_PAGE;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.UNBAN;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.USERS_LIST;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.entity.Role.SUPER_USER;
import static net.dunice.mk.rsmtelegrambot.entity.Role.USER;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.ShowUsersState.ShowUsersStep.BAN_OR_UNBAN;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.ShowUsersState.ShowUsersStep.HANDLE_USER_ACTION;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.ShowUsersState.ShowUsersStep.SHOW_USERS_LIST;
import static net.dunice.mk.rsmtelegrambot.handler.state.stateobject.ShowUsersState.ShowUsersStep.SHOW_USER_DETAILS;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.ShowUsersState;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
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
public class ShowUsersHandler implements MessageHandler {

    private final UserRepository userRepository;
    private final MenuGenerator menuGenerator;
    private final Map<Long, BasicState> basicStates;
    private final Map<Long, ShowUsersState> showUsersStates;
    private final EnumMap<Menu, ReplyKeyboard> menus;
    private static final String USER_INFO_TEMPLATE = """
        Телеграм ID: %s
        ФИО: %s
        Номер телефона: %s
        Дата рождения: %s
        Информация: %s
        Забанен: %s
        Роль: %s
        """;

    @Override
    public BasicState getState() {
        return BasicState.SHOW_USERS;
    }

    @Override
    public SendMessage handle(MessageDto messageDto, Long telegramId) {
        String text = messageDto.getText();
        ShowUsersState state = showUsersStates.get(telegramId);

        if (state == null) {
            showUsersStates.put(telegramId, (state = new ShowUsersState()));
        }

        return switch (state.getStep()) {
            case SHOW_USERS_LIST -> {
                if (state.getUsersToDisplay() == null) {
                    state.setUsersToDisplay(userRepository.findAll(Sort.by(Sort.Direction.ASC, "fullName")));
                    state.setAllUsers(state.getUsersToDisplay());
                }
                state.setStep(SHOW_USER_DETAILS);
                yield generateSendMessage(telegramId,
                    """
                        Выберите пользователя из списка,
                        либо введите часть ФИО для уточнения поиска
                        (минимум 3 символа) :
                        """,
                    generateUserListKeyboard(state.getUsersToDisplay(), state.getPage()));
            }
            case SHOW_USER_DETAILS -> {
                if (TO_MAIN_MENU.equalsIgnoreCase(text)) {
                    yield goToMainMenu(telegramId);
                }
                else if (PREVIOUS_PAGE.equalsIgnoreCase(text)) {
                    state.decrementPage();
                    state.setStep(SHOW_USERS_LIST);
                    yield handle(messageDto, telegramId);
                }
                else if (NEXT_PAGE.equalsIgnoreCase(text)) {
                    state.incrementPage();
                    state.setStep(SHOW_USERS_LIST);
                    yield handle(messageDto, telegramId);
                }
                else if (Pattern.compile("^[а-яА-ЯёЁ]+$").matcher(text).matches()) {
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
                                generateUserListKeyboard(state.getUsersToDisplay(), state.getPage()));
                        } else {
                            yield generateSendMessage(telegramId,
                                "Не найдено ни одного пользователя по заданному фильтру, повторите ввод: ",
                                generateUserListKeyboard(state.getUsersToDisplay(), state.getPage()));
                        }
                    } else {
                        yield generateSendMessage(telegramId,
                            """
                                Необходимо ввести минимум 3 символа для фильтрации пользователей.
                                Повторите ввод либо выберите пользователя из списка:
                                """,
                            generateUserListKeyboard(state.getUsersToDisplay(), state.getPage()));
                    }
                }
                else {
                    try {
                        Long userTgId = Long.valueOf(text.substring(text.lastIndexOf(' ') + 1));
                        Optional<User> userOptional = userRepository.findById(userTgId);
                        if (userOptional.isPresent()) {
                            User targetUser = userOptional.get();
                            String userDescription = getUserDescription(targetUser);
                            state.setTargetUser(targetUser);
                            state.setStep(HANDLE_USER_ACTION);
                            yield generateSendMessage(telegramId, userDescription, generateUserActionKeyboard());
                        } else {
                            yield generateSendMessage(telegramId, "Пользователь не найден",
                                menus.get(GO_TO_MAIN_MENU));
                        }
                    } catch (NumberFormatException e) {
                        yield generateSendMessage(telegramId, "Неверные данные, выберите пользователя из списка или повторите ввод: ");
                    }
                }
            }
            case HANDLE_USER_ACTION -> {
                if (TO_MAIN_MENU.equalsIgnoreCase(text)) {
                    yield goToMainMenu(telegramId);
                } else if (USERS_LIST.equalsIgnoreCase(text)) {
                    state.setStep(SHOW_USERS_LIST);
                    state.setUsersToDisplay(state.getAllUsers());
                    state.setPage(0);
                    yield handle(messageDto, telegramId);
                } else if (StringUtils.equalsAny(text, BAN, UNBAN)) {
                    state.setStep(BAN_OR_UNBAN);
                    yield handle(messageDto, telegramId);
                } else {
                    yield goToMainMenu(telegramId);
                }
            }

            case BAN_OR_UNBAN -> {
                if (TO_MAIN_MENU.equalsIgnoreCase(text)) {
                    yield goToMainMenu(telegramId);
                }
                User targetUser = state.getTargetUser();
                if (telegramId.equals(targetUser.getTelegramId())) {
                    yield generateSendMessage(telegramId,
                        "Команда отклонена, вы не можете забанить/разбанить самого себя.",
                        menus.get(GO_TO_MAIN_MENU));
                }
                if (BAN.equals(text)) {
                    if (targetUser.getUserRole() != USER &&
                        userRepository.findById(telegramId).get().getUserRole() != SUPER_USER) {
                        yield generateSendMessage(telegramId,
                            "Команда на бан пользователя '%s' отклонена, т.к пользователь имеет роль '%s'.".formatted(
                                targetUser.getFullName(), targetUser.getUserRole()), menus.get(GO_TO_MAIN_MENU));
                    } else {
                        targetUser.setBanned(true);
                        targetUser.setUserRole(USER);
                        userRepository.save(targetUser);
                        basicStates.remove(targetUser.getTelegramId());
                        yield generateSendMessage(telegramId,
                            String.format("Пользователь '%s' забанен.", targetUser.getFullName()),
                            menus.get(GO_TO_MAIN_MENU));
                    }
                } else {
                    if (!targetUser.isBanned()) {
                        yield generateSendMessage(telegramId, "Данный пользователь не находится в бан листе.",
                            menus.get(GO_TO_MAIN_MENU));
                    } else {
                        targetUser.setBanned(false);
                        userRepository.save(targetUser);
                        yield generateSendMessage(telegramId,
                            "Пользователь '%s' разбанен.".formatted(targetUser.getFullName()),
                            menus.get(GO_TO_MAIN_MENU));
                    }
                }
            }
        };
    }

    private ReplyKeyboardMarkup generateUserListKeyboard(List<User> users, int page) {
        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add(TO_MAIN_MENU);
        keyboard.add(firstRow);
        int startIndex = page * 10;
        int endIndex = Math.min(startIndex + 10, users.size());
        for (int i = startIndex; i < endIndex; i++) {
            KeyboardRow row = new KeyboardRow();
            User user = users.get(i);
            row.add("%s | Номер билета: %s | TelegramID: %s".formatted(user.getFullName(), user.getUserCard(),
                user.getTelegramId()));
            keyboard.add(row);
        }
        KeyboardRow pagesRow = new KeyboardRow();
        if (startIndex > 0) {
            pagesRow.add(PREVIOUS_PAGE);
        }
        if (endIndex < users.size()) {
            pagesRow.add(NEXT_PAGE);
        }
        keyboard.add(pagesRow);
        replyMarkup.setKeyboard(keyboard);
        replyMarkup.setResizeKeyboard(true);
        replyMarkup.setOneTimeKeyboard(false);
        return replyMarkup;
    }

    private ReplyKeyboardMarkup generateUserListKeyboard(List<User> users) {
        ReplyKeyboardMarkup replyMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow firstRow = new KeyboardRow();
        firstRow.add(TO_MAIN_MENU);
        keyboard.add(firstRow);
        for (User user : users) {
            KeyboardRow row = new KeyboardRow();
            row.add("%s | Номер билета: %s | TelegramID: %s".formatted(user.getFullName(), user.getUserCard(),
                user.getTelegramId()));
            keyboard.add(row);
        }
        replyMarkup.setKeyboard(keyboard);
        replyMarkup.setResizeKeyboard(true);
        replyMarkup.setOneTimeKeyboard(false);
        return replyMarkup;
    }

    private ReplyKeyboard generateUserActionKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton toMainMenuButton = new InlineKeyboardButton(TO_MAIN_MENU);
        toMainMenuButton.setCallbackData(toMainMenuButton.getText());
        InlineKeyboardButton toUsersButton = new InlineKeyboardButton(USERS_LIST);
        toUsersButton.setCallbackData(toUsersButton.getText());
        keyboard.add(List.of(toMainMenuButton));
        keyboard.add(List.of(toUsersButton));
        InlineKeyboardButton banButton = new InlineKeyboardButton(BAN);
        banButton.setCallbackData(banButton.getText());
        InlineKeyboardButton unbanButton = new InlineKeyboardButton(UNBAN);
        unbanButton.setCallbackData(unbanButton.getText());
        keyboard.add(List.of(banButton, unbanButton));
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    private SendMessage goToMainMenu(Long telegramId) {
        showUsersStates.remove(telegramId);
        basicStates.put(telegramId, BasicState.IN_MAIN_MENU);
        return menuGenerator.generateRoleSpecificMainMenu(telegramId,
            userRepository.findByTelegramId(telegramId).get().getUserRole());
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
}

