package org.openhab.binding.airtouch.internal.discovery;

import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.AIRTOUCH4_CONTROLLER_THING_TYPE;
import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.AIRTOUCH5_CONTROLLER_THING_TYPE;
import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.PROPERTY_AIRTOUCH_CONSOLE_ID;
import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.PROPERTY_AIRTOUCH_HOST;
import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.PROPERTY_AIRTOUCH_ID;
import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.PROPERTY_AIRTOUCH_MAC_ADDRESS;
import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.PROPERTY_AIRTOUCH_PORT;
import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.PROPERTY_AIRTOUCH_REFRESH_INTERVAL;
import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.PROPERTY_AIRTOUCH_UID;
import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.PROPERTY_AIRTOUCH_VERSION;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.airtouch.internal.AirTouchBindingConstants;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import airtouch.AirtouchVersion;
import airtouch.discovery.AirtouchDiscoverer;
import airtouch.discovery.AirtouchDiscoveryBroadcastResponseCallback;

@Component(service = DiscoveryService.class, immediate = true, configurationPid = "discovery.airtouch")
@NonNullByDefault
public class AirTouchDiscoveryService extends AbstractDiscoveryService
        implements AirtouchDiscoveryBroadcastResponseCallback {

    private final Logger logger = LoggerFactory.getLogger(AirTouchDiscoveryService.class);
    private AirtouchDiscoverer airtouch4Broadcaster;
    private AirtouchDiscoverer airtouch5Broadcaster;
    private String scanInputLabel = "AirTouch Version to discover";
    private String scanInputDescription = "Must be one of '4' or '5' (withouth quotes)";

    public AirTouchDiscoveryService() {
        super(Set.of(AIRTOUCH4_CONTROLLER_THING_TYPE, AIRTOUCH5_CONTROLLER_THING_TYPE),
                AirTouchBindingConstants.DISCOVERY_SCAN_TIMEOUT_SECONDS, false);
        airtouch4Broadcaster = new AirtouchDiscoverer(AirtouchVersion.AIRTOUCH4, this);
        airtouch5Broadcaster = new AirtouchDiscoverer(AirtouchVersion.AIRTOUCH5, this);
    }

    @Override
    protected void startScan() {
        logger.info("Starting Discovery for AirTouch5");
        airtouch5Broadcaster.start();
        logger.info("Starting Discovery for AirTouch4");
        airtouch4Broadcaster.start();
    }

    @Override
    protected void startScan(String input) {
        if ("5".equals(input)) {
            logger.info("Starting Discovery for AirTouch5");
            airtouch5Broadcaster.start();
        } else if ("4".equals(input)) {
            logger.info("Starting Discovery for AirTouch4");
            airtouch4Broadcaster.start();
        } else {
            startScan();
        }
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        logger.info("Stopping Discovery");
        airtouch4Broadcaster.shutdown();
        airtouch5Broadcaster.shutdown();
    }

    @Override
    public void handleResponse(@Nullable AirtouchDiscoveryBroadcastResponse response) {
        onAirtouchFoundInternal(response);
    }

    @Override
    public boolean isScanInputSupported() {
        return getScanInputLabel() != null && getScanInputDescription() != null;
    }

    @Override
    public @Nullable String getScanInputLabel() {
        return scanInputLabel;
    }

    @Override
    public @Nullable String getScanInputDescription() {
        return scanInputDescription;
    }

    private void onAirtouchFoundInternal(@Nullable AirtouchDiscoveryBroadcastResponse airtouch) {
        if (airtouch != null) {
            logger.info("Found '{}' at '{}' with id '{}'", airtouch.getAirtouchVersion(), airtouch.getHostAddress(),
                    airtouch.getAirtouchId());
            ThingUID thingUID = getThingUID(airtouch);
            logger.info("ThingUID is: '{}'", thingUID);
            Map<String, Object> properties = HashMap.newHashMap(1);
            properties.put(PROPERTY_AIRTOUCH_HOST, airtouch.getHostAddress());
            properties.put(PROPERTY_AIRTOUCH_PORT, airtouch.getPortNumber());
            properties.put(PROPERTY_AIRTOUCH_REFRESH_INTERVAL, 60);
            properties.put(PROPERTY_AIRTOUCH_VERSION, airtouch.getAirtouchVersion().getVersionIdentifier());
            if (AirtouchVersion.AIRTOUCH4.equals(airtouch.getAirtouchVersion())) {
                properties.put(PROPERTY_AIRTOUCH_MAC_ADDRESS, airtouch.getMacAddress());
            }
            if (AirtouchVersion.AIRTOUCH5.equals(airtouch.getAirtouchVersion())) {
                properties.put(PROPERTY_AIRTOUCH_ID, airtouch.getAirtouchId());
                properties.put(PROPERTY_AIRTOUCH_CONSOLE_ID, airtouch.getConsoleId());
            }
            properties.put(PROPERTY_AIRTOUCH_UID, getIdentifier(airtouch));
            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                    .withThingType(resolveThingType(airtouch))
                    .withLabel(airtouch.getAirtouchVersion().getVersionIdentifier())
                    .withRepresentationProperty(PROPERTY_AIRTOUCH_HOST).build();
            thingDiscovered(discoveryResult);
            stopScan();
        }
    }

    private ThingUID getThingUID(AirtouchDiscoveryBroadcastResponse airtouch) {
        return new ThingUID(resolveThingType(airtouch) + ":" + getIdentifier(airtouch));
    }

    private String getIdentifier(AirtouchDiscoveryBroadcastResponse airtouch) {
        if (AirtouchVersion.AIRTOUCH5.equals(airtouch.getAirtouchVersion())) {
            return airtouch.getConsoleId().toLowerCase();
        }
        return airtouch.getMacAddress().replace(":", "").toLowerCase();
    }

    private ThingTypeUID resolveThingType(AirtouchDiscoveryBroadcastResponse airtouch) {
        if (AirtouchVersion.AIRTOUCH5.equals(airtouch.getAirtouchVersion())) {
            return AIRTOUCH5_CONTROLLER_THING_TYPE;
        }
        return AIRTOUCH4_CONTROLLER_THING_TYPE;
    }
}
