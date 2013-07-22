<!--Page developed by Nidhi Neema  -->
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" import="java.lang.String,
    org.cometd.bayeux.Channel,
    org.cometd.bayeux.Message,
    org.cometd.bayeux.client.ClientSessionChannel,
    org.cometd.bayeux.client.ClientSessionChannel.MessageListener,
    org.cometd.client.BayeuxClient,
    org.cometd.client.transport.ClientTransport,
    org.cometd.client.transport.LongPollingTransport,
    org.eclipse.jetty.client.ContentExchange,
    org.eclipse.jetty.client.HttpClient,
    java.net.MalformedURLException,
    java.net.URL,
    java.util.HashMap,
    java.util.Map,
    javax.servlet.http.HttpServletRequest,
    javax.servlet.http.HttpServletResponse,
    java.io.PrintWriter"%>
    
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Streaming Connection</title>
</head>
<body>

<%
 final String LOGIN_ENDPOINT = "https://www.salesforce.com";
 final String USER_NAME = "karishma@impetus.com";
 final String PASSWORD = "welcome123";
 final boolean VERSION_22 = false;
 final boolean USE_COOKIES = VERSION_22;
 final String CHANNEL = "/topic/InvoiceStatementUpdates";
 final String STREAMING_ENDPOINT_URI = VERSION_22 ? "/cometd"
		: "/cometd/28.0";
 final int CONNECTION_TIMEOUT = 60 * 1000; // milliseconds
 final int READ_TIMEOUT = 150 * 1000; // milliseconds %>
Running streaming client example....
<% final String accesstoken=(String)request.getAttribute("ACCESS_TOKEN");
final String url=(String)request.getAttribute("ACCESS_TOKEN");
final PrintWriter finalOut = response.getWriter();
//making client

final BayeuxClient client ;
final HttpClient httpClient = new HttpClient();
httpClient.setConnectTimeout(CONNECTION_TIMEOUT);
httpClient.setTimeout(READ_TIMEOUT);
httpClient.start();
//String[] pair = SoapLoginUtil.login(httpClient, USER_NAME, PASSWORD);
//if (pair == null) {
//	System.exit(1);
//}
//assert pair.length == 2;

final String sessionid = accesstoken;
final String endpoint = url;  %>
Login successful! Endpoint : <%= endpoint%>   Sessionid :<%=sessionid%>
<% final Map<String, Object> options = new HashMap<String, Object>();
options.put(ClientTransport.TIMEOUT_OPTION, READ_TIMEOUT);
final LongPollingTransport transport = new LongPollingTransport(options,
		httpClient) {
	@Override
	protected void customize(final ContentExchange exchange) {
		super.customize(exchange);
		exchange.addRequestHeader("Authorization", "OAuth " + sessionid);
	}
};
final String urlendpoint;
final String myendpoint=endpoint + STREAMING_ENDPOINT_URI;
//try{
 urlendpoint=new URL(myendpoint).toExternalForm();
//}catch(MalformedURLException e){}
 client = new BayeuxClient(urlendpoint, transport);
 
if (USE_COOKIES)
	{
	client.setCookie("com.salesforce.LocaleInfo", "us", 24 * 60 * 60 * 1000);
	client.setCookie("login", USER_NAME, 24 * 60 * 60 * 1000);
	client.setCookie("sid", sessionid, 24 * 60 * 60 * 1000);
	client.setCookie("language", "en_US", 24 * 60 * 60 * 1000);
	}


client.getChannel(Channel.META_HANDSHAKE).addListener(
		new ClientSessionChannel.MessageListener() {
			final public void onMessage(final ClientSessionChannel channel,
					final Message message) { %>
				 [CHANNEL:META_HANDSHAKE]: message
			<% final boolean success = message.isSuccessful();
				if (!success) {
					final String error = (String) message.get("error");
					if (error != null) { %>
		
						
					<% 	System.exit(1);
					}
				 final Exception exception = (Exception) message
							.get("exception");
					if (exception != null) {

						// Example: Java Client Step 4: Add the Source
						// Code
						System.out.println("Exception during HANDSHAKE: ");
						exception.printStackTrace();
						System.out.println("Exiting...");
						System.exit(1);
					}
				}
			}
		});
client.getChannel(Channel.META_CONNECT).addListener(
		new ClientSessionChannel.MessageListener() {
		final public void onMessage(final ClientSessionChannel channel,
				 final Message message) { %>
			 [CHANNEL:META_CONNECT]: message
				<%final boolean success = message.isSuccessful();
				if (!success) {
					final String error = (String) message.get("error");
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
		final public void onMessage(final ClientSessionChannel channel,
				final Message message) { %>
			 [CHANNEL:META_SUBSCRIBE]:message 
			<% final boolean success = message.isSuccessful();
				if (!success) {
				final String error = (String) message.get("error");
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
final boolean handshaken = client.waitFor(10 * 1000,
		BayeuxClient.State.CONNECTED);
if (!handshaken) {
	System.out.println("Failed to handshake: " + client);
	System.exit(1);
}
%>
 Subscribing for channel:CHANNEL 
<% client.getChannel(CHANNEL).subscribe(new MessageListener() {
	final public void onMessage(final ClientSessionChannel channel,final Message message) {
		%>
		Salesforce Changed Received Message: message
		<% 
		// Example: Java Client Step 4: Add the Source Code
	}
});
%>
Waiting for streamed data from your organization ...

<% request.setAttribute("Session_ID", accesstoken);
%>


 Error during HANDSHAKE:error <%=(String)request.getAttribute("Handshake_error")%>
</body>
</html>