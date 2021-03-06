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
package com.r573.enfili.ws.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseService {	
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(BaseService.class);
	
	/**
	 * Start the service. This is optional. Override where necessary.
	 * Framework should always call this when app is starting.
	 */
	public void start(){
	}
	/**
	 * Stop the service. This is optional. Override where necessary.
	 * Framework should always call this when app is stopping.
	 */
	public void stop(){
	}
}
