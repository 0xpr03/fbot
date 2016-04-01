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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class cConfig {
	private File file;
	private String defaults;
	private cLogger log;
	private boolean debug;
	private Properties prop = new Properties();
	
	public cConfig(File file, String defaults, cLogger log, boolean debug){
		this.file = file;
		this.log = log;
		this.defaults = defaults;
		this.debug = debug;
	}
	public cConfig(String file, String defaults, cLogger log, boolean debug){
		this.file = new File(System.getProperty("user.dir")+"/"+file);
		if(debug)log.debug("config", "Path:"+this.file.getName());
		this.log = log;
		this.defaults = defaults;
		this.debug = debug;
	}
	
	/***
	 * Loads the config from the disk
	 * @param create config file if not existend
	 * @return successfull
	 */
	@SuppressWarnings("finally")
	public boolean load(boolean create){
		boolean successful = false;
		try {
			prop.load(new FileInputStream(file));
			return true;
		} catch ( FileNotFoundException e ) {
			if(create){
				log.info("config", "Config not found, recreating.");
				loadDefs();
			}else{log.exception("config", e);}
		} catch ( IOException e ) {
			log.exception("config", e);
		}finally{
			return successful;
		}
	}
	
	//load default, load inside default file and write to hard disk
	/***
	 * Loads the config default value to the disk
	 * @return successfully
	 */
	@SuppressWarnings("finally")
	public boolean loadDefs(){
		boolean successful = false;
		try {
			InputStream in = getClass().getResourceAsStream(defaults);
			// read file from class path, write to disk
			OutputStream out = new FileOutputStream(file);
			byte[] buffer = new byte[1024];
			int len = in.read(buffer);
			while (len != -1) {
			    out.write(buffer, 0, len);
			    len = in.read(buffer);
			}
			out.flush();
			out.close();
			//& read values
			prop.load(in);
			in.close();
			successful = true;
		} catch ( IOException e ) {
			log.exception("config", e);
		}finally{
			return successful;
		}
	}
	
	//save config
	/***
	 * Safes the config to the disk
	 * @return successfully
	 */
	@SuppressWarnings("finally")
	public boolean save(){
		boolean successful = false;
		try {
			prop.store(new FileOutputStream(file), null);
			successful = true;
		} catch ( FileNotFoundException e ) {
			log.exception("config", e);
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			log.exception("config", e);
		}finally{
			return successful;
		}
	}
	
	//set property
	/***
	 * Sets the config entry
	 * @param key property key
	 * @param value property value
	 */
	public void setProperty(String key, String value){
		prop.setProperty(key, value);
		if(debug){
			log.debug("config", "Setting prop: "+key+"\t"+value);
		}
	}
	
	//set property but only print * instead of clean text, if debug enabled
	/***
	 * Sets the config entry without value logging
	 * @param key property key
	 * @param value property value
	 */
	public void setPropertySec(String key, String value){
		prop.setProperty(key, value);
		if(debug){
			log.debug("config", "Setting prop: "+log.hideInfo(key)+"\t"+log.hideInfo(value));
		}
	}
	
	//get property
	/***
	 * Reads the config entry
	 * @param key property key to read
	 * @param defaultValue fallback value in case of missing value
	 * @return value
	 */
	public String getProperty(String key, String defaultValue){
		if(debug){
			log.debug("config", "Getting prop: "+key+"\t"+prop.getProperty(key)+"\tdefault:"+defaultValue);
		}
		return prop.getProperty(key, defaultValue);
	}
	
	//get property but only print * instead of clean text, if debug enabled
	/***
	 * Gets the config entry without loggint he value
	 * @param key property key to read
	 * @param defaultValue fallback value in case of missing value
	 * @return value
	 */
	public String getPropertySec(String key, String defaultValue){
		if(debug){
			log.debug("config", "Getting prop: "+key+"\t"+log.hideInfo(prop.getProperty(key))+"\tdefault:"+log.hideInfo(defaultValue));
		}
		return prop.getProperty(key, defaultValue);
	}
	
	//redefine debug state
	/***
	 * Sets the debug option, used to set it after config was read
	 * @param debug debug
	 */
	public void setDebug(boolean debug){
		this.debug = debug;
	}
}
