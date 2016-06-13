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

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.SSLException;

import org.apache.http.NoHttpResponseException;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;

import me.Aron.Heinecke.fbot.lib.Converter;
import me.Aron.Heinecke.fbot.lib.FileNotFoundException;
import me.Aron.Heinecke.fbot.lib.Socket;
import me.Aron.Heinecke.fbot.lib.cConfig;
import me.Aron.Heinecke.fbot.lib.cLogger;

/**
 * Main class
 * @author Aron Heinecke
 */
public class fbot {
	public static boolean debug = true;

	private static String sdf = "yyyy-MM-dd HH:mm:ss";
	private static String log_file_name = "fbot_log.log";

	private static cLogger log;
	private static cConfig config;

	public static Converter conv;
	public static Socket soc;

	private static DB db;
	private static byte[] hash1;
	private static byte[] hash2;

	private static int interval = 180;
	private static Timer maintimer;
	private static TimerTask filetask;

	private static boolean info;

	public static void main(String[] args) {
		// initialize logger
		log = new cLogger(log_file_name, sdf);
		log.info("main", "#######################");
		log.info("main", "Starting fbot v.2.7.4");
		// log.info("main", "DEV-BUILD FOR TESTING PURPOSE!");
		log.info("main", "support.proctet@t-online.de");

		try {// load configuration, start socket, converter, threads, register
				// exit code
			config = new cConfig("fbot_config.cfg", "/me/Aron/Heinecke/fbot/default.cfg", log, true);
			config.load(true);
			loadConf();

			conv = new Converter();
			soc = new Socket();

			registerExitFunction();
			registerThreads();

		} catch (Exception e) { // catches all errors and log them, otherwise
								// crashes in productive state aren't
								// reconstructible
			log.severe("main", "Catched error in last instance!");
			log.exception("main", e);
		}
		while (true) {
			try {
				// Make sure that the Java VM don't quit this program.
				Thread.sleep(100);
			} catch (Exception e) {/* ignore */
			}
		}
	}

	/***
	 * exit function, stops the task & closes the socket
	 */
	private static void registerExitFunction() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				log.info("main", "Shutting down fbot..");
				try {
					filetask.cancel();
					maintimer.cancel();
				} catch (NullPointerException e) {
					// ignore, caused when thread never started..
				}
				if (info)
					log.info("main", "Canceled threads.");

				try {
					soc.logout();
				} catch (IOException e) {
					log.exception("main", e);
				}
				if (info)
					log.info("main", "Logged out.");

				soc.closeSocket();
				if (info)
					log.info("main", "Closed socket.");

				log.info("main", "Closing log & stopping..");
				log.close();
			}
		});
	}

	/****
	 * registers the timed task to download both pdfs
	 */
	private static void registerThreads() {
		maintimer = new Timer(true);
		filetask = new TimerTask() {
			public void run() {
				hash1 = Task(0, hash1);
				hash2 = Task(1, hash2);
			}
		};
		maintimer.schedule(filetask, 0, 1000 * interval);// 1000*sec
	}

	/***
	 * Get one file if hashsum changed
	 * 1) download
	 * 2) get hash
	 * 3) write LUC: "last update check" timestamp
	 * 4) stop if hash is the same
	 * 5) get number of sites
	 * 6) set new hash
	 * 7) convert pdf to html via pdf2html cmd
	 * 8) restyle the output, as it's very bad and unusable otherwise
	 * 9) add a timestamp to the pdf
	 * @param fid file id 0/1 = today/tomorrow
	 * @param hash hash summ expected
	 * @return new hash summ
	 */
	private static byte[] Task(int fid, byte[] hash) {
		try {
			String file = getPDF(fid);// try download
			if (debug)
				log.debug("task", file);
			if (file == null) { // failed to download check
				log.severe("task", "Failed downloading file!");
			} else {
				// try to get hash
				byte[] b;
				b = conv.getHash(new File(file));
				if (b == null) { // failed to generate hash check
					log.severe("task", "Failed to generate hashsumm!");
				} else if (Arrays.equals(hash, b)) { // no file change check
					if (debug)
						log.debug("task", "Got same hash.");
					if (conv.writeLUC(new File(db.getData_dir() + db.getSave_file2()))) {
						if (debug)
							log.debug("task", "Wrote LUC");
						if (info)
							log.info("task", "scheduled");
					} else {
						log.severe("task", "Couldn't write LUC");
					}
				} else { // file changed, different hash
							// try to get amount of sites
					hash = b; // set new hash
					long starttime = System.currentTimeMillis();
					int sites = conv.pdfSites(file);// get number of sites
					if (sites == -1) {// check sites get fail
						log.severe("task", "Couldn't get number of pages!");
					} else {// got number of sites
							// try to convert the pdf to html
						if (debug)
							log.info("task", "Got numer of pages:" + sites);
						String out = conv.pdf2html(file);
						if (!out.contains("Page-1")) {
							log.severe("task", "Couldn't convert html to pdf: " + out);
						} else {
							// try to restyle all html files
							if (debug)
								log.debug("task", "Successfully converted html to pdf.");
							boolean fail = false; // convert each file & add to
													// the last one the notes
							StringBuilder sb = new StringBuilder();
							String output;
							for (int i = 1; i < sites; i++) {
								output = conv
										.restyleHtml(new File(file.replace(".pdf", "-" + i + ".html")), false, 0, i == 1);
								if (output == null)
									fail = true;
								sb.append(output);
							}
							if (!fail) {
								output = conv
										.restyleHtml(new File(file.replace(".pdf", "-" + sites + ".html")), true, starttime, sites == 1);
								fail = (output == null);
								if (!fail) {
									sb.append(output);
									if (debug)
										log.debug("task", db.getSave_dir() + "/" + getfileName(fid) + ".html");
									conv.writeContent(db.getSave_dir() + "/" + getfileName(fid) + ".html", sb
											.toString());
									if (debug)
										log.debug("task", "wrote file" + fid);
								}
							}

							if (fail) {
								log.severe("task", "Couldn't restyle the html file!");
							} else {
								// add note to pdf & write to file0.pdf in main
								// save directory
								if (debug)
									log.debug("task", "Successfully restyled the html file.");
								if (conv.putNote(file, db.getSave_dir() + "/" + getfileName(fid) + ".pdf") == null) {
									log.severe("task", "Couldn't add the PDF note!");
								} else {
									if (debug)
										log.debug("task", "Added PDF note.");
									if (conv.writeLUC(new File(db.getData_dir() + db.getSave_file2()))) {
										if (debug)
											log.debug("task", "Wrote LUC");
										if (info)
											log.info("task", "scheduled");
									} else {
										log.severe("task", "Couldn't write LUC");
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			log.severe("task", "Catched error in last instance:task!");
			log.exception("task", e);
		}
		return hash;
	}

	/***
	 * Initiates the PDF download for today- / tomorrow-pdf
	 * @param id file today/tomorrow 0/1
	 * @return pdf file path, null if it failed
	 */
	private static String getPDF(int id) {
		String out = null;
		try {
			soc.getReqID(); // get fronter ids
			soc.login(); // login

			switch (id) {
			case 0: // get pdf "heute"
				out = db.getdataFile0Path();
				soc.downloadFile("https://fronter.com/giessen/links/" + db.getLink_file0(), out);
				break;
			case 1: // get pdf "morgen"
				out = db.getdataFile1Path();
				soc.downloadFile("https://fronter.com/giessen/links/" + db.getLink_file1(), out);
				break;
			}
			soc.logout();
		} catch (ConnectTimeoutException e) {
			log.severe("downloadFile", "Connection Timeout! Server unreachable?");
			out = null;
		} catch (SSLException e) {
			log.severe("downloadFile", "SSL exception! Server overloaded?");
			out = null;
		} catch (UnknownHostException e) {
			log.severe("downloadFile", "Unknown-Host exception! Server unreachable?");
			out = null;
		} catch (HttpHostConnectException e){
			log.severe("downloadFile", "HttpHostConnectException! Server overloaded?");
			out = null;
		} catch (NoHttpResponseException e){
			log.severe("downloadFile", "NoHttpResponseException! Server overloaded?");
			out = null;
		} catch (FileNotFoundException e) {
			log.severe("downloadFile", "FileNotFoundException! PDF isn't existing?");
			out = null;
		} catch (SocketException e) {
			log.severe("downloadFile", "Connection Reset! Server overloaded?");
			out = null;
		} catch (IOException e) {
			log.exception("downloadFile", e);
			out = null;
		} catch (StringIndexOutOfBoundsException e) {
			log.exception("downloadFile", e);
			out = null;
		}
		return out;
	}

	/***
	 * loads the configuration
	 */
	private static void loadConf() {
		debug = config.getProperty("debug", "true").equalsIgnoreCase("true");
		info = config.getProperty("info", "true").equalsIgnoreCase("true");
		config.setDebug(debug);
		sdf = config.getProperty("sdf", "yyyy-MM-dd HH:mm:ss");
		log.setDateFormat(sdf);// reload dateformat of logger

		db = new DB();
		db.setLink_file0(config.getProperty("link_file0", "link.phtml?idesc=1&iid=12841"));
		db.setLink_file1(config.getProperty("link_file1", "link.phtml?idesc=1&iid=12839"));
		db.setSave_file0(config.getProperty("save_file0", "file0"));
		db.setSave_file1(config.getProperty("save_file1", "file1"));
		db.setSave_file2(config.getProperty("save_file2", "file2"));
		db.setSave_dir(config.getProperty("save_dir", ""));
		db.setUser(config.getPropertySec("user", ""));
		db.setPass(config.getPropertySec("pass", ""));
		db.setCmd(config.getProperty("cmd", "pdftohtml -c %f"));
		interval = Integer.valueOf(config.getProperty("interval", "180"));
		if (db.getSave_dir().equalsIgnoreCase("")) { // set save directory if
														// path isn't set
			db.setSave_dir(System.getProperty("user.dir") + "/");
		}
		db.setData_dir(db.getSave_dir() + "/data/"); // set data directory
		new File(db.getData_dir()).mkdirs(); // create data directory
	}

	/***
	 * returns the pdf name
	 * @param fid 0/1 for today/tomorrow
	 * @return configured file name
	 */
	private static String getfileName(int fid) {
		if (fid == 0) {
			return db.getSave_file0();
		} else {
			return db.getSave_file1();
		}
	}

	public static DB getDB() {
		return db;
	}

	public static cLogger getLogger() {
		return log;
	}

	public static boolean isDebug() {
		return debug;
	}

	public static String getdateFormat() {
		return sdf;
	}
}
