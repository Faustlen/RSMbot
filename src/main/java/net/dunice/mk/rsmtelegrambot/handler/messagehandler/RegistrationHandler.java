package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.ButtonName.TRY_AGAIN;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.GO_TO_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.SELECTION_MENU;
import static net.dunice.mk.rsmtelegrambot.constant.Menu.TRY_AGAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.IN_MAIN_MENU;
import static net.dunice.mk.rsmtelegrambot.handler.state.BasicState.REGISTRATION;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.RegistrationStep.CONFIRM;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.RegistrationStep.FINISH;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.RegistrationStep.FULL_NAME;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.RegistrationStep.INFO;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.RegistrationStep.PHONE_NUMBER;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.RegistrationStep.REQUEST_REGISTRATION;
import static net.dunice.mk.rsmtelegrambot.handler.state.step.RegistrationStep.RETRY_REGISTRATION;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.constant.Menu;
import net.dunice.mk.rsmtelegrambot.entity.Role;
import net.dunice.mk.rsmtelegrambot.entity.User;
import net.dunice.mk.rsmtelegrambot.handler.MenuGenerator;
import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.RegistrationState;
import net.dunice.mk.rsmtelegrambot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegistrationHandler implements MessageHandler {
    private final Map<Long, RegistrationState> registrationStates;
    private final Map<Long, BasicState> basicStates;
    private static int currentUserCard = 1;
    private final EnumMap<Menu, ReplyKeyboard> menus;
    private final MenuGenerator menuGenerator;
    private final UserRepository userRepository;


    @Override
    public SendMessage handle(String message, Long telegramId) {
        RegistrationState state = registrationStates.get(telegramId);
        if (state == null) {
            registrationStates.put(telegramId, (state = new RegistrationState()));
        }
        return switch (state.getStep()) {
            case REQUEST_REGISTRATION -> {
                state.setStep(CONFIRM);
                yield generateSendMessage(telegramId,
                    "Добро пожаловать! Вы не зарегистрированы, желаете пройти регистрацию? Ответьте 'Да' или 'Нет'.",
                    menus.get(SELECTION_MENU));
            }
            case CONFIRM -> {
                if ("Да".equalsIgnoreCase(message)) {
                    state.setStep(FULL_NAME);
                    yield generateSendMessage(telegramId, "Введите ФИО:");
                } else if ("Нет".equalsIgnoreCase(message)) {
                    state.setStep(RETRY_REGISTRATION);
                    yield generateSendMessage(telegramId, "Регистрация отменена.", menus.get(TRY_AGAIN_MENU));
                } else {
                    state.setStep(RETRY_REGISTRATION);
                    yield generateSendMessage(telegramId, "Неверная команда, регистрация отменена",
                        menus.get(TRY_AGAIN_MENU));
                }
            }
            case FULL_NAME -> {
                state.setFullName(message);
                String[] nameParts = state.getFullName().trim().split("\\s+");
                if (nameParts.length < 3) {
                    state.setStep(RETRY_REGISTRATION);
                    yield generateSendMessage(telegramId,
                        "Ошибка: ФИО должно содержать минимум 3 слова, регистрация отменена.",
                        menus.get(TRY_AGAIN_MENU));
                }
                state.setName(nameParts[1]);
                state.setStep(PHONE_NUMBER);
                yield generateSendMessage(telegramId, "Введите номер телефона:");
            }
            case PHONE_NUMBER -> {
                state.setPhoneNumber(message);
                state.setStep(INFO);
                yield generateSendMessage(telegramId, "Введите дополнительное описание (до 255 символов):");
            }
            case INFO -> {
                if (message.length() <= 255) {
                    state.setInfo(message);
                    saveUser(state, telegramId);
                    state.setStep(FINISH);
                    yield generateSendMessage(telegramId, "Вы успешно зарегистрированы!", menus.get(GO_TO_MAIN_MENU));
                } else {
                    state.setStep(RETRY_REGISTRATION);
                    yield generateSendMessage(telegramId, "Описание слишком длинное, регистрация отменена.",
                        menus.get(TRY_AGAIN_MENU));
                }
            }
            case RETRY_REGISTRATION -> {
                if (TRY_AGAIN.equalsIgnoreCase(message)) {
                    registrationStates.get(telegramId).setStep(REQUEST_REGISTRATION);
                    yield handle(message, telegramId);
                } else {
                    yield generateSendMessage(telegramId, "Неверная команда");
                }
            }
            case FINISH -> {
                if (TO_MAIN_MENU.equalsIgnoreCase(message)) {
                    registrationStates.remove(telegramId);
                    basicStates.put(telegramId, IN_MAIN_MENU);
                    yield menuGenerator.generateRoleSpecificMainMenu(telegramId,
                        userRepository.findByTelegramId(telegramId).get().getUserRole());
                } else {
                    yield generateSendMessage(telegramId, "Неверная команда");
                }
            }
        };
    }

    private void saveUser(RegistrationState state, long telegramId) {
        User user = new User();
        user.setTelegramId(telegramId);
        user.setFullName(state.getFullName());
        user.setName(state.getName());
        user.setUserCard(currentUserCard++);
        user.setPhoneNumber(state.getPhoneNumber());
        user.setInfo(state.getInfo());
        user.setUserRole(Role.USER);
        user.setBirthDate(LocalDate.now()); // Временно
        userRepository.save(user);
    }

    @Override
    public BasicState getState() {
        return REGISTRATION;
    }
}
