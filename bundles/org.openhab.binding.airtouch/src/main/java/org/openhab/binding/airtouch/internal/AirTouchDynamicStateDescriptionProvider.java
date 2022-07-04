package org.openhab.binding.airtouch.internal;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.type.DynamicStateDescriptionProvider;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateOption;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AirTouchDynamicStateDescriptionProvider} is responsible for resolving the options available
 * as State for Channels.
 * <p>
 *
 * OpenHab has the concept of a StateDescription. This is what is defined in the <code>thing-types.xml</code> file
 * under the "state" xml tag.
 * eg.
 * <code>&lt;state step="1" pattern="%d °C" readOnly="true"/&gt;</code>
 * <p>
 * This value is used to configure available states an item can have. It would be nice to just use the values
 * defined in the XML. However, for the AirTouch, we need to read in the valid min/max setpoint values, and the
 * available FanSpeeds and Operating Modes. The available options (eg, AUTO,COOL,HEAT) can differ from one
 * AirTouch install to another.
 * <p>
 * Additionally, we want to setup most of the "state" options in XML. Where we need to change specific states
 * after retrieving configuration from the AirTouch, we then want to merge the values from the
 * <code>thing-types.xml</code>
 * file with the config and produce an updated StateDescription.
 *
 * This service has two functions.
 * <ol>
 * <li>Act as a "resolver" so that when OpenHab wants some StateDescriptions, we can handle the call and return ones for
 * our binding.
 * <li>A cache to handle StateDescriptionFragments as created by our Handler.
 * </ol>
 * A StateDescriptionFragment is a partially filled in StateDescription.
 * When our Handler creates the channels, it also assembles StateDescriptionFragments for states that require modication
 * after
 * reading the AirTouch config.
 * <p>
 * These StateDescriptionFragments are stored inside this service. When OpenHab later asks for a StateDescription, it
 * passes in the one
 * it read from our thing-types.xml. In some cases we want to enhance this StateDescription with a
 * StateDescriptionFragment we created in
 * the handler.
 * <p>
 * So, we read the StateDescriptionFragment from our cache and then build a new StateDescription by merging the passed
 * in StateDescription
 * and our StateDescriptionFragment. We then store this merged StateDescription in another cache or quick retrieval next
 * time.
 *
 * @see #getStateDescription(Channel, StateDescription, Locale)
 * @see #putStateDescriptionFragmentInCache(Channel, StateDescriptionFragment)
 *
 * @author Nathaniel Wolfe - Initial contribution
 */
@Component(service = { DynamicStateDescriptionProvider.class, AirTouchDynamicStateDescriptionProvider.class })
@NonNullByDefault
public class AirTouchDynamicStateDescriptionProvider
        implements DynamicStateDescriptionProvider, AirTouchStateDescriptionFragmentCache {

    private final Logger logger = LoggerFactory.getLogger(AirTouchDynamicStateDescriptionProvider.class);

    private Map<@NonNull Channel, @NonNull StateDescription> stateDescriptionCache = new ConcurrentHashMap<>();
    private Map<@NonNull Channel, @NonNull StateDescriptionFragment> stateDescriptionFragmentCache = new ConcurrentHashMap<>();

    /**
     * If the Channel is one from our binding and we have a StateDescriptionFragment in our cache
     * merge the StateDescriptionFragment with the passed in StateDescription.
     * Store this merged value for next time it is requested.
     */
    @Override
    public @Nullable StateDescription getStateDescription(Channel channel,
            @Nullable StateDescription originalStateDescription, @Nullable Locale locale) {
        if (AirTouchBindingConstants.BINDING_ID.equals(channel.getUID().getBindingId())) {
            logger.trace("state requested for channel {}: Existing description: {}", channel.getUID(),
                    originalStateDescription);
            if (this.stateDescriptionCache.containsKey(channel)) {
                logger.trace("Returning cached StateDescription: {}", this.stateDescriptionCache.get(channel));
                return this.stateDescriptionCache.get(channel);
            } else if (originalStateDescription != null && this.stateDescriptionFragmentCache.containsKey(channel)) {
                logger.trace("Fragment cache contains channel. Attempting to build fragment for {} from {} ",
                        channel.getUID(), this.stateDescriptionFragmentCache.get(channel));

                StateDescriptionFragmentBuilder fragmentBuilder = StateDescriptionFragmentBuilder
                        .create(originalStateDescription);
                StateDescriptionFragment cachedFragment = this.stateDescriptionFragmentCache.get(channel);

                // If the handler added a minimum, set it on the new state
                BigDecimal min = cachedFragment.getMinimum();
                if (min != null) {
                    fragmentBuilder.withMinimum(min);
                }
                // If the handler added a maximum, set it on the new state
                BigDecimal max = cachedFragment.getMaximum();
                if (max != null) {
                    fragmentBuilder.withMaximum(max);
                }
                // If the handler added a set of options, set them on the new state
                List<StateOption> options = cachedFragment.getOptions();
                if (options != null) {
                    fragmentBuilder.withOptions(options);
                }

                StateDescription fragment = fragmentBuilder.build().toStateDescription();
                if (fragment != null) {
                    logger.trace("Fragment created for {} as {} ", channel.getUID(), fragment);
                    this.stateDescriptionCache.put(channel, fragment);
                    return this.stateDescriptionCache.get(channel);
                }
            }
        }
        return null;
    }

    @Override
    public void putStateDescriptionFragmentInCache(@NonNull Channel channel,
            @NonNull StateDescriptionFragment stateDescriptionFragment) {
        logger.trace("Saving fragment to cache: channel: {}, fragment: {}", channel.getUID(), stateDescriptionFragment);
        // Put the fragment into the cache.
        this.stateDescriptionFragmentCache.put(channel, stateDescriptionFragment);
        // then clear the old cached assembled description. Our new fragment will be used to assemble the
        // stateDescription next time it is requested.
        this.stateDescriptionCache.remove(channel);
    }
}
