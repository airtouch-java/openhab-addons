/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.airtouch.internal.handler;

import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.CHANNELUID_AIRCONDITIONER_UNIT_FANSPEED;
import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.CHANNELUID_AIRCONDITIONER_UNIT_MODE;
import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.CHANNELUID_AIRCONDITIONER_UNIT_POWER;
import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.CHANNELUID_AIRCONDITIONER_UNIT_SETPOINT;
import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.CHANNELUID_AIRCONDITIONER_UNIT_SPILL;
import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.CHANNELUID_AIRCONDITIONER_UNIT_TEMPERATURE;
import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.CHANNELUID_AIRCONDITIONER_UNIT_TIMER;
import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.CHANNELUID_AIRCONDITIONER_ZONE_BATTERY_LOW;
import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.CHANNELUID_AIRCONDITIONER_ZONE_FLOW;
import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.CHANNELUID_AIRCONDITIONER_ZONE_POWER;
import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.CHANNELUID_AIRCONDITIONER_ZONE_SETPOINT;
import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.CHANNELUID_AIRCONDITIONER_ZONE_TEMPERATURE;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.airtouch.internal.AirTouchBindingConstants;
import org.openhab.binding.airtouch.internal.AirTouchConfiguration;
import org.openhab.binding.airtouch.internal.AirTouchStateDescriptionFragmentCache;
import org.openhab.binding.airtouch.internal.AirTouchStatusUtil;
import org.openhab.binding.airtouch.internal.dto.AirtouchStatus;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import airtouch.constant.AirConditionerControlConstants;
import airtouch.constant.AirConditionerControlConstants.AcPower;
import airtouch.constant.AirConditionerControlConstants.Mode;
import airtouch.constant.AirConditionerControlConstants.SetpointControl;
import airtouch.constant.AirConditionerStatusConstants;
import airtouch.constant.ZoneControlConstants;
import airtouch.constant.ZoneControlConstants.ZonePower;
import airtouch.constant.ZoneControlConstants.ZoneSetting;
import airtouch.constant.ZoneStatusConstants;
import airtouch.exception.AirtouchMessagingException;
import airtouch.model.AirConditionerAbilityResponse;
import airtouch.model.AirConditionerStatusResponse;
import airtouch.model.ZoneStatusResponse;
import airtouch.v4.constant.MessageConstants;
import airtouch.v4.handler.AirConditionerControlHandler;
import airtouch.v4.handler.GroupControlHandler;
import tech.units.indriya.unit.Units;

/**
 * The {@link AirTouch4Handler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Nathaniel Wolfe - Initial contribution
 */
@NonNullByDefault
public class AirTouch4Handler extends BaseThingHandler implements AirTouchServiceListener {

    private final Logger logger = LoggerFactory.getLogger(AirTouch4Handler.class);

    private final AirTouchService<MessageConstants.Address> airtouch4Service;
    private final AirTouchStateDescriptionFragmentCache stateDescriptionFragmentCache;

    private @Nullable ScheduledFuture<?> future;

    public AirTouch4Handler(Thing thing, AirTouchService<MessageConstants.Address> airTouch4Service,
            AirTouchStateDescriptionFragmentCache stateDescriptionFragmentCache) {
        super(thing);
        this.airtouch4Service = airTouch4Service;
        this.stateDescriptionFragmentCache = stateDescriptionFragmentCache;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (logger.isTraceEnabled()) {
            logger.trace("handleCommand called for '{}' -> '{}'", channelUID.getAsString(), command.toFullString());
            logger.trace("getBindingId: '{}'", channelUID.getBindingId());
            logger.trace("getGroupId: '{}'", channelUID.getGroupId());
            logger.trace("getIdWithoutGroup: '{}'", channelUID.getIdWithoutGroup());
            logger.trace("getThingUID: '{}'", channelUID.getThingUID());
            logger.trace("getId: '{}'", channelUID.getId());
        }

        try {
            if (command instanceof RefreshType) {
                this.airtouch4Service.requestStatusUpdate();
            } else if (channelUID.isInGroup() && channelUID.getGroupId().startsWith("ac-unit-")) { // NOSONAR - null
                                                                                                   // pointer won't be
                                                                                                   // thrown because
                                                                                                   // isInGroup was
                                                                                                   // called.
                handleUnitCommand(channelUID, command);
            } else if (channelUID.isInGroup() && channelUID.getGroupId().startsWith("ac-zone-")) { // NOSONAR - null
                                                                                                   // pointer won't be
                                                                                                   // thrown because
                                                                                                   // isInGroup was
                                                                                                   // called.
                handleZoneCommand(channelUID, command);
            }
        } catch (IOException e) {
            final AirTouchConfiguration myConfig = getConfigAs(AirTouchConfiguration.class);
            handleConnectionException(e, myConfig);
        } catch (IllegalArgumentException | AirtouchMessagingException e) {
            logger.warn("Failed to handleCommand for '{}' -> '{}'. Exception: {}", channelUID.getAsString(),
                    command.toFullString(), e.getMessage());
        }

        // TODO: handle command

        // Note: if communication with thing fails for some reason,
        // indicate that by setting the status with detail information:
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
        // "Could not control device at IP address x.x.x.x");
    }

    private void handleConnectionException(final Exception e, final AirTouchConfiguration myConfig) {
        logger.warn("Exception occured requesting AirTouch update: {}. Attempting to restart service.", e.getMessage());
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                String.format("Error with connection  to '%s'. %s", myConfig.host, e.getMessage()));
        try {
            this.airtouch4Service.restart(myConfig.host, myConfig.port);
            this.airtouch4Service.requestFullUpdate();
        } catch (AirtouchMessagingException | IOException ex) {
            logger.warn(
                    "Exception occured requesting AirTouch restart. Reconnection should be attempted on next refresh. Exception was: {}. ",
                    ex.getMessage());
        }
    }

    private void handleUnitCommand(ChannelUID channelUID, Command command) throws IOException {
        Integer unitNumber = Integer.parseInt(channelUID.getGroupId().replace("ac-unit-", "")); // NOSONAR
        switch (channelUID.getIdWithoutGroup()) {
            case CHANNELUID_AIRCONDITIONER_UNIT_POWER:
                if (command instanceof OnOffType) {
                    AcPower acPowerState = convertToAcPower((OnOffType) command);
                    this.airtouch4Service.sendRequest(AirConditionerControlHandler.requestBuilder(unitNumber)
                            .acPower(acPowerState).build(this.airtouch4Service.getNextRequestId()));
                }
                break;
            case CHANNELUID_AIRCONDITIONER_UNIT_SETPOINT:
                if (command instanceof QuantityType) {
                    int setpointValue = ((QuantityType<?>) command).intValue();
                    this.airtouch4Service.validateZoneSetpoint(unitNumber, setpointValue);
                    this.airtouch4Service.sendRequest(AirConditionerControlHandler.requestBuilder(unitNumber)
                            .setpointControl(SetpointControl.SET_TO_VALUE).setpointValue(setpointValue)
                            .build(this.airtouch4Service.getNextRequestId()));
                } else if (command instanceof DecimalType) {
                    int setpointValue = ((DecimalType) command).intValue();
                    this.airtouch4Service.validateZoneSetpoint(unitNumber, setpointValue);
                    this.airtouch4Service.sendRequest(AirConditionerControlHandler.requestBuilder(unitNumber)
                            .setpointControl(SetpointControl.SET_TO_VALUE).setpointValue(setpointValue)
                            .build(this.airtouch4Service.getNextRequestId()));
                }
                break;
            case CHANNELUID_AIRCONDITIONER_UNIT_MODE:
                if (command instanceof StringType) {
                    Mode acMode = Mode.valueOf(command.toFullString());
                    this.airtouch4Service.validateAcMode(unitNumber, acMode);
                    this.airtouch4Service.sendRequest(AirConditionerControlHandler.requestBuilder(unitNumber)
                            .acMode(acMode).build(this.airtouch4Service.getNextRequestId()));
                }
                break;
        }
        logger.debug("unitCommand was '{}' for unitNumber '{}' of type '{}'", channelUID.getIdWithoutGroup(),
                unitNumber, command.getClass());
    }

    private void handleZoneCommand(ChannelUID channelUID, Command command) throws IOException {
        Integer zoneNumber = Integer.parseInt(channelUID.getGroupId().replace("ac-zone-", "")); // NOSONAR
        switch (channelUID.getIdWithoutGroup()) {
            case CHANNELUID_AIRCONDITIONER_ZONE_POWER:
                if (command instanceof OnOffType) {
                    ZonePower zonePowerState = convertToZonePower((OnOffType) command);
                    this.airtouch4Service.sendRequest(GroupControlHandler.requestBuilder(zoneNumber)
                            .power(zonePowerState).build(this.airtouch4Service.getNextRequestId()));
                } else if (command instanceof StringType) {
                    ZonePower zonePowerState = convertToZonePower((StringType) command);
                    this.airtouch4Service.validateZonePowerState(zoneNumber, zonePowerState);
                    this.airtouch4Service.sendRequest(GroupControlHandler.requestBuilder(zoneNumber)
                            .power(zonePowerState).build(this.airtouch4Service.getNextRequestId()));
                }
                break;
            case CHANNELUID_AIRCONDITIONER_ZONE_SETPOINT:
                if (command instanceof QuantityType) {
                    int setpointValue = ((QuantityType<?>) command).intValue();
                    this.airtouch4Service.validateZoneSetpoint(zoneNumber, setpointValue);
                    this.airtouch4Service.sendRequest(
                            GroupControlHandler.requestBuilder(zoneNumber).setting(ZoneSetting.SET_TARGET_SETPOINT)
                                    .settingValue(setpointValue).build(this.airtouch4Service.getNextRequestId()));
                }
                if (command instanceof DecimalType) {
                    int setpointValue = ((DecimalType) command).intValue();
                    this.airtouch4Service.validateZoneSetpoint(zoneNumber, setpointValue);
                    this.airtouch4Service.sendRequest(
                            GroupControlHandler.requestBuilder(zoneNumber).setting(ZoneSetting.SET_TARGET_SETPOINT)
                                    .settingValue(setpointValue).build(this.airtouch4Service.getNextRequestId()));
                }
                break;
        }
        logger.debug("zoneCommand was '{}' for unitNumber '{}' of type '{}'", channelUID.getIdWithoutGroup(),
                zoneNumber, command.getClass());
    }

    private AcPower convertToAcPower(OnOffType command) {
        return command.equals(OnOffType.ON) ? AirConditionerControlConstants.AcPower.POWER_ON
                : AirConditionerControlConstants.AcPower.POWER_OFF;
    }

    private ZonePower convertToZonePower(OnOffType command) {
        return command.equals(OnOffType.ON) ? ZoneControlConstants.ZonePower.POWER_ON
                : ZoneControlConstants.ZonePower.POWER_OFF;
    }

    private ZonePower convertToZonePower(StringType command) {
        return ZoneControlConstants.ZonePower.valueOf(command.toFullString());
    }

    @Override
    public void initialize() {
        final AirTouchConfiguration config = getConfigAs(AirTouchConfiguration.class);
        logger.debug("Using configuration: {}", config);

        // TODO: Initialize the handler.
        // The framework requires you to return from this method quickly, i.e. any network access must be done in
        // the background initialization below.
        // Also, before leaving this method a thing status from one of ONLINE, OFFLINE or UNKNOWN must be set. This
        // might already be the real thing status in case you can decide it directly.
        // In case you can not decide the thing status directly (e.g. for long running connection handshake using WAN
        // access or similar) you should set status UNKNOWN here and then decide the real status asynchronously in the
        // background.

        // set the thing status to UNKNOWN temporarily and let the background task decide for the real status.
        // the framework is then able to reuse the resources from the thing handler initialization.
        // we set this upfront to reliably check status updates in unit tests.
        updateStatus(ThingStatus.UNKNOWN);

        if (config.host != null) {

            // Example for background initialization:
            scheduler.execute(() -> {
                this.airtouch4Service.registerListener(this);
                try {
                    this.airtouch4Service.restart(config.host, config.port);
                    this.airtouch4Service.requestFullUpdate();
                } catch (IOException | AirtouchMessagingException e) {
                    handleConnectionException(e, config);
                }

            });
        }

        // These logging types should be primarily used by bindings
        // logger.trace("Example trace message");
        // logger.debug("Example debug message");
        // logger.warn("Example warn message");
        //
        // Logging to INFO should be avoided normally.
        // See https://www.openhab.org/docs/developer/guidelines.html#f-logging

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }

    protected void thingStructureChanged(AirtouchStatus airtouchStatus) {
        logger.debug("Updating thing structure");
        ThingBuilder thingBuilder = editThing();

        /*
         * Loop over the ACs and create channels for each AC.
         * Most installations probably have one AC Unit.
         * The channels are grouped together per AC unit.
         */
        airtouchStatus.getAcAbilities().values().forEach(ac -> {
            logger.debug("Attempting to add found ac {}", ac);
            ChannelGroupUID channelGroupUID = new ChannelGroupUID(
                    this.getThing().getUID() + ":ac-unit-" + ac.getAcNumber());
            logger.trace("ChannelGroupUID: '{}'", channelGroupUID.getAsString());

            Channel channelUnitPower = ChannelBuilder
                    .create(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_UNIT_POWER))
                    .withLabel(String.format("AC Unit Power - %s (%s)", ac.getAcName(), ac.getAcNumber()))
                    .withType(new ChannelTypeUID(AirTouchBindingConstants.BINDING_ID,
                            CHANNELUID_AIRCONDITIONER_UNIT_POWER))
                    .build();
            thingBuilder.withoutChannel(channelUnitPower.getUID()).withChannel(channelUnitPower);
            logger.trace("Added channel: '{}'", channelUnitPower.getLabel());

            Channel channelUnitSetPoint = ChannelBuilder
                    .create(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_UNIT_SETPOINT))
                    .withLabel(String.format("AC Unit Setpoint - %s (%s)", ac.getAcName(), ac.getAcNumber()))
                    .withType(new ChannelTypeUID(AirTouchBindingConstants.BINDING_ID,
                            CHANNELUID_AIRCONDITIONER_UNIT_SETPOINT))
                    .build();
            thingBuilder.withoutChannel(channelUnitSetPoint.getUID()).withChannel(channelUnitSetPoint);
            this.stateDescriptionFragmentCache.putStateDescriptionFragmentInCache(channelUnitSetPoint,
                    StateDescriptionFragmentBuilder.create().withMinimum(BigDecimal.valueOf(ac.getMinSetPoint()))
                            .withMaximum(BigDecimal.valueOf(ac.getMaxSetPoint())).build());
            logger.trace("Added channel: '{}'", channelUnitSetPoint.getLabel());

            Channel channelUnitTemperature = ChannelBuilder
                    .create(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_UNIT_TEMPERATURE))
                    .withLabel(String.format("AC Unit Temperature - %s (%s)", ac.getAcName(), ac.getAcNumber()))
                    .withType(new ChannelTypeUID(AirTouchBindingConstants.BINDING_ID,
                            CHANNELUID_AIRCONDITIONER_UNIT_TEMPERATURE))
                    .build();
            thingBuilder.withoutChannel(channelUnitTemperature.getUID()).withChannel(channelUnitTemperature);
            logger.trace("Added channel: '{}'", channelUnitTemperature.getLabel());

            Channel channelUnitMode = ChannelBuilder
                    .create(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_UNIT_MODE))
                    .withLabel(String.format("AC Unit Mode - %s (%s)", ac.getAcName(), ac.getAcNumber()))
                    .withType(new ChannelTypeUID(AirTouchBindingConstants.BINDING_ID,
                            CHANNELUID_AIRCONDITIONER_UNIT_MODE))
                    .build();
            thingBuilder.withoutChannel(channelUnitMode.getUID()).withChannel(channelUnitMode);
            List<StateOption> modes = ac.getSupportedModes().stream()
                    .map(m -> new StateOption(m.toString(), m.toString())).collect(Collectors.toList());
            this.stateDescriptionFragmentCache.putStateDescriptionFragmentInCache(channelUnitMode,
                    StateDescriptionFragmentBuilder.create().withOptions(modes).build());
            logger.trace("Added channel: '{}'", channelUnitMode.getLabel());

            Channel channelUnitFanspeed = ChannelBuilder
                    .create(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_UNIT_FANSPEED))
                    .withLabel(String.format("AC Unit Fanspeed - %s (%s)", ac.getAcName(), ac.getAcNumber()))
                    .withType(new ChannelTypeUID(AirTouchBindingConstants.BINDING_ID,
                            CHANNELUID_AIRCONDITIONER_UNIT_FANSPEED))
                    .build();
            thingBuilder.withoutChannel(channelUnitFanspeed.getUID()).withChannel(channelUnitFanspeed);
            List<StateOption> speeds = ac.getSupportedFanSpeeds().stream()
                    .map(m -> new StateOption(m.toString(), m.toString())).collect(Collectors.toList());
            this.stateDescriptionFragmentCache.putStateDescriptionFragmentInCache(channelUnitFanspeed,
                    StateDescriptionFragmentBuilder.create().withOptions(speeds).build());
            logger.trace("Added channel: '{}'", channelUnitFanspeed.getLabel());

            Channel channelUnitSpill = ChannelBuilder
                    .create(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_UNIT_SPILL))
                    .withLabel(String.format("AC Unit Spill - %s (%s)", ac.getAcName(), ac.getAcNumber()))
                    .withType(new ChannelTypeUID(AirTouchBindingConstants.BINDING_ID,
                            CHANNELUID_AIRCONDITIONER_UNIT_SPILL))
                    .build();
            thingBuilder.withoutChannel(channelUnitSpill.getUID()).withChannel(channelUnitSpill);
            logger.trace("Added channel: '{}'", channelUnitSpill.getLabel());

            Channel channelUnitTimer = ChannelBuilder
                    .create(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_UNIT_TIMER))
                    .withLabel(String.format("AC Unit Timer - %s (%s)", ac.getAcName(), ac.getAcNumber()))
                    .withType(new ChannelTypeUID(AirTouchBindingConstants.BINDING_ID,
                            CHANNELUID_AIRCONDITIONER_UNIT_TIMER))
                    .build();
            thingBuilder.withoutChannel(channelUnitTimer.getUID()).withChannel(channelUnitTimer);
            logger.trace("Added channel: '{}'", channelUnitTimer.getLabel());

        });

        /*
         * Loop over the Zones and create channels for each Zone.
         * The channels are grouped together per Zone.
         */
        airtouchStatus.getGroupNames().forEach((zoneId, zoneName) -> {
            logger.info("Attmpting to add found zone {}", zoneName);
            ChannelGroupUID channelGroupUID = new ChannelGroupUID(this.getThing().getUID() + ":ac-zone-" + zoneId);
            logger.info("ChannelGroupUID: '{}'", channelGroupUID.getAsString());

            Channel channelZonePower = ChannelBuilder
                    .create(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_ZONE_POWER))
                    .withLabel(String.format("Zone Power - %s", zoneName))
                    .withType(new ChannelTypeUID(AirTouchBindingConstants.BINDING_ID,
                            CHANNELUID_AIRCONDITIONER_ZONE_POWER))
                    .build();
            thingBuilder.withoutChannel(channelZonePower.getUID()).withChannel(channelZonePower);
            logger.trace("Added channel: '{}'", channelZonePower.getLabel());

            Channel channelZoneSetpoint = ChannelBuilder
                    .create(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_ZONE_SETPOINT))
                    .withLabel(String.format("Zone Setpoint - %s", zoneName))
                    .withType(new ChannelTypeUID(AirTouchBindingConstants.BINDING_ID,
                            CHANNELUID_AIRCONDITIONER_ZONE_SETPOINT))
                    .build();
            thingBuilder.withoutChannel(channelZoneSetpoint.getUID()).withChannel(channelZoneSetpoint);
            try {
                AirConditionerAbilityResponse ac = AirTouchStatusUtil.findAcForZone(airtouchStatus, zoneId);
                this.stateDescriptionFragmentCache.putStateDescriptionFragmentInCache(channelZoneSetpoint,
                        StateDescriptionFragmentBuilder.create().withMinimum(BigDecimal.valueOf(ac.getMinSetPoint()))
                                .withMaximum(BigDecimal.valueOf(ac.getMaxSetPoint())).build());
            } catch (IllegalArgumentException e) {
                logger.debug("Unable to set min/max setpoint values for zone '{}'", zoneId, e);
            }
            logger.trace("Added channel: '{}'", channelZoneSetpoint.getLabel());

            if (airtouchStatus.getGroupStatuses().get(zoneId).hasSensor()) {
                Channel channelZoneTemperature = ChannelBuilder
                        .create(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_ZONE_TEMPERATURE))
                        .withLabel(String.format("Zone Temperature - %s", zoneName))
                        .withType(new ChannelTypeUID(AirTouchBindingConstants.BINDING_ID,
                                CHANNELUID_AIRCONDITIONER_ZONE_TEMPERATURE))
                        .build();
                thingBuilder.withoutChannel(channelZoneTemperature.getUID()).withChannel(channelZoneTemperature);
                logger.trace("Added channel: '{}'", channelZoneTemperature.getLabel());

                Channel channelZoneBatteryLow = ChannelBuilder
                        .create(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_ZONE_BATTERY_LOW))
                        .withLabel(String.format("Zone Battery Low Indicator  - %s", zoneName))
                        .withType(new ChannelTypeUID(AirTouchBindingConstants.BINDING_ID,
                                CHANNELUID_AIRCONDITIONER_ZONE_BATTERY_LOW))
                        .build();
                thingBuilder.withoutChannel(channelZoneBatteryLow.getUID()).withChannel(channelZoneBatteryLow);
                logger.trace("Added channel: '{}'", channelZoneBatteryLow.getLabel());
            }

            Channel channelZoneFlowPercentage = ChannelBuilder
                    .create(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_ZONE_FLOW))
                    .withLabel(String.format("Zone AirFlow - %s", zoneName))
                    .withType(new ChannelTypeUID(AirTouchBindingConstants.BINDING_ID,
                            CHANNELUID_AIRCONDITIONER_ZONE_FLOW))
                    .build();
            thingBuilder.withoutChannel(channelZoneFlowPercentage.getUID()).withChannel(channelZoneFlowPercentage);
            logger.trace("Added channel: '{}'", channelZoneFlowPercentage.getLabel());

        });
        updateThing(thingBuilder.build());
    }

    public void allStatusUpdate(AirtouchStatus airtouchStatus) {
        logger.trace("Updating thing statuses: {}", airtouchStatus);
        airconditionerStatusUpdate(airtouchStatus.getAcStatuses());
        zoneStatusUpdate(airtouchStatus.getGroupStatuses());
    }

    @Override
    public void dispose() {
        logger.debug("Dispose called. Attempting to cleanup threads and services.");
        if (this.future != null && !this.future.isCancelled()) {
            logger.debug("Cancelling AirTouch polling update.");
            this.future.cancel(true);
        }
        logger.debug("Shutting down AirTouch service and connection.");
        this.airtouch4Service.shutdown();
        super.dispose();
    }

    @Override
    public void fullUpdate(AirtouchStatus status) {
        updateStatus(ThingStatus.ONLINE);
        thingStructureChanged(status);
        allStatusUpdate(status);
        final AirTouchConfiguration myConfig = getConfigAs(AirTouchConfiguration.class);
        logger.debug("Scheduling AirTouch polling update every {} seconds", myConfig.refreshInterval);
        this.future = scheduler.scheduleWithFixedDelay(() -> {
            try {
                airtouch4Service.requestStatusUpdate();
            } catch (IOException | AirtouchMessagingException e) {
                handleConnectionException(e, myConfig);
            }
        }, myConfig.refreshInterval, myConfig.refreshInterval, TimeUnit.SECONDS);
    }

    @Override
    public void airconditionerStatusUpdate(final List<AirConditionerStatusResponse> acStatuses) {
        final List<AirConditionerStatusResponse> myAcStatuses = acStatuses;
        myAcStatuses.forEach(ac -> {
            ChannelGroupUID channelGroupUID = new ChannelGroupUID(
                    this.getThing().getUID() + ":ac-unit-" + ac.getAcNumber());

            updateState(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_UNIT_POWER),
                    AirConditionerStatusConstants.PowerState.ON.equals(ac.getPowerstate()) ? OnOffType.ON
                            : OnOffType.OFF);
            logger.trace("Updating channel: '{}:{}'", channelGroupUID.getAsString(),
                    CHANNELUID_AIRCONDITIONER_UNIT_POWER);

            updateState(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_UNIT_SETPOINT),
                    QuantityType.valueOf(ac.getTargetSetpoint(), Units.CELSIUS));
            logger.trace("Updating channel: '{}:{}'", channelGroupUID.getAsString(),
                    CHANNELUID_AIRCONDITIONER_UNIT_SETPOINT);

            updateState(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_UNIT_TEMPERATURE),
                    QuantityType.valueOf(ac.getCurrentTemperature(), Units.CELSIUS));
            logger.trace("Updating channel: '{}:{}'", channelGroupUID.getAsString(),
                    CHANNELUID_AIRCONDITIONER_UNIT_TEMPERATURE);

            updateState(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_UNIT_MODE),
                    new StringType(ac.getMode().toString()));
            logger.trace("Updating channel: '{}:{}'", channelGroupUID.getAsString(),
                    CHANNELUID_AIRCONDITIONER_UNIT_MODE);

            updateState(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_UNIT_FANSPEED),
                    new StringType(ac.getFanSpeed().toString()));
            logger.trace("Updating channel: '{}:{}'", channelGroupUID.getAsString(),
                    CHANNELUID_AIRCONDITIONER_UNIT_FANSPEED);

            updateState(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_UNIT_SPILL),
                    ac.isSpill() ? OnOffType.ON : OnOffType.OFF);
            logger.trace("Updating channel: '{}:{}'", channelGroupUID.getAsString(),
                    CHANNELUID_AIRCONDITIONER_UNIT_SPILL);

            updateState(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_UNIT_TIMER),
                    ac.isAcTimer() ? OnOffType.ON : OnOffType.OFF);
            logger.trace("Updating channel: '{}:{}'", channelGroupUID.getAsString(),
                    CHANNELUID_AIRCONDITIONER_UNIT_TIMER);
        });
    }

    @Override
    public void zoneStatusUpdate(final List<ZoneStatusResponse> groupStatuses) {
        groupStatuses.forEach(zone -> {
            ChannelGroupUID channelGroupUID = new ChannelGroupUID(
                    this.getThing().getUID() + ":ac-zone-" + zone.getZoneNumber());
            updateState(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_ZONE_POWER),
                    ZoneStatusConstants.PowerState.ON.equals(zone.getPowerstate()) ? OnOffType.ON : OnOffType.OFF);
            logger.trace("Updating channel: '{}:{}'", channelGroupUID.getAsString(),
                    CHANNELUID_AIRCONDITIONER_ZONE_POWER);

            updateState(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_ZONE_SETPOINT),
                    new DecimalType(zone.getTargetSetpoint()));
            logger.trace("Updating channel: '{}:{}'", channelGroupUID.getAsString(),
                    CHANNELUID_AIRCONDITIONER_ZONE_SETPOINT);

            updateState(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_ZONE_FLOW),
                    new QuantityType<>(zone.getOpenPercentage(), Units.PERCENT));
            logger.trace("Updating channel: '{}:{}'", channelGroupUID.getAsString(),
                    CHANNELUID_AIRCONDITIONER_ZONE_FLOW);

            /*
             * If the install does not have the physical Zone Temperature devices, we can't determine the
             * Zone Temperature or the Device Battery Status.
             */
            if (groupStatuses.get(zone.getZoneNumber()).hasSensor()) {
                updateState(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_ZONE_TEMPERATURE),
                        new DecimalType(zone.getCurrentTemperature()));
                logger.trace("Updating channel: '{}:{}'", channelGroupUID.getAsString(),
                        CHANNELUID_AIRCONDITIONER_ZONE_TEMPERATURE);

                updateState(new ChannelUID(channelGroupUID, CHANNELUID_AIRCONDITIONER_ZONE_BATTERY_LOW),
                        Boolean.TRUE.equals(zone.isBatteryLow()) ? OnOffType.ON : OnOffType.OFF);
                logger.trace("Updating channel: '{}:{}'", channelGroupUID.getAsString(),
                        CHANNELUID_AIRCONDITIONER_ZONE_BATTERY_LOW);
            }
        });
    }
}
