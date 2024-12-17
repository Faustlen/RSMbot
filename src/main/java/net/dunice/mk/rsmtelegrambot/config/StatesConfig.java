package net.dunice.mk.rsmtelegrambot.config;

import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.GrantAdminState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.PartnerRegistrationState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.UserRegistrationState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.UpdateProfileState;
import net.dunice.mk.rsmtelegrambot.handler.state.step.SelectRegistrationStep;
import net.dunice.mk.rsmtelegrambot.handler.state.step.ShowEventsStep;
import net.dunice.mk.rsmtelegrambot.handler.state.step.ShowPartnersStep;
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

    @Bean("showEventSteps")
    public Map<Long, ShowEventsStep> showEventsStepMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean("showPartnerSteps")
    public Map<Long, ShowPartnersStep> showPartnersStepMap() {return new ConcurrentHashMap<>();};

    @Bean("selectRegistrationSteps")
    public Map<Long, SelectRegistrationStep> selectRegistrationStepsMap() {
        return new ConcurrentHashMap<>();
    };

    @Bean("allStatesMap")
    public List<Map<Long, ?>> getAllStatesMap(Map<Long, GrantAdminState> grantAdminStates,
                                              Map<Long, UserRegistrationState> registrationStates,
                                              Map<Long, UpdateProfileState> updateProfileStates,
                                              Map<Long, ShowEventsStep> showEventSteps,
                                              Map<Long, ShowPartnersStep> showPartnerSteps) {
        return List.of(grantAdminStates, registrationStates, updateProfileStates, showEventSteps, showPartnerSteps);
    }
}
