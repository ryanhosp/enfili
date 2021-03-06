/*
 * Enfili
 * Project hosted at https://github.com/ryanhosp/enfili/
 * Copyright 2013 Ho Siaw Ping Ryan
 *    
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.r573.enfili.ws.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.JavaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.r573.enfili.common.doc.json.JsonHelper;
import com.r573.enfili.common.exception.WsRuntimeException;
import com.r573.enfili.common.text.StringHelper;
import com.r573.enfili.ws.data.WsError;
import com.r573.enfili.ws.data.WsResponse;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

public class WsRestClient {

	private final Logger log = LoggerFactory.getLogger(WsRestClient.class);
	
	private Client jerseyClient = new Client();
	private String baseUrl;
	private Map<String, Cookie> cookies;

	public WsRestClient(String baseUrl) {
		ClientConfig clientConfig = new DefaultClientConfig();
		clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
		jerseyClient = Client.create(clientConfig);
		cookies = new HashMap<String, Cookie>();
		this.baseUrl = baseUrl;
	}

	private WebResource.Builder getResource(String path, Map<String,String> queryParams) {
		WebResource webResource = jerseyClient.resource(baseUrl + path);
		for(String key : queryParams.keySet()){
			webResource = webResource.queryParam(key, queryParams.get(key));
		}

		WebResource.Builder builder = webResource.type(MediaType.APPLICATION_JSON_TYPE).accept(MediaType.APPLICATION_JSON_TYPE);
		builder = addCookies(builder);
		return builder;
	}

	public <T> WsResponse<T> get(String path, Class<T> clazz, Map<String,String> queryParams) throws WsRestException {
		ClientResponse response = getResource(path,queryParams).get(ClientResponse.class);
		return processResponse(response, clazz);
	}
	public <T> WsResponse<T> get(String path, Class<T> clazz) throws WsRestException {
		return get(path,clazz,new HashMap<String, String>());
	}

	public <T> WsResponse<T> post(String path, Object postObj, Class<T> clazz) throws WsRestException {
		String postObjJson = JsonHelper.toJson(postObj);
		ClientResponse response = getResource(path,new HashMap<String, String>()).post(ClientResponse.class, postObjJson);
		log.debug("status=" + response.getStatus());
		return processResponse(response, clazz);
	}

	public <T> WsResponse<T> put(String path, Object postObj, Class<T> clazz) throws WsRestException {
		String postObjJson = JsonHelper.toJson(postObj);
		ClientResponse response = getResource(path,new HashMap<String, String>()).put(ClientResponse.class, postObjJson);
		return processResponse(response, clazz);
	}

	public <T> WsResponse<T> delete(String path, Class<T> clazz) throws WsRestException {
		ClientResponse response = getResource(path,new HashMap<String, String>()).delete(ClientResponse.class);
		return processResponse(response, clazz);
	}
	
	private <T> WsResponse<T> processResponse(ClientResponse response, Class<T> clazz) throws WsRestException {

		ObjectMapper objectMapper = new ObjectMapper();
		JavaType javaType = null;
		
		if (clazz != null) {
			javaType = objectMapper.getTypeFactory().constructParametricType(WsResponse.class, clazz);
		} else {
			javaType = objectMapper.getTypeFactory().constructType(WsResponse.class);
		}
		
		List<NewCookie> newCookies = response.getCookies();
		for (NewCookie newCookie : newCookies) {
			cookies.put(newCookie.getName(), newCookie);
		}

		String wsResponseJson = response.getEntity(String.class);
		log.debug(wsResponseJson);
		
		try {
			JsonFactory factory = objectMapper.getJsonFactory();
			JsonParser jp = factory.createJsonParser(wsResponseJson);
			JsonNode node = objectMapper.readTree(jp);
			JsonNode statusNode = node.get("statusCode");
			
			if(statusNode.getTextValue().equals("OK")){
				WsResponse<T> wsResponse = JsonHelper.fromJson(wsResponseJson, javaType);
				return wsResponse;				
			}
			else{
				JavaType errorType = objectMapper.getTypeFactory().constructParametricType(WsResponse.class, WsError.class);
				WsResponse<WsError> error = JsonHelper.fromJson(wsResponseJson, errorType);
				throw new WsRestException(error.getResponseData());			
			}
		} catch (JsonParseException e) {
			throw new WsRuntimeException(null,e.getClass().getName());
		} catch (IOException e) {
			throw new WsRuntimeException(null,e.getClass().getName());
		}
	}

	public File postAndDownloadFile(String path, Object postObj, File downloadDir, ArrayList<String> acceptTypes) {
		String postObjJson = JsonHelper.toJson(postObj);
		log.debug("POST JSON " + postObjJson);
		
		WebResource.Builder builder = jerseyClient.resource(baseUrl + path).type(MediaType.APPLICATION_JSON_TYPE);
		builder = addAcceptTypes(builder, acceptTypes);
		builder = addCookies(builder);
		
		ClientResponse response = getResource(path,new HashMap<String, String>()).post(ClientResponse.class, postObjJson);
		log.debug("status=" + response.getStatus());
		String receivedType = response.getHeaders().getFirst("Content-Type");
		if(receivedType == null){
			log.error("Null Content-Type returned from the server");
			return null;
		}
		else if (receivedType.equalsIgnoreCase("application/json")){
			log.error("Failed to download file with error response " + response.getEntity(String.class));
			return null;
		}
		else {
			String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
			
			String fileName = null;
			if((contentDisposition != null) && (contentDisposition.startsWith("attachment"))){
				String[] split1 = contentDisposition.split(";");
				String[] split2 = split1[1].split("=");
				fileName = split2[1].trim();
				if(fileName.startsWith("\"")) {
					fileName = fileName.substring(1, fileName.length()-1);
				}
			}
			else {
				fileName = UUID.randomUUID().toString();
			}
			File fileToSave = new File(FilenameUtils.concat(downloadDir.getPath(), fileName));
			InputStream inputStream = response.getEntityInputStream();
						
			try {
				FileUtils.copyInputStreamToFile(inputStream, fileToSave);
				return fileToSave;
			}
			catch(IOException e) {
				log.error(StringHelper.stackTraceToString(e));
				return null;
			}			
		}
	}
	
	private WebResource.Builder addCookies(WebResource.Builder builder) {
		Set<String> cookieNames = cookies.keySet();
		for (String cookieName : cookieNames) {
			builder = builder.cookie(cookies.get(cookieName));
		}
		return builder;		
	}
	private WebResource.Builder addAcceptTypes(WebResource.Builder builder, ArrayList<String> acceptTypes) {
		for (String acceptType : acceptTypes) {
			builder = builder.accept(acceptType);
		}
		return builder;		
	}
}
