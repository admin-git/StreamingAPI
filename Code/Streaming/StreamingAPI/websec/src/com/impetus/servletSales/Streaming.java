package com.impetus.servletSales;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * @author nidhi.neema Class to start the streaming Process. And makeTopic
 *         method to create PushTopic if it is not exsits. A PushTopic
 *         represents a query that is the basis for notifying listeners of
 *         changes to records in SFDC.
 * 
 *         The Force.com Streaming API lets you expose a near real-time stream
 *         of data from the Force.com platform in a secure and scalable way.
 *         Administrators can create topics, to which applications can
 *         subscribe, receiving asynchronous notifications of changes to data in
 *         Force.com, via the Bayeux protocol.
 */
@WebServlet(urlPatterns = { "/Streaming" })
public class Streaming extends HttpServlet {
	private static final String ACCESS_TOKEN = "ACCESS_TOKEN";
	private static final String INSTANCE_URL = "INSTANCE_URL";
	private static final String API_VERSION = "23.0";

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		PrintWriter writer = response.getWriter();

		String accessToken = (String) request.getSession().getAttribute(
				ACCESS_TOKEN);

		String instanceUrl = (String) request.getSession().getAttribute(
				INSTANCE_URL);

		if (accessToken == null) {
			writer.write("Error - no access token");
			return;
		}

		writer.write("We have an access token: " + accessToken + "\n"
				+ "Using instance " + instanceUrl + "\n\n");

		try {
			// makeTopic(accessToken,instanceUrl);
			StreamingClientExample.streaming(accessToken, instanceUrl, request,
					response); // Starting here streaming process

			// directly redirecting to streamMessages.jsp
			// response.sendRedirect("StreamMessages.jsp");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * makeTopic method to create Push topic via code . We could also create
	 * PushTopic at SFDC at its developer console . We need to create a push
	 * topic if it is already not exist.
	 * 
	 * @param accessToken
	 *            this is appended as request header of Content Exchange.
	 * @param instanceUrl
	 *            this is appended as request header of Content Exchange.
	 * @throws Exception
	 */
	private void makeTopic(String accessToken, String instanceUrl)
			throws Exception {

		String url = instanceUrl + "/services/data/v23.0/sobjects/PushTopic/";

		JSONObject topic = new JSONObject();

		// specify the api version used.
		topic.put("ApiVersion", API_VERSION);

		// specify the name of the topic
		topic.put("Name", "InvoiceStatementUpdates");

		// specify the Query . This is most important field as Event
		// Notifications are generated for updates that match the query.
		topic.put("Query",
				"select Id,details__c from Invoice_Statement_Demo__c");

		// specify NotifyForOperations as "All" if we want notifications on
		// create , update and delete data operations.
		topic.put("NotifyForOperations", "All");

		// specify "referenced" to NotifyForFields field so that now Streaming
		// API will use fields in both the SELECT clause and the WHERE clause to
		// generate a notification.
		topic.put("NotifyForFields", "Referenced");

		System.out.print("PushTopic data: ");
		System.out.println(topic.toString(2));

		HttpClient httpClient = new HttpClient();
		httpClient.start();

		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("POST");
		exchange.setURL(url);

		exchange.setRequestHeader("Content-Type", "application/json");
		exchange.setRequestHeader("Authorization", "OAuth " + accessToken);
		exchange.setRequestContentSource(new ByteArrayInputStream(topic
				.toString().getBytes("UTF-8")));

		httpClient.send(exchange);
		exchange.waitForDone();

		String content = exchange.getResponseContent();

		System.out.print("**********Creating Response: ");
		// Response may be array or object
		if (content.charAt(0) == '[') {
			JSONArray response = new JSONArray(new JSONTokener(content));

			System.out.println(response.toString(2));
		} else {
			JSONObject response = new JSONObject(new JSONTokener(content));

			System.out.println(response.toString(2));
		}

	}

}
