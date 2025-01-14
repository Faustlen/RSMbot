package net.dunice.mk.rsmtelegrambot.config;

import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.EventCreationState;
import net.dunice.mk.rsmtelegrambot.handler.state.GrantAdminState;
import net.dunice.mk.rsmtelegrambot.handler.state.MessageBroadcastState;
import net.dunice.mk.rsmtelegrambot.handler.state.PartnerRegistrationState;
import net.dunice.mk.rsmtelegrambot.handler.state.SelectRegistrationState;
import net.dunice.mk.rsmtelegrambot.handler.state.ShowAdminsState;
import net.dunice.mk.rsmtelegrambot.handler.state.ShowEventsState;
import net.dunice.mk.rsmtelegrambot.handler.state.ShowPartnersState;
import net.dunice.mk.rsmtelegrambot.handler.state.ShowUsersState;
import net.dunice.mk.rsmtelegrambot.handler.state.UpdateProfileState;
import net.dunice.mk.rsmtelegrambot.handler.state.UserRegistrationState;
import net.dunice.mk.rsmtelegrambot.handler.state.ShowAnalyticsState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class StatesConfig {

    @Bean
    public Map<Long, BasicState> getInteractionStatesMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean("eventCreationStates")
    public Map<Long, EventCreationState> getEventCreationStatesMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean("grantAdminStates")
    public Map<Long, GrantAdminState> getGrantAdminStatesMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean("userRegistrationStates")
    public Map<Long, UserRegistrationState> getRegistrationStatesMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean("partnerRegistrationStates")
    public Map<Long, PartnerRegistrationState> getPartnerRegistrationStatesMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean("updateProfileStates")
    public Map<Long, UpdateProfileState> getUpdateProfileStatesMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean("showAdminsStates")
    public Map<Long, ShowAdminsState> getShowAdminsStatesMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean("showAnalyticsStates")
    public Map<Long, ShowAnalyticsState> getShowAnalyticsStatesMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean("showUsersStates")
    public Map<Long, ShowUsersState> getShowUsersStatesMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean("showEventStates")
    public Map<Long, ShowEventsState> getShowEventsStatesMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean("showPartnersStates")
    public Map<Long, ShowPartnersState> getShowPartnersStatesMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean("selectRegistrationSteps")
    public Map<Long, SelectRegistrationState> getSelectRegistrationStatesMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean("messageBroadcastStates")
    public Map<Long, MessageBroadcastState> getMessageBroadcastStatesMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean("allStatesMap")
    public List<Map<Long, ?>> getAllStatesMap(Map<Long, GrantAdminState> grantAdminStates,
                                              Map<Long, UserRegistrationState> registrationStates,
                                              Map<Long, UpdateProfileState> updateProfileStates,
                                              Map<Long, ShowEventsState> showEventStates,
                                              Map<Long, ShowPartnersState> showPartnersStates,
                                              Map<Long, EventCreationState> eventCreationStates,
                                              Map<Long, PartnerRegistrationState> partnerRegistrationStates,
                                              Map<Long, ShowAdminsState> showAdminsStates,
                                              Map<Long, ShowUsersState> showUsersStates,
                                              Map<Long, SelectRegistrationState> selectRegistrationStates,
                                              Map<Long, MessageBroadcastState> messageBroadcastStates) {
        return List.of(grantAdminStates, registrationStates, updateProfileStates, showEventStates, showPartnersStates,
            eventCreationStates, partnerRegistrationStates, showAdminsStates, showUsersStates, selectRegistrationStates,
            messageBroadcastStates, showAnalyticsStates);
    }
}
