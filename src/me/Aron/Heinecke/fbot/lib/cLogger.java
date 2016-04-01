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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Custom logger
 * @author Aron Heinecke
 */
public class cLogger {
	private File file;
	private SimpleDateFormat dateFormat;
	private PrintStream logFile;

	/***
	 * Creates a new Logger
	 * @param file log file
	 * @param dateFormat date format to be used
	 */
	public cLogger(File file, String dateFormat){
		// for ex.:yyyy-MM-dd HH:mm:ss
		this.dateFormat = new SimpleDateFormat(dateFormat);
		this.file = file;
		try {
			logFile = new PrintStream(new FileOutputStream(file, true), true, "UTF-8");
		} catch ( UnsupportedEncodingException e ) {
			e.printStackTrace();
		} catch ( FileNotFoundException e ) {
			e.printStackTrace();
		}
	}
	
	/***
	 * Creates a new Logger
	 * @param file log file
	 * @param dateFormat date format to be used
	 */
	public cLogger(String file, String dateFormat){
		// for ex.:yyyy-MM-dd HH:mm:ss
		this.dateFormat = new SimpleDateFormat(dateFormat);
		this.file = new File(System.getProperty("user.dir")+"/"+file);
		try {
			logFile = new PrintStream(new FileOutputStream(file, true), true, "UTF-8");
		} catch ( UnsupportedEncodingException e ) {
			e.printStackTrace();
		} catch ( FileNotFoundException e ) {
			e.printStackTrace();
		}
	}
	
	/***
	 * Adds a log entry
	 * @param level log entry level
	 * @param type log entry type
	 * @param content entry content
	 */
	public void log(String level, String type, String content){
		logFile.println(dateFormat.format(new Date(System.currentTimeMillis())) + "\t" + level.toUpperCase() + "\t" + type.toUpperCase() + "\t" + content);
		System.out.println(dateFormat.format(new Date(System.currentTimeMillis())) + "\t" + level.toUpperCase() + "\t" + type.toUpperCase() + "\t" + content);
	}
	
	//log function with lists
	public void log(String level, String type, List<?> content){
		StringBuilder sb = new StringBuilder();
		for(Object s: content){
			sb.append(s.toString());
		}
		logFile.println(dateFormat.format(new Date(System.currentTimeMillis())) + "\t" + level.toUpperCase() + "\t" + type.toUpperCase() + "\t" + sb.toString());
		System.out.println(dateFormat.format(new Date(System.currentTimeMillis())) + "\t" + level.toUpperCase() + "\t" + type.toUpperCase() + "\t" + sb.toString());
	}
	
	/***
	 * Creates an exception log entry
	 * @param type log entry type
	 * @param e exception to be logged
	 */
	public void exception(String type, Exception e){
		log("SEVERE", type, "Undefined Error Exception e\n" + e.getMessage() + "\n" + e.getCause() + "\n" + Arrays.toString(e.getStackTrace()) +"\n"+ e.getClass().getName());
	}
	
	//log function for direct level info
	public void info(String type, String content){
		log("INFO", type, content);
	}
	
	//log function for direct level severe
	public void severe(String type, String content){
		log("SEVERE", type, content);
	}
	
	//log function for direct level debug
	public void debug(String type, String content){
		log("DEBUG", type, content);
	}
	
	//log function for direct level warning
	public void warning(String type, String content){
		log("WARNING", type, content);
	}
	
	public File getFile(){
		return file;
	}
	
	//redefine the date format
	public void setDateFormat(String dateFormat){
		this.dateFormat = new SimpleDateFormat(dateFormat);
	}
	
	//hide info (password.. etc. )
	public String hideInfo(String input) {
		String output = "";
		if(input != null){
			if(!input.equals("")){
				for ( int i = 0; i < input.length(); i++ ) {
					output = output + "*";
				}
			}
		}
		return output;
	}
	
	//close printstream
	public void close(){
		logFile.close();
	}
}
