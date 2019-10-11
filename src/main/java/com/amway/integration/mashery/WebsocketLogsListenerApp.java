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
		LOGGER.debug("Instantiating new websocket client: " + url);
		WebsocketClientEndpoint clientEndPoint = new WebsocketClientEndpoint(new URI(url), asynctimeoutms, sessiontimeoutms);
		LOGGER.debug("Instantiated new websocket client and ready to receive messages.");
		websocketActive = true;
		
		
		// always keep running
		while(websocketActive)
		{
			try {
				LOGGER.debug("Websocket application listener sleeping.");
				Thread.sleep(pingtimems); 
				} catch(InterruptedException ie) { }
			try 
			{
				if (!clientEndPoint.getHealthCheck()) {
					LOGGER.warn("Websocket failed health check.");
					websocketActive = false;
					LOGGER.debug("Requesting close websocket connection.");
					clientEndPoint.Close();
					LOGGER.warn("Requesting websocket client restart.");
					break;
				} else {
					LOGGER.debug("Websocket passed health check.");
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
				LOGGER.info("Opening websocket client.");
				ConfigurableApplicationContext ctx = SpringApplication.run(WebsocketLogsListenerApp.class, args);
				LOGGER.warn("Closing websocket client.");
				ctx.close();
				
			}catch (Exception e){
				LOGGER.error("Websocket client exception: " + e.toString());
			}
		}
	}
}