package org.openhab.binding.airtouch.internal.discovery;

import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.AIRTOUCH4_CONTROLLER_THING_TYPE;
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

    public AirTouchDiscoveryService() {
        super(Set.of(AIRTOUCH4_CONTROLLER_THING_TYPE), AirTouchBindingConstants.DISCOVERY_SCAN_TIMEOUT_SECONDS, false);
        airtouch4Broadcaster = new AirtouchDiscoverer(AirtouchVersion.AIRTOUCH4, this);
    }

    @Override
    protected void startScan() {
        airtouch4Broadcaster.start();
    }

    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        airtouch4Broadcaster.shutdown();
    }

    @Override
    public void handleResponse(@Nullable AirtouchDiscoveryBroadcastResponse response) {
        onAirtouchFoundInternal(response);
    }

    private void onAirtouchFoundInternal(@Nullable AirtouchDiscoveryBroadcastResponse airtouch) {
        if (airtouch != null) {
            logger.info("Found '{}' at '{}' with id '{}'", airtouch.getAirtouchVersion(), airtouch.getHostAddress(),
                    airtouch.getAirtouchId());
            ThingUID thingUID = getThingUID(airtouch);
            logger.info("ThingUID is: '{}'", thingUID);
            Map<String, Object> properties = new HashMap<>(1);
            properties.put(PROPERTY_AIRTOUCH_ID, airtouch.getAirtouchId());
            properties.put(PROPERTY_AIRTOUCH_HOST, airtouch.getHostAddress());
            properties.put(PROPERTY_AIRTOUCH_PORT, airtouch.getPortNumber());
            properties.put(PROPERTY_AIRTOUCH_REFRESH_INTERVAL, 60);
            properties.put(PROPERTY_AIRTOUCH_VERSION, airtouch.getAirtouchVersion().getVersionIdentifier());
            properties.put(PROPERTY_AIRTOUCH_MAC_ADDRESS, airtouch.getMacAddress());
            properties.put(PROPERTY_AIRTOUCH_UID, airtouch.getMacAddress().replace(":", "").toLowerCase());
            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                    .withThingType(AIRTOUCH4_CONTROLLER_THING_TYPE)
                    .withLabel(airtouch.getAirtouchVersion().getVersionIdentifier())
                    .withRepresentationProperty(PROPERTY_AIRTOUCH_HOST).build();
            thingDiscovered(discoveryResult);
            stopScan();
        }
    }

    private ThingUID getThingUID(AirtouchDiscoveryBroadcastResponse airtouch) {
        return new ThingUID(AIRTOUCH4_CONTROLLER_THING_TYPE.getAsString() + ":"
                + airtouch.getMacAddress().replace(":", "").toLowerCase());
    }
}
