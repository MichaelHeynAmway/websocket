package com.amway.integration.mashery;

import java.net.URI;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;


import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.PongMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClientEndpoint
public class WebsocketClientEndpoint 
{
	private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketClientEndpoint.class);
	
	Session userSession = null;
	long lastPingSent = 0L;
	long lastPongReceived = 0L;
	boolean gotPing = true;
	
	URI endpointURI;
	Long asynctimeoutms;
	Long sessiontimeoutms;
	
	public WebsocketClientEndpoint(URI endpointURI, Long asynctimeoutms, Long sessiontimeoutms)
	{
		this.endpointURI = endpointURI;
		this.asynctimeoutms = asynctimeoutms;
		this.sessiontimeoutms = sessiontimeoutms;
		this.userSession = connect();
	}
	
	private Session connect()
	{
		try 
		{
			WebSocketContainer container = ContainerProvider.getWebSocketContainer();
			if(asynctimeoutms != null)
			{	container.setAsyncSendTimeout(asynctimeoutms); }
			if(sessiontimeoutms != null)
			{	container.setDefaultMaxSessionIdleTimeout(sessiontimeoutms); }
			return container.connectToServer(this, endpointURI);
		} catch (Exception e) {
			LOGGER.error("unable to connect", e);
			throw new RuntimeException(e);
		}
	}
	
	@OnOpen
	public void onOpen(Session userSession) 
	{
		this.userSession = userSession; 
		LOGGER.info("Connection opened: " + userSession);
	}
	
	@OnClose
	public void onClose(Session userSession, CloseReason reason) 
	{
		this.userSession = null; 
		LOGGER.error("Connection closed: " + reason);
		System.exit(1);
	}
	
	
    public void onPong(PongMessage pongMessage) 
	{
		lastPongReceived = System.currentTimeMillis();
		long latency = lastPongReceived - lastPingSent;
		gotPing = true;
		LOGGER.info("Pong after " + latency + " ms " + new String(pongMessage.getApplicationData().array(), StandardCharsets.UTF_8));
	}
	
	public synchronized void sendPing() throws IOException
	{
		if(!gotPing)
		{
			LOGGER.error("missed ping from " + lastPingSent + " will attempt reconnect");
			// detected disconnect
			this.userSession = connect();
			// skip this call
			return;
		}
		// clear status
		gotPing = false;
		lastPingSent = System.currentTimeMillis();
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(0, lastPingSent);
		userSession.getAsyncRemote().sendPing(buffer);
		LOGGER.info("pinging " + lastPingSent);
	}
	
	public void sendPong() throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(0, System.currentTimeMillis());
		userSession.getAsyncRemote().sendPong(buffer);
		LOGGER.info("ponging " + buffer);
	}
	
	@OnMessage
	public void onMessage(byte[] message) {
        try {
			System.out.println(new String(message, "utf-8"));
		} catch (UnsupportedEncodingException e) {}

	}
}