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

import java.util.Collection;
import java.util.List;

import com.r573.enfili.common.singleton.SingletonManager;

/**
 * Main purpose of this class is to recycle Service classes so that one instance of each
 * is sufficient for the entire app.
 * 
 * @author ryanho
 *
 */
public class ServiceFactory {
	private static SingletonManager<BaseService> singletonManager = new SingletonManager<BaseService>();
	
	public static void initWithServices(List<BaseService> services){
		for(BaseService service : services) {
			singletonManager.addInstance(service.getClass().getName(), service);
			service.start();
		}
	}
	public static <T extends BaseService>T getService(Class<T> serviceClass) {
		@SuppressWarnings("unchecked")
		T serviceInstance = (T) singletonManager.getInstance(serviceClass.getName());
		return serviceInstance;
	}
	public static void stopAllServices(){
		Collection<BaseService> services = singletonManager.getAllInstances();
		for(BaseService service : services) {
			service.stop();
		}
	}
}
