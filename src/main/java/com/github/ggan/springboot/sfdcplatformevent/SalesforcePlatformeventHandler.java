package com.github.ggan.springboot.sfdcplatformevent;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.Base64;
import java.util.prefs.Preferences;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class SalesforcePlatformeventHandler {
	private static Logger logger = LoggerFactory.getLogger(SalesforcePlatformeventHandler.class);

	@Value("${boomi.api.baseUrl}")
	private String boomiBaseUrl;

	@Value("${boomi.api.username}")
	private String boomiUsername;

	@Value("${boomi.api.password}")
	private String boomiPassword;

	public synchronized void handleRequest(String payload, String salesforceEventName, Long replayId) {
		logger.info("Received payload from platform event " + salesforceEventName + " with replayId: " + replayId);
		logger.info(payload);
		try {
			URL url = null;
			if (salesforceEventName.equals("/event/Cloud_News__e")) {
				url = new URL(boomiBaseUrl + "/cloud_news/");
			} else if (salesforceEventName.equals("/event/account_update__e")) {
				url = new URL(boomiBaseUrl + "/account_update/");
			} else {
				throw new RuntimeException(salesforceEventName + " not handled!");
			}
			String encoding = Base64.getEncoder().encodeToString((boomiUsername + ":" + boomiPassword).getBytes());

			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Authorization", "Basic " + encoding);

			connection.setDoOutput(true);

			try (OutputStream os = connection.getOutputStream()) {
				byte[] input = payload.getBytes("utf-8");
				os.write(input, 0, input.length);
			}

			InputStream content = (InputStream) connection.getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(content));
			String line;
			while ((line = in.readLine()) != null) {
				System.out.println(line);
			}
			writeReplayId(salesforceEventName, replayId);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void writeReplayId(String event, Long replayId) {
		Preferences prefs = Preferences.userRoot().node(Application.class.getName());
		prefs.putLong(event, replayId);
	}

}
