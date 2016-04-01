/*******************************************************************************
 * Copyright 2013-2016 Aron Heinecke
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package me.Aron.Heinecke.fbot.lib;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLPeerUnverifiedException;

import me.Aron.Heinecke.fbot.fbot;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

public class Socket {
	private HttpClient client;
	private HttpClientBuilder hcbuilder;
	private HttpClientConnectionManager connManager = new BasicHttpClientConnectionManager();
	
	/***
	 * No content compression, fixing download problematics,
	 * no automatic retries, fixing undocumented loops, causing
	 * hangups
	 */
	public Socket() {
		if(fbot.isDebug()) fbot.getLogger().debug("socket", "initializing");
		hcbuilder = HttpClientBuilder.create();
		hcbuilder.setConnectionManager(connManager);
		hcbuilder.disableContentCompression();
		hcbuilder.disableAutomaticRetries();
		client = hcbuilder.build();
	}

	/***
	 * Reads and extracts the required tokens to login into fronter
	 * @return site content, tokens are stored in the DB
	 */
	public synchronized String getReqID() throws ClientProtocolException, IOException, IllegalStateException {

		String url = "https://fronter.com/giessen/index.phtml";
		//create client & request
		HttpGet request = new HttpGet(url);

		// add request header
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		request.addHeader("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
		request.addHeader("Content-Type", "application/x-www-form-urlencoded");
		request.addHeader("Cache-Control", "max-age=0");
		request.addHeader("Connection", "keep-alive");
		request.addHeader("DNT", "1");
		request.addHeader("Host", "fronter.com");
		request.addHeader("Referer", "https://fronter.com/giessen/index.phtml");
		request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/35.0");

		//Create context which stores the cookies
		HttpClientContext context = HttpClientContext.create();
		HttpResponse response = client.execute(request, context);
		
		if(fbot.isDebug()){
			fbot.getLogger().debug("socket", "Sending GET request to URL: " + url);
			fbot.getLogger().debug("socket", "Response code: " + response.getStatusLine().getStatusCode());
			fbot.getLogger().log("debug","socket", context.getCookieStore().getCookies());
		}
		//create & feed buffer
		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		
		StringBuffer result = new StringBuffer();
		String line = "";
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		rd.close();


		// extract the information out of the string
		String output = result.toString();
		String reqtoken = output;
		reqtoken = reqtoken.substring(reqtoken.indexOf("fronter_request_token\" value=") + 30);
		reqtoken = reqtoken.substring(0, reqtoken.indexOf("\" />"));
		
		String SSOCOM = output;
		SSOCOM = SSOCOM.substring(SSOCOM.indexOf("SSO_COMMAND_SECHASH\" value=\"") + 28);
		SSOCOM = SSOCOM.substring(0, SSOCOM.indexOf("\">"));
		
		if(fbot.isDebug()){
			fbot.getLogger().debug("socket", "SSOCOM-Token= " + SSOCOM);
			fbot.getLogger().debug("socket", "Req-Token= " + reqtoken);
		}
		fbot.getDB().setReqtoken(reqtoken);
		fbot.getDB().setSSOCOM(SSOCOM);

		return output;
	}

	/***
	 * Performs the login into fronter based on the DB credentials
	 * Requires the tokens form getReqID
	 * @return site content
	 */
	public synchronized String login() throws ClientProtocolException, IOException {
		String url = "https://fronter.com/giessen/index.phtml";
		//create client & post
		HttpPost post = new HttpPost(url);

		// add header
		post.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		post.setHeader("Accept-Encoding", "gzip, deflate");
		post.setHeader("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
		post.setHeader("Connection", "keep-alive");
		post.setHeader("DNT", "1");
		post.setHeader("Host", "fronter.com");
		post.setHeader("Referer", "https://fronter.com/giessen/index.phtml");
		post.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/35.0");

		// set login parameters & fake normal login data
		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair("SSO_COMMAND", ""));
		urlParameters.add(new BasicNameValuePair("SSO_COMMAND_SECHASH", fbot.getDB().getSSOCOM()));
		urlParameters.add(new BasicNameValuePair("USER_INITIAL_SCREEN_HEIGH...", "1080"));
		urlParameters.add(new BasicNameValuePair("USER_INITIAL_SCREEN_WIDTH", "1920"));
		urlParameters.add(new BasicNameValuePair("USER_INITIAL_WINDOW_HEIGH...", "914"));
		urlParameters.add(new BasicNameValuePair("USER_INITIAL_WINDOW_WIDTH", "1920"));
		urlParameters.add(new BasicNameValuePair("USER_SCREEN_SIZE", ""));
		urlParameters.add(new BasicNameValuePair("chp", ""));
		urlParameters.add(new BasicNameValuePair("fronter_request_token", fbot.getDB().getReqtoken()));
		urlParameters.add(new BasicNameValuePair("mainurl", "main.phtml"));
		urlParameters.add(new BasicNameValuePair("newlang", "de"));
		urlParameters.add(new BasicNameValuePair("password", fbot.getDB().getPass()));
		urlParameters.add(new BasicNameValuePair("saveid", "-1"));
		urlParameters.add(new BasicNameValuePair("username", fbot.getDB().getUser()));

		//create gzip encoder
		UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(urlParameters);
		urlEncodedFormEntity.setContentEncoding(new BasicHeader(HTTP.CONTENT_ENCODING, "UTF_8"));
		post.setEntity(urlEncodedFormEntity);

		//Create own context which stores the cookies
		HttpClientContext context = HttpClientContext.create();
		
		HttpResponse response = client.execute(post, context);
		
		if(fbot.isDebug()){
			fbot.getLogger().debug("socket", "Sending POST request to URL: " + url);
			fbot.getLogger().debug("socket", "Response code: " + response.getStatusLine().getStatusCode());
			fbot.getLogger().log("debug","socket", context.getCookieStore().getCookies());
		}

		// input-stream with gzip-accept
		InputStream input = response.getEntity().getContent();
		InputStreamReader isr = new InputStreamReader(input);
		BufferedReader rd = new BufferedReader(isr);

		StringBuffer result = new StringBuffer();
		String line = "";
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		input.close();
		isr.close();
		rd.close();

		fbot.getDB().setCookieStore(context.getCookieStore());

		return result.toString();

	}

	/***
	 * Downloads a specified file via http
	 * @param url request url
	 * @param savFile path to safe the file at
	 * @throws ClientProtocolException, IOException, SSLPeerUnverifiedException, FileNotFoundException 
	 */
	public synchronized String downloadFile(String url, String savFile) throws ClientProtocolException, IOException, SSLPeerUnverifiedException, ClientProtocolException, IOException, SSLPeerUnverifiedException, FileNotFoundException {
//		String url = "https://fronter.com/giessen/links/link.phtml?idesc=1&iid=12841";
		
		//Create context and set custom cookies store
		HttpClientContext context = HttpClientContext.create();
		context.setCookieStore(fbot.getDB().getCookieStore());

		HttpGet request = new HttpGet(url);

		// add request header
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		request.addHeader("Accept-Encoding", "binary");
		request.addHeader("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
		request.addHeader("Connection", "keep-alive");
		request.addHeader("DNT", "1");
		request.addHeader("Host", "fronter.com");
		request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/35.0");
		//execute request with local context (session-cookie)
		HttpResponse response = client.execute(request, context);

		if(fbot.isDebug()){
			fbot.getLogger().debug("socket", "Sending POST request to URL: " + url);
			fbot.getLogger().debug("socket", "Response code: " + response.getStatusLine().getStatusCode());
			fbot.getLogger().log("debug","socket", context.getCookieStore().getCookies());
		}
		
		//save response to file
		HttpEntity entity = response.getEntity();
		if ( entity != null && response.getStatusLine().getStatusCode() == 200 ) {
			InputStream inputStream = entity.getContent();

			OutputStream os = new FileOutputStream(savFile);

			byte[] buffer = new byte[1024];
			int bytesRead;
			//read from is to buffer
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			inputStream.close();
			//flush OutputStream to write any buffered data to file
			os.flush();
			os.close();
			inputStream.close();

			return savFile;
		} else {
			fbot.getLogger().severe("socket", "File download, server response: "+response.getStatusLine().getStatusCode());
			request.abort();
			request.releaseConnection();
			throw new FileNotFoundException("Not found!");
		}
	}

	/***
	 * Performs a logout from fronter based on the DB credentials
	 * @param site content
	 */
	public synchronized String logout() throws ClientProtocolException, IOException {
		String url = "https://fronter.com/giessen/index.phtml?logout=1";

		//create own context which custom cookie store
		HttpClientContext context = HttpClientContext.create();
		context.setCookieStore(fbot.getDB().getCookieStore());
		

		//create client & request
		HttpGet request = new HttpGet(url);

		// add request header
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		request.addHeader("Accept-Encoding", "gzip, deflate");
		request.addHeader("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
		request.addHeader("Content-Type", "application/x-www-form-urlencoded");
		request.addHeader("Connection", "close");
		request.addHeader("DNT", "1");
		request.addHeader("Host", "fronter.com");
		request.addHeader("Referer", "https://fronter.com/giessen/personalframe.phtml");
		request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/35.0");

		//execute request with local context
		HttpResponse response = client.execute(request, context);

		if(fbot.isDebug()){
			fbot.getLogger().debug("socket", "Sending GET request to URL: " + url);
			fbot.getLogger().debug("socket", "Response code: " + response.getStatusLine().getStatusCode());
		}

		//get gzip
		InputStream input = response.getEntity().getContent();
		GZIPInputStream gzip = new GZIPInputStream(input);
		InputStreamReader isr = new InputStreamReader(gzip);
		BufferedReader rd = new BufferedReader(isr);

		//write buffer
		StringBuffer result = new StringBuffer();
		String line = "";
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		input.close();
		gzip.close();
		isr.close();
		rd.close();
		
		return result.toString();
	}

	public String test() throws ClientProtocolException, IOException {
		String url = "https://fronter.com/giessen";

		HttpGet request = new HttpGet(url);

		// add request header
		request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/35.0");

		HttpResponse response = client.execute(request);

		System.out.println("Sending 'GET' request to URL : " + url);//DEBUG
		System.out.println("Response Code: " + response.getStatusLine().getStatusCode());//DEBUG

		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

		StringBuffer result = new StringBuffer();
		String line = "";
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}

		return result.toString();

	}
	
	/***
	 * Closes the socket, this will effectively end this class
	 */
	public void closeSocket(){
		connManager.shutdown();
	}
	
}
