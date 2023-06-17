package com.acutus.atk.spring.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.stereotype.Component;

import static org.springframework.boot.availability.ReadinessState.ACCEPTING_TRAFFIC;

@Component
public class ApplicationState {

    @Autowired
    ApplicationAvailability availability;

    public boolean isReady() {
        return availability.getLivenessState() == LivenessState.CORRECT && availability.getReadinessState() == ACCEPTING_TRAFFIC;
    }
}
