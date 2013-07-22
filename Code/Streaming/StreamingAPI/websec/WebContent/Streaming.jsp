<!--Page developed by Nidhi Neema  -->
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" import="org.cometd.bayeux.client.ClientSessionChannel
    ,org.cometd.bayeux.client.ClientSessionChannel.MessageListener
    ,org.cometd.bayeux.Message
    ,org.cometd.bayeux.Channel
    ,org.cometd.client.BayeuxClient
    " %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Streaming Process</title>


<script LANGUAGE="JavaScript" src="jsapi/jquery_1.5.2.min.js"></script>
<script src="jsapi/json2.js"></script>
<script src="jsapi/jquery-1.5.1.js"></script>
<script LANGUAGE="JavaScript" src="jsapi/Cometd.js"></script>
<script LANGUAGE="JavaScript" src="jsapi/jquery.cometd.js"></script>

<script type="text/javascript">
(function($){
$(document).ready(function() {
// Connect to the CometD endpoint
alert("jquery done..");
$.cometd.init({
url: window.location.protocol+'//'+window.location.hostname+'/cometd/24.0/',
requestHeaders: { Authorization: 'OAuth {!$Api.Session_ID}'}
});
// Subscribe to a topic. JSON-encoded update will be returned
// in the callback
$.cometd.subscribe('/topic/InvoiceStatementUpdates', function(message) {
$('#content').append('<p>Notification: ' +
'Channel: ' + JSON.stringify(message.channel) + '<br>' +
'Record name: ' + JSON.stringify(message.data.sobject.Name) +
'<br>' + 'ID: ' + JSON.stringify(message.data.sobject.Id) +
'<br>' + 'Event type: ' + JSON.stringify(message.data.event.type)+
'<br>' + 'Created: ' + JSON.stringify(message.data.event.createdDate)
+
'</p>');
});
});
});
</script>
</head>
<body>
Running streaming client example....


<div id="content">
<h1>Streaming API Test Page</h1>
<p>This is a demonstration page for Streaming API. Notifications from the
InvoiceStatementUpdates channel will appear here...</p>


</div>
 [CHANNEL:META_HANDSHAKE]:<%=request.getAttribute("META_HANDSHAKE")%> 
 <%-- <%if (request.getAttribute("Handshake_error") != null )%>
Error during HANDSHAKE:error <%=(String)request.getAttribute("Handshake_error")%> --%> <br>
[CHANNEL:META_CONNECT]:  <%=session.getAttribute("META_CONNECT")%> <br>
[CHANNEL:META_SUBSCRIBE]:  <%=session.getAttribute("META_SUBSCRIBE")%> <br>
Waiting for handshake... <br>
Subscribing for channel: <%=session.getAttribute("CHANNEL")%> <br>
Received Message: <%=session.getAttribute("MessageReceived")%><br>
</body>
</html>