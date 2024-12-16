package net.dunice.mk.rsmtelegrambot.config;

import net.dunice.mk.rsmtelegrambot.handler.state.BasicState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.GrantAdminState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.RegistrationState;
import net.dunice.mk.rsmtelegrambot.handler.state.stateobject.UpdateProfileState;
import net.dunice.mk.rsmtelegrambot.handler.state.step.ShowEventsStep;
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

    @Bean("registrationStates")
    public Map<Long, RegistrationState> getRegistrationStatesMap() {
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

    @Bean("allStatesMap")
    public List<Map<Long, ?>> getAllStatesMap(Map<Long, GrantAdminState> grantAdminStates,
                                              Map<Long, RegistrationState> registrationStates,
                                              Map<Long, UpdateProfileState> updateProfileStates,
                                              Map<Long, ShowEventsStep> showEventSteps) {
        return List.of(grantAdminStates, registrationStates, updateProfileStates, showEventSteps);
    }
}
