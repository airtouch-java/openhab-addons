package org.openhab.binding.airtouch.internal.handler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.airtouch.internal.AirTouchStatusUtil;
import org.openhab.binding.airtouch.internal.dto.AirtouchStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import airtouch.Request;
import airtouch.Response;
import airtouch.connector.AirtouchConnector;
import airtouch.connector.AirtouchConnectorThreadFactory;
import airtouch.constant.AirConditionerControlConstants.Mode;
import airtouch.constant.ZoneControlConstants.ZonePower;
import airtouch.model.AirConditionerAbilityResponse;
import airtouch.model.AirConditionerStatusResponse;
import airtouch.model.ConsoleVersionResponse;
import airtouch.model.ZoneNameResponse;
import airtouch.v4.connector.Airtouch4ConnectorThreadFactory;
import airtouch.v4.constant.GroupControlConstants.GroupPower;
import airtouch.v4.constant.MessageConstants;
import airtouch.v4.handler.AirConditionerAbilityHandler;
import airtouch.v4.handler.AirConditionerStatusHandler;
import airtouch.v4.handler.ConsoleVersionHandler;
import airtouch.v4.handler.GroupNameHandler;
import airtouch.v4.handler.GroupStatusHandler;

@NonNullByDefault
public class AirTouch4ServiceImpl implements AirTouchService<MessageConstants.Address> {

    private final Logger logger = LoggerFactory.getLogger(AirTouch4ServiceImpl.class);

    private @Nullable AirtouchConnector<MessageConstants.Address> airtouchConnector;

    private AirtouchStatus status = new AirtouchStatus();
    private Map<Integer, Boolean> responseReceived = new HashMap<>();
    private AtomicInteger counter = new AtomicInteger(0);
    private AtomicBoolean gotConfigFromAirtouch = new AtomicBoolean(false);
    private @Nullable ScheduledFuture<?> future;

    private @Nullable AirTouchServiceListener airTouchServiceListener;
    private AirtouchConnectorThreadFactory threadFactory = new Airtouch4ConnectorThreadFactory();

    @Override
    public void requestFullUpdate() throws IOException {
        if (this.airtouchConnector == null) {
            logger.trace("AirtouchConnector is not yet initialised. Skipping requestFullUpdate");
            return;
        }
        final AirtouchConnector<MessageConstants.Address> myairtouchConnector = this.airtouchConnector;
        this.gotConfigFromAirtouch.set(false);
        this.responseReceived.clear();

        if (counter.get() > 120) {
            counter.set(0);
        }

        int nextRequestId = counter.incrementAndGet();
        this.responseReceived.put(nextRequestId, Boolean.FALSE);
        myairtouchConnector.sendRequest(GroupStatusHandler.generateRequest(nextRequestId, null));

        nextRequestId = counter.incrementAndGet();
        this.responseReceived.put(nextRequestId, Boolean.FALSE);
        myairtouchConnector.sendRequest(GroupNameHandler.generateRequest(nextRequestId, null));

        nextRequestId = counter.incrementAndGet();
        this.responseReceived.put(nextRequestId, Boolean.FALSE);
        myairtouchConnector.sendRequest(AirConditionerStatusHandler.generateRequest(nextRequestId, null));

        nextRequestId = counter.incrementAndGet();
        this.responseReceived.put(nextRequestId, Boolean.FALSE);
        myairtouchConnector.sendRequest(ConsoleVersionHandler.generateRequest(nextRequestId));

        nextRequestId = counter.incrementAndGet();
        this.responseReceived.put(nextRequestId, Boolean.FALSE);
        myairtouchConnector.sendRequest(AirConditionerAbilityHandler.generateRequest(nextRequestId, null));
    }

    @Override
    public void requestStatusUpdate() throws IOException {
        if (this.airtouchConnector == null) {
            logger.trace("AirtouchConnector is not yet initialised. Skipping requestStatusUpdate");
            return;
        }
        final AirtouchConnector<MessageConstants.Address> myairtouchConnector = this.airtouchConnector;
        myairtouchConnector.sendRequest(GroupStatusHandler.generateRequest(counter.incrementAndGet(), null));
        myairtouchConnector.sendRequest(AirConditionerStatusHandler.generateRequest(counter.incrementAndGet(), null));
    }

    @SuppressWarnings({ "unchecked" })
    private void handleEvent(final @Nullable Response response) {
        if (response == null) {
            logger.trace("handlingEvent called with null response");
            return;
        }
        logger.trace("handlingEvent with response: {}", response);
        switch (response.getMessageType()) {
            case AC_STATUS:
                final List<AirConditionerStatusResponse> acStatuses = response.getData();
                status.setAcStatuses(acStatuses);
                if (this.gotConfigFromAirtouch.get() && this.airTouchServiceListener != null) {
                    this.airTouchServiceListener.airconditionerStatusUpdate(acStatuses);
                }
                break;
            case ZONE_STATUS:
                status.setGroupStatuses(response.getData());
                if (this.gotConfigFromAirtouch.get() && this.airTouchServiceListener != null) {
                    this.airTouchServiceListener.zoneStatusUpdate(response.getData());
                }
                break;
            case ZONE_NAME:
                status.setGroupNames(((List<ZoneNameResponse>) response.getData()).stream()
                        .collect(Collectors.toMap(ZoneNameResponse::getZoneNumber, ZoneNameResponse::getName)));
                break;
            case AC_ABILITY:
                status.setAcAbilities(((List<AirConditionerAbilityResponse>) response.getData()).stream()
                        .collect(Collectors.toMap(AirConditionerAbilityResponse::getAcNumber, r -> r)));
                break;
            case CONSOLE_VERSION:
                status.setConsoleVersion((ConsoleVersionResponse) response.getData().stream().findFirst().orElse(null));
                break;
            case EXTENDED:
                break;
            case ZONE_CONTROL:
                break;
            default:
                break;
        }

        if (this.responseReceived.containsKey(response.getMessageId())) {
            this.responseReceived.put(response.getMessageId(), Boolean.TRUE);
        }

        if (!this.responseReceived.containsValue(Boolean.FALSE) && !this.gotConfigFromAirtouch.get()) {
            this.gotConfigFromAirtouch.set(true);
            logger.debug("Expected events received: {}. Updating thing structure.", this.responseReceived);
            if (this.airTouchServiceListener != null) {
                this.airTouchServiceListener.fullUpdate(getStatus());
            } else {
                logger.debug(
                        "airTouchServiceListener is null. Skipping airTouchServiceListener#initialisationCompleted()");
            }
        } else if (!this.gotConfigFromAirtouch.get()) {
            logger.debug("Not all events received yet: {}", this.responseReceived);
        }
    }

    public AirtouchStatus getStatus() {
        return status;
    }

    @Override
    public void start(@Nullable String host, int port) {
        this.airtouchConnector = new AirtouchConnector<>(threadFactory, host, port, this::handleEvent);
        this.airtouchConnector.start();
    }

    @Override
    public void shutdown() {
        if (this.airtouchConnector != null && this.airtouchConnector.isRunning()) {
            this.airtouchConnector.shutdown();
        }
    }

    @Override
    public void restart(@Nullable String host, int port) {
        shutdown();
        start(host, port);
    }

    @Override
    public void sendRequest(@NonNull Request<MessageConstants.Address> airTouchRequest) throws IOException {
        this.airtouchConnector.sendRequest(airTouchRequest);
    }

    @Override
    public void registerListener(@NonNull AirTouchServiceListener airTouchServiceListener) {
        this.airTouchServiceListener = airTouchServiceListener;
    }

    @Override
    public int getNextRequestId() {
        return this.counter.incrementAndGet();
    }

    @Override
    public void validateAcSetpoint(int acNumber, int setpointValue) throws IllegalArgumentException {
        AirConditionerAbilityResponse acAbilityResponse = this.status.getAcAbilities().get(acNumber);
        if (setpointValue > acAbilityResponse.getMaxSetPoint() || setpointValue < acAbilityResponse.getMinSetPoint()) {
            throw new IllegalArgumentException(String.format(
                    "Setpoint value '%s' is not supported for AC '%s' (%s). Accepted setpoint range is %s - %s",
                    setpointValue, acAbilityResponse.getAcName(), acAbilityResponse.getAcNumber(),
                    acAbilityResponse.getMinSetPoint(), acAbilityResponse.getMaxSetPoint()));
        }
    }

    @Override
    public void validateAcMode(int acNumber, Mode acMode) throws IllegalArgumentException {
        AirConditionerAbilityResponse acAbilityResponse = this.status.getAcAbilities().get(acNumber);
        if (!acAbilityResponse.getSupportedModes().contains(acMode)) {
            throw new IllegalArgumentException(String.format(
                    "Mode value '%s' is not supported for AC '%s' (%s). Accepted Modes are: %s", acMode.toString(),
                    acAbilityResponse.getAcName(), acAbilityResponse.getAcNumber(), acAbilityResponse
                            .getSupportedModes().stream().map(Mode::toString).collect(Collectors.joining(","))));

        }
    }

    @Override
    public void validateZonePowerState(int zoneNumber, ZonePower zonePowerState) throws IllegalArgumentException {
        if (GroupPower.TURBO_POWER.getGeneric().equals(zonePowerState)
                && !this.status.getGroupStatuses().get(zoneNumber).isTurboSupported()) {
            throw new IllegalArgumentException(String.format("'%s' is not supported for Zone '%s' (%s)", zonePowerState,
                    this.status.getGroupNames().get(zoneNumber), zoneNumber));
        }
    }

    @Override
    public void validateZoneSetpoint(int zoneNumber, int setpointValue) throws IllegalArgumentException {
        AirConditionerAbilityResponse acAbilityResponse = AirTouchStatusUtil.findAcForZone(this.status, zoneNumber);
        if (setpointValue > acAbilityResponse.getMaxSetPoint() || setpointValue < acAbilityResponse.getMinSetPoint()) {
            throw new IllegalArgumentException(String.format(
                    "Setpoint value '%s' is not supported for AC '%s' (%s). Accepted setpoint range is %s - %s",
                    setpointValue, acAbilityResponse.getAcName(), acAbilityResponse.getAcNumber(),
                    acAbilityResponse.getMinSetPoint(), acAbilityResponse.getMaxSetPoint()));
        }
    }
}
