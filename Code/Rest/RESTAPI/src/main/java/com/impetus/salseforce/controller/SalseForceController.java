/**
 * 
 */
package com.impetus.salseforce.controller;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author rukmanesh.bhawnani
 * 
 */

@Controller
public class SalseForceController {

	private static final String ACCESS_TOKEN = "ACCESS_TOKEN";
	private static final String INSTANCE_URL = "INSTANCE_URL";

	private String clientId = null;
	private String clientSecret = null;
	private String redirectUri = null;
	private String environment = null;
	private String authUrl = null;
	private String tokenUrl = null;

	private String instanceUrl = null;
	private String accessToken = null;

	@PostConstruct
	public void init() {

		clientId = "3MVG9Y6d_Btp4xp5WvdRmNPKGUwbuF7YQn46HL5qlh80xu0ddCR5ZMSYR6kQuYLiE0dn48tb_tOMod1E0KxjI";
		clientSecret = "6705804086869681386";
		redirectUri = "http://localhost:8080/mongoDB/oauth/_callback";
		environment = "https://login.salesforce.com";
		try {
			authUrl = environment
					+ "/services/oauth2/authorize?response_type=code&client_id="
					+ clientId + "&redirect_uri="
					+ URLEncoder.encode(redirectUri, "UTF-8");
		} catch (UnsupportedEncodingException e) {

			e.printStackTrace();
		}

		tokenUrl = environment + "/services/oauth2/token";

	}

	@RequestMapping(value = { "/oauth", "/oauth/*" }, method = RequestMethod.GET)
	public void oAuth(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		accessToken = (String) request.getSession().getAttribute(ACCESS_TOKEN);

		if (accessToken == null) {

			if (request.getRequestURI().endsWith("oauth")) {
				// we need to send the user to authorize
				response.sendRedirect(authUrl);
				return;
			} else {

				System.out.println("Auth successful - got callback");

				String code = request.getParameter("code");

				HttpClient httpclient = new HttpClient();

				PostMethod post = new PostMethod(tokenUrl);
				post.addParameter("code", code);
				post.addParameter("grant_type", "authorization_code");
				post.addParameter("client_id", clientId);
				post.addParameter("client_secret", clientSecret);
				post.addParameter("redirect_uri", redirectUri);

				try {
					httpclient.executeMethod(post);

					try {
						JSONObject authResponse = new JSONObject(
								new JSONTokener(new InputStreamReader(
										post.getResponseBodyAsStream())));
						System.out.println("Auth response: "
								+ authResponse.toString(2));

						accessToken = authResponse.getString("access_token");
						instanceUrl = authResponse.getString("instance_url");

						System.out.println("Got access token: " + accessToken);
					} catch (JSONException e) {
						e.printStackTrace();
						throw new ServletException(e);
					}
				} finally {
					post.releaseConnection();
				}
			}

			// Set a session attribute so that other servlets can get the access
			// token
			request.getSession().setAttribute(ACCESS_TOKEN, accessToken);

			// We also get the instance URL from the OAuth response, so set it
			// in the session too
			request.getSession().setAttribute(INSTANCE_URL, instanceUrl);
		}

		
//		return accessToken;
		// response.sendRedirect(request.getContextPath() + "/DemoREST");
	}

	@RequestMapping(value = { "/showAccounts" }, method = RequestMethod.GET)
	private @ResponseBody String showAccounts() throws ServletException, IOException {

		String responseJson = "";
		HttpClient httpclient = new HttpClient();

		GetMethod get = new GetMethod(instanceUrl
				+ "/services/data/v20.0/query.json");

		// set the token in the header
		get.setRequestHeader("Authorization", "OAuth " + accessToken);

		// set the SOQL as a query param
		NameValuePair[] params = new NameValuePair[1];

		params[0] = new NameValuePair("q",
				"SELECT Name, Id from Account LIMIT 100");
		get.setQueryString(params);

		try {
			httpclient.executeMethod(get);
			System.out.println("get.getStatusCode() ======= "+get.getStatusCode());
			if (get.getStatusCode() == HttpStatus.SC_OK) {
				try {
					responseJson = get.getResponseBodyAsString();
					System.out.println("responseJson ====== "+responseJson);
				} catch (Exception e) {
					e.printStackTrace();
					throw new ServletException(e);
				}
			}
		} finally {
			get.releaseConnection();
		}

		return responseJson;
	}
	
	
	@RequestMapping(value = { "/createAccounts" }, method = RequestMethod.GET)
	private @ResponseBody String createAccount(@RequestParam("name") String name) throws ServletException,
			IOException {
		String accountId = null;

		HttpClient httpclient = new HttpClient();

		JSONObject account = new JSONObject();

		try {
			account.put("Name", name);
		} catch (JSONException e) {
			e.printStackTrace();
			throw new ServletException(e);
		}

		System.out.println("instanceUrl == "+instanceUrl);
		
		PostMethod post = new PostMethod(instanceUrl
				+ "/services/data/v20.0/sobjects/Account/");

		post.setRequestHeader("Authorization", "OAuth " + accessToken);
		post.setRequestEntity(new StringRequestEntity(account.toString(),
				"application/json", null));

		try {
			httpclient.executeMethod(post);

			if (post.getStatusCode() == HttpStatus.SC_CREATED) {
				try {
					JSONObject response = new JSONObject(new JSONTokener(
							new InputStreamReader(
									post.getResponseBodyAsStream())));
					System.out.println("Create response: "
							+ response.toString(2));

					if (response.getBoolean("success")) {
						accountId = response.getString("id");
					}
				} catch (JSONException e) {
					e.printStackTrace();
					//throw new ServletException(e);
				}
			}
		} finally {
			post.releaseConnection();
		}

		return accountId;
	}
	
	@RequestMapping(value = { "/showAccount" }, method = RequestMethod.GET)
	private @ResponseBody String showAccount(@RequestParam("accountId") String accountId, HttpServletResponse res) throws ServletException,
			IOException {
		HttpClient httpclient = new HttpClient();
		GetMethod get = new GetMethod(instanceUrl
				+ "/services/data/v20.0/sobjects/Account/" + accountId);
		String response = "";
		// set the token in the header
		get.setRequestHeader("Authorization", "OAuth " + accessToken);
		
		try {
			httpclient.executeMethod(get);
			if (get.getStatusCode() == HttpStatus.SC_OK) {
				try {
					
					response = get.getResponseBodyAsString();
					
//					JSONObject response = new JSONObject(
//							new JSONTokener(new InputStreamReader(
//									get.getResponseBodyAsStream())));
//					System.out.println("Query response: "
//							+ response.toString(2));
//
//					writer.write("Account content\n\n");
//
//					Iterator iterator = response.keys();
//					while (iterator.hasNext()) {
//						String key = (String) iterator.next();
//						String value = response.getString(key);
//						writer.write(key + ":" + (value != null ? value : "") + "\n");
//					}
//
//					writer.write("\n");
				} catch (Exception e) {
					e.printStackTrace();
					throw new ServletException(e);
				}
			}
		} finally {
			get.releaseConnection();
		}
		
		res.setContentType("application/json");
		return response;
		
	}

	@RequestMapping(value = { "/updateAccount" }, method = RequestMethod.GET)
	private @ResponseBody String updateAccount(@RequestParam("accountId") String accountId, @RequestParam("newName") String newName, @RequestParam("city") String city) throws ServletException, IOException {
		HttpClient httpclient = new HttpClient();

		String response = "";
		JSONObject update = new JSONObject();

		try {
			update.put("Name", newName);
			update.put("BillingCity", city);
		} catch (JSONException e) {
			e.printStackTrace();
			throw new ServletException(e);
		}

		PostMethod patch = new PostMethod(instanceUrl+ "/services/data/v20.0/sobjects/Account/" + accountId) {
			@Override
			public String getName() {
				return "PATCH";
			}
		};

		patch.setRequestHeader("Authorization", "OAuth " + accessToken);
		patch.setRequestEntity(new StringRequestEntity(update.toString(),
				"application/json", null));

		try {
			httpclient.executeMethod(patch);
			response = "HTTP status " + patch.getStatusCode() + " updating account " + accountId;
			
		} finally {
			patch.releaseConnection();
		}
		return response;
	}
	
	@RequestMapping(value = { "/googleAccountAccessToken" }, method = RequestMethod.GET)
	private  @ResponseBody String getTest() throws IOException {
		System.out.println("---==================== stArt DemoREST.getTest()");
		HttpClient httpclient = new HttpClient();

		PostMethod post = new PostMethod(instanceUrl+ "/services/apexrest/test");

		post.setRequestHeader("Authorization", "OAuth " + accessToken);
		String res = "";
		try {
			httpclient.executeMethod(post);
			
			if (post.getStatusCode() == HttpStatus.SC_OK) {
				
				res = post.getResponseBodyAsString();
				System.out.println("post.getResponseBodyAsString() -------------- "+res);
				
			}
			
			
					
		} finally {
			post.releaseConnection();
		}
		System.out.println("---==================== end DemoREST.getTest()");
		
		return res;
	}

	
	@RequestMapping(value = { "/deleteAccount" }, method = RequestMethod.GET)
	private  @ResponseBody String deleteAccount(@RequestParam("accountId") String accountId) throws IOException {
		HttpClient httpclient = new HttpClient();

		String response = "";
			
		DeleteMethod delete = new DeleteMethod(instanceUrl
				+ "/services/data/v20.0/sobjects/Account/" + accountId);

		delete.setRequestHeader("Authorization", "OAuth " + accessToken);

		try {
			httpclient.executeMethod(delete);
			response = "HTTP status " + delete.getStatusCode() + " deleting account " + accountId;
		} finally {
			delete.releaseConnection();
		}
		
		return response;
	}

}
