package com.amway.integration.mashery;

import java.net.URI;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ApplicationRunner;

import org.springframework.beans.factory.annotation.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SpringBootApplication
public class WebsocketLogsListenerApp implements ApplicationRunner
{
	private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketLogsListenerApp.class);
	
	@Value("${websocket.url}")
	private String url;
	
	@Value("${websocket.pingtimems}")
	private Long pingtimems = 60000L;
	
	@Value("${websocket.asynctimeoutms}")
	private Long asynctimeoutms = null;
	
	@Value("${websocket.sessiontimeoutms}")
	private Long sessiontimeoutms = null;
	
	@Override
	public void run(ApplicationArguments args) throws Exception 
	{
		boolean websocketActive = false;

		// abort if config not set
		if(url == null || url.length() < 3)
		{
			LOGGER.error("Missing config property mashery.url, exiting!");
			System.exit(1);
		}
		// open websocket
		try {
		LOGGER.info("Instantiating new websocket client: " + url);
		WebsocketClientEndpoint clientEndPoint = new WebsocketClientEndpoint(new URI(url), asynctimeoutms, sessiontimeoutms);
		LOGGER.info("Instantiated new websocket client.");
		websocketActive = true;
		
		
		// always keep running
		while(websocketActive)
		{
			try { Thread.sleep(pingtimems); } catch(InterruptedException ie) { }
			try 
			{
				if (!clientEndPoint.getMessageReceivedSinceLastCheck()) {
					LOGGER.warn("Websocket not receiving messages since last check.");
					websocketActive = false;
					clientEndPoint.Close();
					LOGGER.warn("Requesting websocket client restart.");
					break;
				} else {
					LOGGER.debug("Websocket active and receiving messages.");
				}
			} catch(Exception e) {
				LOGGER.error("Exception checking the endpoint connection: " + e.toString());
				websocketActive = false;
				break;
			}
		}
		} catch(Exception e) {
			LOGGER.error("Exception while instantiating new websocket client.");
		}
	}
	
	public static void main(String[] args) 
	{
		while(true) {
			try {
				LOGGER.info("Opening websocket listener.");
				ConfigurableApplicationContext ctx = SpringApplication.run(WebsocketLogsListenerApp.class, args);
				LOGGER.info("Closing websocket listener.");
				ctx.close();
				
			}catch (Exception e){
				LOGGER.error("Websocket listener exception: " + e.toString());
			}
		}
	}
}