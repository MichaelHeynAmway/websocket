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
import javax.websocket.OnError;


//import org.eclipse.jetty.websocket.api.Session;
//import org.eclipse.jetty.websocket.api.StatusCode;
//import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
//import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
//import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
//import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
//import org.eclipse.jetty.websocket.api.annotations.WebSocket;
//import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
//import org.eclipse.jetty.websocket.client.WebSocketClient;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClientEndpoint
public class WebsocketClientEndpoint 
{
	private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketClientEndpoint.class);
	static final Logger msgLogger = LoggerFactory.getLogger("ES");
	
	Session userSession = null;
	long lastPingSent = 0L;
	long lastPongReceived = 0L;
	boolean gotPing = true;
	
	URI endpointURI;
	Long asynctimeoutms;
	Long sessiontimeoutms;
	boolean messageReceived = false;
	
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
		
	}
	
	
    public void onPong(PongMessage pongMessage) 
	{
		lastPongReceived = System.currentTimeMillis();
		long latency = lastPongReceived - lastPingSent;
		gotPing = true;
		LOGGER.info("Pong after " + latency + " ms " + new String(pongMessage.getApplicationData().array(), StandardCharsets.UTF_8));
	}
	
	public synchronized void sendPing() throws Exception
	{
		try {
		if(!gotPing)
		{
			// detected disconnect
			throw new Exception("Ping IO Exception");
			//this.userSession = connect();
			// skip this call
			//return;
		}
		// clear status
		gotPing = false;
		lastPingSent = System.currentTimeMillis();
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(0, lastPingSent);
		userSession.getAsyncRemote().sendPing(buffer);
		} catch(Exception e) {
			throw new Exception("Ping IO Exception");
		}
	}
	
	public void sendPong() throws IOException
	{
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(0, System.currentTimeMillis());
		userSession.getAsyncRemote().sendPong(buffer);
	}
	
	@OnMessage
	public void onMessage(byte[] message) throws Exception {
		
       try {
    	   msgLogger.info(new String(message, "utf-8"));
    	   messageReceived = true;
		} catch (UnsupportedEncodingException e) {}
	}
	
	@OnError
	public void onError(Session session,Throwable t) {
		LOGGER.error(t.toString());
	}
	public void Close() throws IOException {
		userSession.close();
	}
	
   public boolean isClosed() {
        return userSession == null || !userSession.isOpen();
    }
   
   public boolean getMessageReceivedSinceLastCheck() {
	   boolean lastState = messageReceived;
	   messageReceived = false;
	   return lastState;
   }

}