package com.impetus.servletSales;

import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.client.ClientSessionChannel.MessageListener;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;

//Example: Java Client Step 1: Create an Object
import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;

import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author nidhi.neema
 * 
 *         This example demonstrates how a streaming client works against the
 *         Salesforce Streaming API.It uses Bayeux protocol and CometD. A
 *         BayeuxClient handshakes with a Bayeux server and then subscribes
 *         ClientSessionChannel.MessageListener to channels in order to receive
 *         messages, and may also publish messages to the Bayeux server.
 * 
 *         BayeuxClient relies on pluggable transports for communication with
 *         the Bayeux server, and the most common transport is
 *         LongPollingTransport, which uses HTTP to transport Bayeux messages
 *         and it is based on Jetty's HTTP client.
 * 
 *         When the communication with the server is finished, the BayeuxClient
 *         can be disconnected from the Bayeux server.
 */
public class StreamingClientExample {
	// This URL is used only for logging in. The LoginResult
	// returns a serverUrl which is then used for constructing
	// the streaming URL. The serverUrl points to the endpoint
	// where your organization is hosted.
	static final String LOGIN_ENDPOINT = "https://www.salesforce.com";
	private static final String USER_NAME = "karishma@impetus.com";
	// private static final String PASSWORD =
	// "welcome123vkXsW9PJWPkOFprnpLbjCaXK";
	private static final String PASSWORD = "welcome123";
	// NOTE: Putting passwords in code is not a good practice and not
	// recommended.
	// Set this to true only when using this client
	// against the Summer'11 release (API version=22.0).
	private static final boolean VERSION_22 = false;
	private static final boolean USE_COOKIES = VERSION_22;
	// The channel to subscribe to. Same as the name of the PushTopic.
	// Be sure to create this topic before running this sample.
	private static final String CHANNEL = "/topic/InvoiceStatementUpdates";
	private static final String STREAMING_ENDPOINT_URI = VERSION_22 ? "/cometd"
			: "/cometd/28.0";
	// The long poll duration.
	private static final int CONNECTION_TIMEOUT = 60 * 1000; // milliseconds
	private static final int READ_TIMEOUT = 150 * 1000; // milliseconds

	// public static void main(String[] args) throws Exception { //for
	// standalone streaming application
	public static void streaming(String accesstoken, String url,
			final HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		System.out.println("Running streaming client example....");
		PrintWriter out = response.getWriter();
		final HttpSession session = request.getSession();
		final BayeuxClient client = makeClient(accesstoken, url);

		client.getChannel(Channel.META_HANDSHAKE).addListener(
				new ClientSessionChannel.MessageListener() {
					public void onMessage(ClientSessionChannel channel,
							Message message) {
						System.out.println("[CHANNEL:META_HANDSHAKE]: "
								+ message);
						request.setAttribute("META_HANDSHAKE", message);
						boolean success = message.isSuccessful();
						if (!success) {
							String error = (String) message.get("error");
							if (error != null) {
								System.out.println("Error during HANDSHAKE: "
										+ error);
								System.out.println("Exiting...");
								System.exit(1);
							}
							Exception exception = (Exception) message
									.get("exception");
							if (exception != null) {

								// Example: Java Client Step 4: Add the Source
								// Code
								System.out
										.println("Exception during HANDSHAKE: ");
								exception.printStackTrace();
								System.out.println("Exiting...");
								System.exit(1);
							}
						}
					}
				});

		session.setAttribute("META_HANDSHAKE",
				request.getAttribute("META_HANDSHAKE"));
		client.getChannel(Channel.META_CONNECT).addListener(
				new ClientSessionChannel.MessageListener() {
					public void onMessage(ClientSessionChannel channel,
							Message message) {
						System.out
								.println("[CHANNEL:META_CONNECT]: " + message);
						session.setAttribute("META_CONNECT", message);
						boolean success = message.isSuccessful();
						if (!success) {
							String error = (String) message.get("error");
							if (error != null) {
								System.out.println("Error during CONNECT: "
										+ error);
								System.out.println("Exiting...");
								System.exit(1);
							}
						}
					}
				});

		client.getChannel(Channel.META_SUBSCRIBE).addListener(
				new ClientSessionChannel.MessageListener() {
					public void onMessage(ClientSessionChannel channel,
							Message message) {
						System.out.println("[CHANNEL:META_SUBSCRIBE]: "
								+ message);
						session.setAttribute("META_SUBSCRIBE", message);
						boolean success = message.isSuccessful();
						if (!success) {
							String error = (String) message.get("error");
							if (error != null) {
								System.out.println("Error during SUBSCRIBE: "
										+ error);
								System.out.println("Exiting...");
								System.exit(1);
							}
						}
					}
				});
		client.handshake();
		System.out.println("Waiting for handshake");
		boolean handshaken = client.waitFor(10 * 1000,
				BayeuxClient.State.CONNECTED);
		if (!handshaken) {
			System.out.println("Failed to handshake: " + client);
			System.exit(1);
		}
		System.out.println("Subscribing for channel: " + CHANNEL);
		session.setAttribute("CHANNEL", CHANNEL);
		client.getChannel(CHANNEL).subscribe(new MessageListener() {
			public void onMessage(ClientSessionChannel channel, Message message) {
				System.out.println("Received Message: " + message);
				session.setAttribute("MessageReceived", message);
				// Example: Java Client Step 4: Add the Source Code
			}
		});
		System.out
				.println("Waiting for streamed data from your organization ...");

		request.setAttribute("Session_ID", accesstoken);

		RequestDispatcher dispatcher = request
				.getRequestDispatcher("Streaming.jsp");
		dispatcher.forward(request, response);

		// If we dont want to redirect to any jsp page , remove comment of the
		// next block.
		/*
		 * while (true) { // This infinite loop is for demo only, // to receive
		 * streamed events on the // specified topic from your organization. }
		 */

	}

	/**
	 * Creating a BayeuxClient that can receive/publish messages from/to a
	 * Bayeux server[SFDC]
	 * 
	 * @param accesstoken
	 *            needed for creating LongPollingTransport[Cometd for long
	 *            polling]
	 * @param url
	 *            instance url for creating LongPollingTransport. For sdfc, it
	 *            is the instance of user workspaces.
	 * @return BayeuxClient for subscribing to channel.
	 * @throws Exception
	 */
	private static BayeuxClient makeClient(String accesstoken, String url)
			throws Exception {
		HttpClient httpClient = new HttpClient();
		httpClient.setConnectTimeout(CONNECTION_TIMEOUT);
		httpClient.setTimeout(READ_TIMEOUT);
		httpClient.start();
		// String[] pair = SoapLoginUtil.login(httpClient, USER_NAME, PASSWORD);
		// if (pair == null) {
		// System.exit(1);
		// }
		// assert pair.length == 2;

		final String sessionid = accesstoken;
		String endpoint = url;
		System.out.println("Login successful!\nEndpoint: " + endpoint
				+ "\nSessionid=" + sessionid);
		Map<String, Object> options = new HashMap<String, Object>();
		options.put(ClientTransport.TIMEOUT_OPTION, READ_TIMEOUT);
		LongPollingTransport transport = new LongPollingTransport(options,
				httpClient) {
			@Override
			protected void customize(ContentExchange exchange) {
				super.customize(exchange);
				exchange.addRequestHeader("Authorization", "OAuth " + sessionid);
			}
		};
		String myurl = salesforceStreamingEndpoint(endpoint);
		BayeuxClient client = new BayeuxClient(
				salesforceStreamingEndpoint(endpoint), transport);
		if (USE_COOKIES)
			establishCookies(client, USER_NAME, sessionid);
		return client;
	}

	/**
	 * @param endpoint
	 *            salesforce instance url that we get after OAUTH
	 *            authentication.
	 * @return well formed URL required to create a BayeuxClient.
	 * @throws MalformedURLException
	 */
	private static String salesforceStreamingEndpoint(String endpoint)
			throws MalformedURLException {
		return new URL(endpoint + STREAMING_ENDPOINT_URI).toExternalForm();
	}

	/**
	 * @param client
	 *            reference to a BayeuxClient.
	 * @param user
	 *            logged in ID of sfdc user.
	 * @param sid
	 *            session id that we get after OAUTH authentication.
	 */
	private static void establishCookies(BayeuxClient client, String user,
			String sid) {
		client.setCookie("com.salesforce.LocaleInfo", "us", 24 * 60 * 60 * 1000);
		client.setCookie("login", user, 24 * 60 * 60 * 1000);
		client.setCookie("sid", sid, 24 * 60 * 60 * 1000);
		client.setCookie("language", "en_US", 24 * 60 * 60 * 1000);
	}
}
