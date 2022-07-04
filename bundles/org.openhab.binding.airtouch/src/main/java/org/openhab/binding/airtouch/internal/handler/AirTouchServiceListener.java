package org.openhab.binding.airtouch.internal.handler;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.binding.airtouch.internal.dto.AirtouchStatus;

import airtouch.model.AirConditionerStatusResponse;
import airtouch.model.ZoneStatusResponse;

public interface AirTouchServiceListener {

    void fullUpdate(final @NonNull AirtouchStatus status);

    void airconditionerStatusUpdate(final @NonNull List<AirConditionerStatusResponse> acStatuses);

    void zoneStatusUpdate(final @NonNull List<ZoneStatusResponse> groupStatuses);
}
