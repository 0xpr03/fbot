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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.exceptions.InvalidPdfException;
import com.itextpdf.text.io.RandomAccessSourceFactory;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.RandomAccessFileOrArray;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.itextpdf.text.pdf.parser.SimpleTextExtractionStrategy;

import me.Aron.Heinecke.fbot.fbot;

public class Converter {
	long CONVERT_TIMEOUT = 4000; // MS
	private DateFormat dateFormat;
	
	public Converter() {
		dateFormat = new SimpleDateFormat(fbot.getdateFormat());
		if(fbot.isDebug()) fbot.getLogger().debug("converter", fbot.getdateFormat());
	}

	/***
	 * Converts a pdf file to html using the command in the DB
	 * The html files are stored in the config specified folder
	 * @param file path of the file to convert
	 * @return contains the cmd execution output
	 */
	public String pdf2html(String file) {
		try {
			java.lang.Runtime rt = java.lang.Runtime.getRuntime();
			// Start a new process: UNIX command
			java.lang.Process p = rt.exec(fbot.getDB().getCmd().replace("%f", file));
			
			// wait for process finish or it reaches a timeout
			long now = System.currentTimeMillis();
			long finish = now + CONVERT_TIMEOUT;
			while(isAlive(p) &&  (System.currentTimeMillis() < finish))
			{
				Thread.sleep(10);
			}
			if(isAlive(p))
			{
				fbot.getLogger().severe("converter", "pdf2html timeout");
				p.destroy();
				return "ERROR!";
			}
			// get process output
			java.io.InputStream is = p.getInputStream();
			java.io.BufferedReader reader = new java.io.BufferedReader(new InputStreamReader(is));
			String s = null;
			StringBuilder sb = new StringBuilder();
			while ((s = reader.readLine()) != null) {
				sb.append(s);
			}
			is.close();
			reader.close();
			if ( fbot.isDebug() )
				fbot.getLogger().debug("converter", sb.toString());
			return sb.toString();
		} catch ( IOException | InterruptedException e ) {
			fbot.getLogger().exception("converter", e);
		}
		return "ERROR!";
	}

	/**
	 * Returns whether a process is still alive or not.
	 * @param p
	 * @return true for alive
	 */
	private static boolean isAlive(Process p){
		try{
			p.exitValue();
			return false;
		}catch(IllegalThreadStateException e) {
			return true;
		}
	}
	
	/***
	 * Return the amount of sites in a pdf
	 * using a deprecated (working) iText function
	 * @param file path of the file to use
	 * @return amount of sites
	 */
	@SuppressWarnings("deprecation")
	public int pdfSites(String file){
		try {
			RandomAccessFile raf = new RandomAccessFile(new File(file), "r");
			RandomAccessFileOrArray pdfFile;
			pdfFile = new RandomAccessFileOrArray(new RandomAccessSourceFactory().createSource(raf));
			PdfReader reader = new PdfReader(pdfFile, new byte[0]);
			int pages = reader.getNumberOfPages();
			reader.close();
			return pages;
		} catch ( InvalidPdfException e ) {
			fbot.getLogger().severe("converter", "Invalid PDF file!: no index");
		} catch ( Exception e ) {
			fbot.getLogger().exception("converter", e);
		}
		return -1;
	}

	/***
	 * Restyles the html resulted by the pdf2html conversion
	 * So it's usable in the website & one file
	 * @param file path of the file to be restyled
	 * @param addNote append "generated in" note
	 * @param starttime start time for the note
	 * @param addTime add pdf time and table header
	 * @return returns the converted html, debug usage
	 */
	public String restyleHtml(File file,boolean addNote, long starttime, boolean addTime) {
		try {
			String rawData = loadContent(file);
			
			rawData = rawData.replaceAll("</P>", "").replaceAll("&#160;", " ").replaceAll("</p>", "");//get input & first gc, delete all of the &nbsp;'s
			
			if(fbot.isDebug()) fbot.getLogger().debug("converter", "Raw Data:\n"+ rawData);
			
			//replace parts with html syntax
			StringBuilder sbr = new StringBuilder();
			StringBuilder sbr2 = new StringBuilder();
			if(addTime)sbr.append("<table id=\"table\" class=\"tablesorter\">\n<thead><tr><th>Kürzel</th><th>H.</th><th>St.</th><th>R.</th><th>Zusatz</th></tr></thead>\n"); // time only in 1. file; create table only in first file..
			String year = "."+String.valueOf(Calendar.getInstance().get(Calendar.YEAR))+"</b>";
			for ( String s : rawData.split("\n") ) {
				if(s.contains(year)){
					if(addTime)sbr.insert(0, "<font size=4>"+ s.substring(s.indexOf("ft00\">")+6) +"</font>\n" );
				}else{
					if (s.contains("ft01")) { // site 1
						if (s.contains(">Vertretungen:")) {
							if(fbot.isDebug())fbot.getLogger().debug("converter", "FT1: \n" + s);
						} else if (s.contains("left:364px") || s.contains("left:394px") || s.contains("left:431px")) { //h. St. R.
							sbr.append("<td>" + s.substring(s.indexOf("ft01\">") + 6) + "</td>");
						} else if (s.contains("left:311")) { // Kürzel
							sbr.append("<tr><td>" + s.substring(s.indexOf("ft01\">") + 6) + "</td>");
						} else if (s.contains("left:469px")) { // Zusatz
							sbr.append("<td>" + s.substring(s.indexOf("ft01\">") + 6) + "</td></tr>\n");
						} else if (s.contains("left:465")) { // unknown, zusatz ?
							//sbr.append("<td>" + s.substring(s.indexOf("ft01\">") + 6) + "</td></tr>\n");
						}
					} else if (s.contains("ft00")) { // site > 1
						if (s.contains("<b>Vertretungen</b>")) {
							if(fbot.isDebug())fbot.getLogger().debug("converter", "FT0: \n" + s);
							// sbr.append("<tr><th>Kürz.</th><th>h</th><th>Stufe</th><th>Saal</th><th>Vertretung etc.</th></tr>");
						} else if (s.contains("left:364px") || s.contains("left:394px") || s.contains("left:431px")) { //h. St. R.
							sbr.append("<td>" + s.substring(s.indexOf("ft00\">") + 6) + "</td>");
						} else if (s.contains("left:311px")) { // Kürzel
							sbr.append("<tr><td>" + s.substring(s.indexOf("ft00\">") + 6) + "</td>");
						} else if (s.contains("left:469px")) { // Zusatz
							sbr.append("<td>" + s.substring(s.indexOf("ft00\">") + 6) + "</td></tr>\n");
						} else if (s.contains("left:465")) { // unknown, zusatz ?
							//sbr.append("<td>" + s.substring(s.indexOf("ft00\">") + 6) + "</td></tr>\n");
						}
					} else if (s.contains("ft02")) {
						if (s.contains("left:49px")) { // Zusatz über dem Plan
							sbr2.append("<b>" + s.substring(s.indexOf("ft02\">") + 6) + "</b><br>\n");
						}
					} else if( s.contains("ft03")) {
						if (s.contains("<b>")){
							sbr2.append("<b>" + s.substring(s.indexOf("ft03\">") + 6) + "</b><br>\n");
						}
					}
				}
			}
			if(addNote){
				sbr.append("</table>\n<br><i>Generated in "+(System.currentTimeMillis()-starttime)+"(ms)<br>");
				sbr.append("Last change: "+getFormatedDate()+"</i>");
			}
			sbr.insert(0, sbr2.toString()); // add sbr2 at the start of sbr
			if(fbot.isDebug())fbot.getLogger().debug("converter", "SBR2:\n"+sbr2.toString() + "\nSBR1:\n"+sbr.toString());
			
			return sbr.toString();
		} catch ( IOException e ) {
			fbot.getLogger().exception("converter", e);
			return null;
		}
	}
	
	public synchronized String loadContent(File file)throws IOException{
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
		String str;
		StringBuilder sb = new StringBuilder();
		while ((str = in.readLine()) != null) {
			sb.append(str+"\n");
		}
		in.close();
		return sb.toString();
	}
	
	public synchronized String loadContent(String file)throws IOException{
		return loadContent(new File(file));
	}
	
	public synchronized boolean writeContent(String file, String content) {
		try {
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
			out.write(content);
			out.flush();
			out.close();
			return true;
		} catch (IOException e) {
			fbot.getLogger().exception("converter", e);
			return false;
		}
	}

	//get hash-sum of file
	public byte[] getHash(File file) { // return hash
		byte[] digest = null;
		try {
			//read file
			byte[] buffer = new byte[(int) file.length()];
			FileInputStream fis = new FileInputStream(file);
			fis.read(buffer);
			fis.close();
			//get md5 instance
			MessageDigest md = MessageDigest.getInstance("MD5");
			//write buffer to md5 instance
			md.update(buffer);
			//get hash
			digest = md.digest();
		} catch ( NoSuchAlgorithmException | IOException e ) {
			fbot.getLogger().exception("converter", e);
		}
		return digest;
	}
	
	/***
	 * Writes the last update check timestamp into a file
	 * @param file path of the file to be used
	 * @return successfully
	 */
	public boolean writeLUC(File file){
		try{
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
			out.write(getFormatedDate());
			out.flush();
			out.close();
			return true;
		} catch ( IOException e ) {
			fbot.getLogger().exception("converter", e);
		}
		return false;
	}
	
	/***
	 * Runs addPDFNote with predefined statements
	 * @param rfile pdf file to be read from
	 * @param wfile pdf file to be written to
	 * @return path of the pdf file, null if it failed
	 */
	public String putNote(String rfile, String wfile){
			return addPDFNote(new File(rfile), new File(wfile), "Last changed: "+getFormatedDate()+"|generated by fronter.proctet.net");
	}
	
	private String getFormatedDate(){
		return dateFormat.format(new Date(System.currentTimeMillis()));
	}
	
	/***
	 * Add a note to the bottom of a pdf file in italic font
	 * @param rfile file to be read from
	 * @param wfile file to be written to
	 * @param text text to add
	 * @return path to the resulting pdf, null if it failed
	 */
	private String addPDFNote(File rfile, File wfile, String text){
		try{
			PdfReader pdfReader = new PdfReader(rfile.getAbsolutePath());

		      PdfStamper pdfStamper = new PdfStamper(pdfReader,new FileOutputStream(wfile));

		      for(int i=1; i<= pdfReader.getNumberOfPages(); i++){

		          PdfContentByte cb = pdfStamper.getUnderContent(i);
		          
		          BaseFont bf = BaseFont.createFont();
		          bf.setPostscriptFontName("ITALIC");
		          cb.beginText();
		          cb.setFontAndSize(bf, 12);
		          cb.setTextMatrix(10, 20);
		          cb.showText(text);
		          cb.endText();
		      }

		      pdfStamper.close();
			return wfile.getAbsolutePath();
		}catch(IOException | DocumentException e){
			fbot.getLogger().exception("converter", e);
			return null;
		}
	}

	@Deprecated
	public String ReadPdfFile(File file) throws IOException {
		StringBuilder text = new StringBuilder();

		if ( file.exists() ) {
			PdfReader pdfReader = new PdfReader(file.getAbsolutePath());

			for ( int pageid = 1; pageid <= pdfReader.getNumberOfPages(); pageid++ ) {

				SimpleTextExtractionStrategy strategy = new SimpleTextExtractionStrategy();
				String currentText = PdfTextExtractor.getTextFromPage(pdfReader, pageid, strategy);

				//currentText = Encoding.UTF8.GetString(ASCIIEncoding.Convert(Encoding.Default, Encoding.UTF8, Encoding.Default.GetBytes(currentText)));
				text.append(currentText);
			}
			pdfReader.close();
		}
		return text.toString();
	}
}