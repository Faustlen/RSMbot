package net.dunice.mk.rsmtelegrambot.config;

import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.EventCreationState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.GrantAdminState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.PartnerRegistrationState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.ShowAdminsState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.ShowEventsState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.ShowPartnersState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.ShowUsersState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.UpdateProfileState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.UserRegistrationState;
import net.dunice.mk.rsmtelegrambot.handler.state.step.SelectRegistrationStep;
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
    public Map<Long, ShowAdminsState> getShowAdminsStatesMap() {return new ConcurrentHashMap<>();}

    @Bean("showUsersStates")
    public Map<Long, ShowUsersState> getShowUsersStatesMap() {return new ConcurrentHashMap<>();}

    @Bean("showEventStates")
    public Map<Long, ShowEventsState> getShowEventsStatesMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean("showPartnersStates")
    public Map<Long, ShowPartnersState> getShowPartnersStatesMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean("selectRegistrationSteps")
    public Map<Long, SelectRegistrationStep> getSelectRegistrationStepsMap() {
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
                                              Map<Long, SelectRegistrationStep> selectRegistrationSteps) {
        return List.of(grantAdminStates, registrationStates, updateProfileStates, showEventStates, showPartnersStates,
            eventCreationStates, partnerRegistrationStates, showAdminsStates, showUsersStates, selectRegistrationSteps);
    }
}
