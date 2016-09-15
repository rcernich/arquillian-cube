package org.arquillian.cube.openshift.impl.client;

import io.undertow.client.ClientCallback;
import io.undertow.client.ClientExchange;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientStatistics;
import io.undertow.client.spdy.SpdyClientConnection;
import io.undertow.protocols.spdy.SpdyChannel;

/**
 * Helper class that overrides {@link sendRequest} to make it synchronized.
 * Can be safely removed when we depend on Undertow 1.3.25
 */
public class PortForwarderSpdyClientConnection extends SpdyClientConnection {

    public PortForwarderSpdyClientConnection(SpdyChannel spdyChannel, ClientStatistics clientStatistics) {
        super(spdyChannel, clientStatistics);
    }

    @Override
    public synchronized void sendRequest(ClientRequest request, ClientCallback<ClientExchange> clientCallback) {
        super.sendRequest(request, clientCallback);
    }
}
