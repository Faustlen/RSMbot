package net.dunice.mk.rsmtelegrambot.handler.state;

public class PartnerEditingState {
    PartnerEditingStep step = PartnerEditingStep.SHOW_INFO;

    public enum PartnerEditingStep {
        SHOW_INFO
    }
}
