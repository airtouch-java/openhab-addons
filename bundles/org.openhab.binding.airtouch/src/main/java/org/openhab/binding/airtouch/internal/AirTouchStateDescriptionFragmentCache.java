package org.openhab.binding.airtouch.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Channel;
import org.openhab.core.types.StateDescriptionFragment;

@NonNullByDefault
public interface AirTouchStateDescriptionFragmentCache {

    void putStateDescriptionFragmentInCache(Channel channel, StateDescriptionFragment stateDescriptionFragment);
}
