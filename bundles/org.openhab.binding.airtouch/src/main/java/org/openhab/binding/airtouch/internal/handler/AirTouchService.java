package org.openhab.binding.airtouch.internal.handler;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import airtouch.Request;
import airtouch.constant.AirConditionerControlConstants.Mode;
import airtouch.constant.ZoneControlConstants.ZonePower;

public interface AirTouchService<A> {

    /**
     * Start service and create a connection to AirTouch using the host and port provided.
     *
     * @param host - IP or Hostname of AirTouch on your network
     * @param port - Port the AirTouch is listening on.
     */
    void start(@Nullable String host, int port);

    /**
     * Stops AirTouch service if it's running.
     * Should disconnect from AirTouch connection created by {@link #start(String, int)} or
     * {@link #restart(String, int)}.
     */
    void shutdown();

    /**
     * Restart service and create a new connection to AirTouch using the host and port provided.
     *
     * @param host - IP or Hostname of AirTouch on your network
     * @param port - Port the AirTouch is listening on.
     */
    void restart(@Nullable String host, int port);

    /**
     * Register the {@link AirTouchServiceListener} with the service.
     * This callback will be called to send updates back to the
     *
     * @param airTouchServiceListener
     */
    void registerListener(@NonNull AirTouchServiceListener airTouchServiceListener);

    /**
     * Get the next requestId from the internal counter maintained by {@link AirTouchService}
     * It's useful to provide a different ID for each request to the Airtouch, so that we can
     * identify the response.
     *
     * @return An int that can be used for assembling the {@link Request} to the AirTouch.
     */
    int getNextRequestId();

    /**
     * Sends a {@link Request} to the AirTouch.
     * A request could be to execute a change, or to request an update.
     *
     * @param airTouchRequest
     * @throws IOException if AirTouch cannot be contacted.
     */
    void sendRequest(@NonNull Request<A> airTouchRequest) throws IOException;

    /**
     * Requests a full update from AirTouch. This includes "configurations", eg, AC Names, Zone Names, Error Code,
     * Console Version as well as "statuses", eg Setpoint, Temperature, Power state, etc.
     * Expect the following to be called shortly after:
     * <ul>
     * <li>{@link AirTouchServiceListener#airconditionerStatusUpdate(java.util.List)}
     * <li>{@link AirTouchServiceListener#zoneStatusUpdate(java.util.List)}
     * <li>{@link AirTouchServiceListener#fullUpdate(org.openhab.binding.airtouch.internal.dto.AirtouchStatus)}
     * </ul>
     *
     * @throws IOException if AirTouch cannot be contacted.
     */
    void requestFullUpdate() throws IOException;

    /**
     * Requests a status update from AirTouch. eg AC and Zone Setpoint, Temperature, Power state, etc.
     * Expect the following to be called shortly after:
     * <ul>
     * <li>{@link AirTouchServiceListener#airconditionerStatusUpdate(java.util.List)}
     * <li>{@link AirTouchServiceListener#zoneStatusUpdate(java.util.List)}
     * </ul>
     *
     * @throws IOException if AirTouch cannot be contacted.
     */
    void requestStatusUpdate() throws IOException;

    /**
     * Validate that the requested AC Unit Setpoint is applicable for this Airtouch.
     *
     * @param acNumber
     * @param setpointValue
     * @throws IllegalArgumentException
     */
    void validateAcSetpoint(int acNumber, int setpointValue) throws IllegalArgumentException;

    /**
     * Validate that the requested AC Unit Mode is applicable for this Airtouch.
     *
     * @param acNumber
     * @param acMode
     * @throws IllegalArgumentException
     */
    void validateAcMode(int acNumber, @NonNull Mode acMode) throws IllegalArgumentException;

    /**
     * Validate that the requested AC Unit Setpoint is applicable for this Airtouch.
     *
     * @param zoneNumber
     * @param zonePowerState
     * @throws IllegalArgumentException
     */
    void validateZonePowerState(int zoneNumber, @NonNull ZonePower zonePowerState) throws IllegalArgumentException;

    /**
     * Validate that the requested Zone Setpoint is applicable for this Airtouch.
     *
     * @param zoneNumber
     * @param setpointValue
     * @throws IllegalArgumentException
     */
    void validateZoneSetpoint(int zoneNumber, int setpointValue) throws IllegalArgumentException;
}
