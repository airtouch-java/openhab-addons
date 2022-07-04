package org.openhab.binding.airtouch.internal.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import airtouch.model.AirConditionerAbilityResponse;
import airtouch.model.AirConditionerStatusResponse;
import airtouch.model.ConsoleVersionResponse;
import airtouch.model.ZoneStatusResponse;

public class AirtouchStatus {
    private List<AirConditionerStatusResponse> acStatuses = new ArrayList<>();
    private Map<Integer, AirConditionerAbilityResponse> acAbilities = new HashMap<>();
    private List<ZoneStatusResponse> groupStatuses = new ArrayList<>();
    private Map<Integer, String> groupNames = new HashMap<>();
    private ConsoleVersionResponse consoleVersion = null;

    public List<AirConditionerStatusResponse> getAcStatuses() {
        return acStatuses;
    }

    public void setAcStatuses(final List<AirConditionerStatusResponse> acStatuses) {
        this.acStatuses = acStatuses;
    }

    public Map<Integer, AirConditionerAbilityResponse> getAcAbilities() {
        return acAbilities;
    }

    public void setAcAbilities(Map<Integer, AirConditionerAbilityResponse> acAbilities) {
        this.acAbilities = acAbilities;
    }

    public List<ZoneStatusResponse> getGroupStatuses() {
        return groupStatuses;
    }

    public void setGroupStatuses(final List<ZoneStatusResponse> groupStatuses) {
        this.groupStatuses = groupStatuses;
    }

    public Map<Integer, String> getGroupNames() {
        return groupNames;
    }

    public void setGroupNames(Map<Integer, String> groupNames) {
        this.groupNames = groupNames;
    }

    public ConsoleVersionResponse getConsoleVersion() {
        return consoleVersion;
    }

    public void setConsoleVersion(ConsoleVersionResponse consoleVersion) {
        this.consoleVersion = consoleVersion;
    }

    @Override
    public String toString() {
        return "AirtouchStatus [acStatuses=" + acStatuses + ", acAbilities=" + acAbilities + ", groupStatuses="
                + groupStatuses + ", groupNames=" + groupNames + ", consoleVersion=" + consoleVersion + "]";
    }
}
