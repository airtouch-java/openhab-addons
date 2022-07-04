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
package org.openhab.binding.airtouch.internal;

import static org.openhab.binding.airtouch.internal.AirTouchBindingConstants.*;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.airtouch.internal.handler.AirTouch4Handler;
import org.openhab.binding.airtouch.internal.handler.AirTouch4ServiceImpl;
import org.openhab.binding.airtouch.internal.handler.AirTouchService;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AirTouch4HandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Nathaniel Wolfe - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.airtouch", service = ThingHandlerFactory.class)
public class AirTouchHandlerFactory extends BaseThingHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(AirTouchHandlerFactory.class);
    private final AirTouchDynamicStateDescriptionProvider dynamicStateDescriptionProvider;
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(AIRTOUCH4_CONTROLLER_THING_TYPE);

    @Activate
    public AirTouchHandlerFactory(@Reference AirTouchDynamicStateDescriptionProvider dynamicStateDescriptionProvider) {
        this.dynamicStateDescriptionProvider = dynamicStateDescriptionProvider;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (AIRTOUCH4_CONTROLLER_THING_TYPE.equals(thingTypeUID)) {
            logger.debug("Creating AirTouchAirConditionerHandler for '{}'", thing.getThingTypeUID());
            AirTouchService<airtouch.v4.constant.MessageConstants.Address> airTouch4Service = new AirTouch4ServiceImpl();
            return new AirTouch4Handler(thing, airTouch4Service, this.dynamicStateDescriptionProvider);
        }

        return null;
    }
}
