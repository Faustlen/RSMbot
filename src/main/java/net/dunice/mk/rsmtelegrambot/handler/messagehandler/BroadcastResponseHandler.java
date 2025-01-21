package net.dunice.mk.rsmtelegrambot.handler.messagehandler;

import lombok.RequiredArgsConstructor;
import net.dunice.mk.rsmtelegrambot.dto.MessageDto;
import net.dunice.mk.rsmtelegrambot.entity.Partner;
import net.dunice.mk.rsmtelegrambot.handler.BaseHandler;
import net.dunice.mk.rsmtelegrambot.repository.PartnerRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

@Service
@RequiredArgsConstructor
public class BroadcastResponseHandler implements BaseHandler {

    private final PartnerRepository partnerRepository;

    @Override
    public SendMessage handle(MessageDto messageDto, Long telegramId) {
        String callbackData = messageDto.getText();
        String action = callbackData.split("_")[0];

        return switch (action) {
            case "partnerAccept" -> {
                yield handlePartnerChange(callbackData, telegramId);
            }
            default -> {
                yield generateSendMessage(telegramId, "Неизвестная команда");
            }
        };
    }

    public SendMessage handlePartnerChange(String callbackData, Long adminTelegramId) {
        if (callbackData.startsWith("partnerAccept_confirm_")) {
            Long partnerId = Long.parseLong(callbackData.replace("partnerAccept_confirm_", ""));
            Partner partner = partnerRepository.findById(partnerId).orElse(null);
            if (partner != null) {
                partner.setValid(true);
                partnerRepository.save(partner);
                return generateSendMessage(adminTelegramId, "Партнер \"" + partner.getName() + "\" успешно подтвержден.");
            } else {
                return generateSendMessage(adminTelegramId, "Партнер не найден.");
            }
        } else if (callbackData.startsWith("partnerAccept_reject_")) {
            Long partnerId = Long.parseLong(callbackData.replace("partnerAccept_reject_", ""));
            Partner partner = partnerRepository.findById(partnerId).orElse(null);
            if (partner != null) {
                return generateSendMessage(adminTelegramId, "Партнер \"" + partner.getName() + "\" отклонён.");
            } else {
                return generateSendMessage(adminTelegramId, "Партнер не найден.");
            }
        } else {
            return generateSendMessage(adminTelegramId, "Неизвестная команда.");
        }
    }
}
