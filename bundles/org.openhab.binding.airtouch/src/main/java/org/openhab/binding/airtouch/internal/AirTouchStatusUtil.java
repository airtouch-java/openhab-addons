package org.openhab.binding.airtouch.internal;

import org.openhab.binding.airtouch.internal.dto.AirtouchStatus;

import airtouch.model.AirConditionerAbilityResponse;

public class AirTouchStatusUtil {
    private AirTouchStatusUtil() {
    } // Can't Instantiate utilities class.

    /**
     * Determines if the Zone Number is greater or equal to the starting group number on the AC.
     * Loops over all the AC units in an effort to handle where AirConditioner '1' has a startingGroup of '0'
     * and AirConditioner '2' has a startingGroup as "4". If our zone is "4", we will be on AC unit 2.
     *
     * @param airTouchStatus
     * @param zoneNumber
     * @return the AC unit that handles this zone.
     */
    public static AirConditionerAbilityResponse findAcForZone(AirtouchStatus airTouchStatus, Integer zoneNumber) {
        AirConditionerAbilityResponse response = null;
        for (AirConditionerAbilityResponse acAbilityResponse : airTouchStatus.getAcAbilities().values()) {
            if (zoneNumber >= acAbilityResponse.getStartGroupNumber()) {
                response = acAbilityResponse;
            }
        }
        if (response == null) {
            throw new IllegalArgumentException(String.format("Unable to determine AC Unit for Zone '%s' (%s).",
                    airTouchStatus.getGroupNames().get(zoneNumber), zoneNumber));
        }
        return response;
    }
}
