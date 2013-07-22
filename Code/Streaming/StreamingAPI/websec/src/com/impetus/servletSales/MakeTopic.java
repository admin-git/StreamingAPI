package com.impetus.servletSales;

import java.io.ByteArrayInputStream;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * @author nidhi.neema Java Program for creating a PushTopic for streaming with
 *         sfdc. usage java MakeTopic <topicname> <query> <topicname> is the
 *         name of the topic. <query> Event Notifications are generated for
 *         updates that match the query.
 */
public class MakeTopic {
	// Get credentials etc from environment
	private static final String LOGIN_SERVER = System
			.getenv("https://login.salesforce.com");
	private static final String USERNAME = System
			.getenv("karishma@impetus.com");
	private static final String PASSWORD = System.getenv("welcome123");

	private static final String CLIENT_ID = System
			.getenv("3MVG9A2kN3Bn17hsQ4_qm06eToy9a_stk.N.j0JT63dR6Cz_SqVBDVDr_1cMzJ14OJOIsqAVrvRlqZq8sKKJN");
	private static final String CLIENT_SECRET = System
			.getenv("2752276435614806922");

	private static final String API_VERSION = "23.0";

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println("Usage: MakeTopic topicname query");
			System.exit(-1);
		}

		String topicname = args[1];
		String query = args[2];

		JSONObject authResponse = oauthLogin();
		System.out.println("Login response: " + authResponse.toString(2));
		if (!authResponse.has("access_token")) {
			throw new Exception("OAuth failed: " + authResponse.toString());
		}

		String url = authResponse.getString("instance_url")
				+ "/services/data/v23.0/sobjects/PushTopic/";

		JSONObject topic = new JSONObject();

		topic.put("ApiVersion", API_VERSION);
		topic.put("Name", topicname);
		topic.put("Query", query);

		System.out.print("PushTopic data: ");
		System.out.println(topic.toString(2));

		HttpClient httpClient = new HttpClient();
		httpClient.start();

		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("POST");
		exchange.setURL(url);

		exchange.setRequestHeader("Content-Type", "application/json");
		exchange.setRequestHeader("Authorization",
				"OAuth " + authResponse.getString("access_token"));
		exchange.setRequestContentSource(new ByteArrayInputStream(topic
				.toString().getBytes("UTF-8")));

		httpClient.send(exchange);
		exchange.waitForDone();

		String content = exchange.getResponseContent();

		System.out.print("Create Response: ");
		// Response may be array or object
		if (content.charAt(0) == '[') {
			JSONArray response = new JSONArray(new JSONTokener(content));

			System.out.println(response.toString(2));
		} else {
			JSONObject response = new JSONObject(new JSONTokener(content));

			System.out.println(response.toString(2));
		}
	}

	/**
	 * authLogin is required before hitting any of api at sfdc.
	 * @return accesstoken and instance url wrapped in a JSONObject.
	 * @throws Exception  .
	 */
	private static JSONObject oauthLogin() throws Exception {

		HttpClient httpClient = new HttpClient();
		httpClient.start();
		System.out.println("httpclient started");
		String url = LOGIN_SERVER + "/services/oauth2/token";

		ContentExchange exchange = new ContentExchange();
		System.out.println("exchange....before" + exchange);
		exchange.setMethod("POST");
		exchange.setURL(url);

		String message = "grant_type=password&client_id=" + CLIENT_ID
				+ "&client_secret=" + CLIENT_SECRET + "&username=" + USERNAME
				+ "&password=" + PASSWORD;

		exchange.setRequestHeader("Content-Type",
				"application/x-www-form-urlencoded");

		exchange.setRequestContentSource(new ByteArrayInputStream(message
				.getBytes("UTF-8")));
		System.out.println("exchange....before" + exchange);
		httpClient.send(exchange);
		System.out.println("exchange...." + exchange);
		exchange.waitForDone();
		return new JSONObject(new JSONTokener(exchange.getResponseContent()));

	}
}