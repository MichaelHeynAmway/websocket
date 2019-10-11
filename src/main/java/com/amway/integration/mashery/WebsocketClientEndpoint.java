package com.amway.integration.mashery;

import java.net.URI;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.websocket.OnError;

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
			LOGGER.error("Unable to connect.");
			LOGGER.debug("Unable to connect exception reason: " , e);
			throw new RuntimeException(e);
		}
	}
	
	@OnOpen
	public void onOpen(Session userSession) 
	{
		this.userSession = userSession;
		LOGGER.info("Connection opened.");
		LOGGER.debug("Connection opened session context: " + userSession);
	}
	
	@OnClose
	public void onClose(Session userSession, CloseReason reason) 
	{
		this.userSession = null; 
		LOGGER.info("Connection closed.");
		LOGGER.debug("Connection closed reason: " + reason.getReasonPhrase());
		
	}
		
	@OnMessage
	public void onMessage(byte[] message) throws Exception {
		
       try {
    	   msgLogger.info(new String(message, "utf-8"));
    	   messageReceived = true;
		} catch (UnsupportedEncodingException e) {
			LOGGER.debug("Unsupported encoding", e);
		}
	}
	
	@OnError
	public void onError(Session session,Throwable t) {
		LOGGER.error("Websocket client error: ",t);
	}

	public void Close() throws IOException {
		try {
		userSession.close();
		}catch (IOException io) {
			LOGGER.debug("Close Exception: ",io);
			throw new RuntimeException(io);
		}
	}
	
   public boolean isClosed() {
        return userSession == null || !userSession.isOpen();
    }
   
   public boolean getHealthCheck() {
	   boolean lastState = messageReceived;
	   messageReceived = false;
	   return lastState;
   }

}