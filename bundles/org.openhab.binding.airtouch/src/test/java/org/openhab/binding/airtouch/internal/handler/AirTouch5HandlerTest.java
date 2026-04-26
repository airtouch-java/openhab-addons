package org.openhab.binding.airtouch.internal.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.binding.airtouch.internal.AirTouchDynamicStateDescriptionProvider;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.ThingHandlerCallback;

import airtouch.Request;
import airtouch.constant.ZoneControlConstants;
import airtouch.v5.constant.MessageConstants;
import airtouch.v5.constant.MessageConstants.Address;
import airtouch.v5.constant.MessageConstants.MessageType;
import airtouch.v5.handler.ZoneControlHandler;

@ExtendWith(MockitoExtension.class)
class AirTouch5HandlerTest {

    private @Mock ThingHandlerCallback callbackMock;
    private @Mock Thing thingMock;
    private @Mock AirTouchService<MessageConstants.Address> airTouch4Service;
    private ChannelGroupUID acZone0ChannelGroupUID = new ChannelGroupUID(
            "airtouch:airtouch4-controller:mac_address:ac-zone-0");
    private AirTouch5Handler handler;
    private ArgumentCaptor<Request<MessageConstants.Address>> requestCaptor;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setupClass() {
        when(airTouch4Service.getNextRequestId()).thenReturn(100);
        handler = new AirTouch5Handler(thingMock, airTouch4Service, new AirTouchDynamicStateDescriptionProvider());
        requestCaptor = ArgumentCaptor.forClass(Request.class);
    }

    @Test
    void testHandlePowerOnZone0Command() throws IOException {
        handler.handleCommand(new ChannelUID(acZone0ChannelGroupUID, "airconditioner-zone-power"), OnOffType.ON);

        Request<MessageConstants.Address> request = ZoneControlHandler.requestBuilder(0)
                .power(ZoneControlConstants.ZonePower.POWER_ON).build(this.airTouch4Service.getNextRequestId());

        verify(airTouch4Service).sendRequest(requestCaptor.capture());
        assertEquals(MessageType.CONTROL_OR_STATUS, requestCaptor.getValue().getMessageType());
        assertEquals(100, requestCaptor.getValue().getMessageId());
        assertEquals(Address.STANDARD_SEND, requestCaptor.getValue().getAddress());
        assertEquals(request.getHexString(), requestCaptor.getValue().getHexString());
    }

    @Test
    void testHandlePowerOnZone0StringCommand() throws IOException {
        handler.handleCommand(new ChannelUID(acZone0ChannelGroupUID, "airconditioner-zone-power"),
                new StringType("POWER_ON"));

        Request<MessageConstants.Address> request = ZoneControlHandler.requestBuilder(0)
                .power(ZoneControlConstants.ZonePower.POWER_ON).build(this.airTouch4Service.getNextRequestId());

        verify(airTouch4Service).sendRequest(requestCaptor.capture());
        assertEquals(MessageType.CONTROL_OR_STATUS, requestCaptor.getValue().getMessageType());
        assertEquals(100, requestCaptor.getValue().getMessageId());
        assertEquals(Address.STANDARD_SEND, requestCaptor.getValue().getAddress());
        assertEquals(request.getHexString(), requestCaptor.getValue().getHexString());
    }

    @Test
    void testHandlePowerOffZone0Command() throws IOException {
        handler.handleCommand(new ChannelUID(acZone0ChannelGroupUID, "airconditioner-zone-power"), OnOffType.OFF);

        Request<MessageConstants.Address> request = ZoneControlHandler.requestBuilder(0)
                .power(ZoneControlConstants.ZonePower.POWER_OFF).build(this.airTouch4Service.getNextRequestId());
        verify(airTouch4Service).sendRequest(requestCaptor.capture());
        assertEquals(MessageType.CONTROL_OR_STATUS, requestCaptor.getValue().getMessageType());
        assertEquals(100, requestCaptor.getValue().getMessageId());
        assertEquals(Address.STANDARD_SEND, requestCaptor.getValue().getAddress());
        assertEquals(request.getHexString(), requestCaptor.getValue().getHexString());
    }

    @Test
    void testHandlePowerOffZone0StringCommand() throws IOException {
        handler.handleCommand(new ChannelUID(acZone0ChannelGroupUID, "airconditioner-zone-power"),
                new StringType("POWER_OFF"));

        Request<MessageConstants.Address> request = ZoneControlHandler.requestBuilder(0)
                .power(ZoneControlConstants.ZonePower.POWER_OFF).build(this.airTouch4Service.getNextRequestId());
        verify(airTouch4Service).sendRequest(requestCaptor.capture());
        assertEquals(MessageType.CONTROL_OR_STATUS, requestCaptor.getValue().getMessageType());
        assertEquals(100, requestCaptor.getValue().getMessageId());
        assertEquals(Address.STANDARD_SEND, requestCaptor.getValue().getAddress());
        assertEquals(request.getHexString(), requestCaptor.getValue().getHexString());
    }

    @Test
    void testHandleTurboPowerZone0Command() throws IOException {
        handler.handleCommand(new ChannelUID(acZone0ChannelGroupUID, "airconditioner-zone-power"),
                new StringType("TURBO_POWER"));

        Request<MessageConstants.Address> request = ZoneControlHandler.requestBuilder(0)
                .power(ZoneControlConstants.ZonePower.TURBO_POWER).build(this.airTouch4Service.getNextRequestId());

        verify(airTouch4Service).sendRequest(requestCaptor.capture());
        assertEquals(MessageType.CONTROL_OR_STATUS, requestCaptor.getValue().getMessageType());
        assertEquals(100, requestCaptor.getValue().getMessageId());
        assertEquals(Address.STANDARD_SEND, requestCaptor.getValue().getAddress());
        assertEquals(request.getHexString(), requestCaptor.getValue().getHexString());
    }
}
