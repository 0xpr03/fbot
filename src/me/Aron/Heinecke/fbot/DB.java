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
package me.Aron.Heinecke.fbot;

import me.Aron.Heinecke.fbot.lib.cLogger;

import org.apache.http.client.CookieStore;

public class DB {
	private String reqtoken;
	private String SSOCOM;
	private CookieStore cookieStore;
	private String link_file0;
	private String link_file1;
	private String save_file0; // file0
	private String save_file1; // file1
	private String save_file2; // last check
	private String save_dir;
	public String getData_dir() {
		return data_dir;
	}

	public void setData_dir(String data_dir) {
		this.data_dir = data_dir;
	}

	private String data_dir;
	private String user;
	private String pass;
	private String cmd;
	public cLogger log;
	
	public cLogger getLog() {
		return log;
	}

	public void setLog(cLogger log) {
		this.log = log;
	}

	public String getCmd() {
		return cmd;
	}

	public void setCmd(String cmd) {
		this.cmd = cmd;
	}

	public String getPass() {
		return pass;
	}

	public void setPass(String pass) {
		this.pass = pass;
	}

	public String getLink_file0() {
		return link_file0;
	}

	public void setLink_file0(String link_file0) {
		this.link_file0 = link_file0;
	}

	public String getLink_file1() {
		return link_file1;
	}

	public void setLink_file1(String link_file1) {
		this.link_file1 = link_file1;
	}

	public String getSave_file0() {
		return save_file0;
	}

	public void setSave_file0(String save_file0) {
		this.save_file0 = save_file0;
	}

	public String getSave_dir() {
		return save_dir;
	}

	public void setSave_dir(String save_dir) {
		this.save_dir = save_dir;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public DB(){
	}

	public CookieStore getCookieStore() {
		return cookieStore;
	}

	public void setCookieStore(CookieStore cookieStore) {
		this.cookieStore = cookieStore;
	}

	public String getReqtoken() {
		return reqtoken;
	}

	public void setReqtoken(String reqtoken) {
		this.reqtoken = reqtoken;
	}

	public String getSSOCOM() {
		return SSOCOM;
	}

	public void setSSOCOM(String sSOCOM) {
		SSOCOM = sSOCOM;
	}
	
	public String getdataFile0Path(){
		return this.getData_dir()+ this.getSave_file0() + ".pdf";
	}
	
	public String getdataFile1Path(){
		return this.getData_dir()+ this.getSave_file1() + ".pdf";
	}

	public String getSave_file1() {
		return save_file1;
	}

	public void setSave_file1(String save_file1) {
		this.save_file1 = save_file1;
	}

	public String getSave_file2() {
		return save_file2;
	}

	public void setSave_file2(String save_file2) {
		this.save_file2 = save_file2;
	}
}
