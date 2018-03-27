package com.rodrigopontes.wififiletransfer;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class HttpFileServer extends NanoHTTPD {

	Context context;
	MenuActivity menuActivity;
	StringBuilder htmlContent = new StringBuilder();
	File currentPath;
	FileInputStream fileInputStream = null;
	BufferedInputStream bufferedInputStream = null;
	BufferedOutputStream bufferedOutputStream = null;

	public HttpFileServer(int port, Context context, MenuActivity menuActivity) {
		super(port);
		this.context = context;
		this.menuActivity = menuActivity;
	}

	public void create() {
		try {
			start(SOCKET_READ_TIMEOUT, false);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Response serve(IHTTPSession session) {
		menuActivity.activateWiFiLED();
		try {
			if(session.getMethod().equals(Method.POST)) {
				Map<String, String> files = new HashMap<>();
				session.parseBody(files);
				bufferedInputStream = new BufferedInputStream(new FileInputStream(files.get("FileInput")));
				bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(currentPath + "/" + session.getParms().get("FileInput")));
				byte[] data = new byte[32768];
				while(bufferedInputStream.read(data) > 0) {
					menuActivity.activateHDDLED();
					bufferedOutputStream.write(data);
				}
				bufferedOutputStream.flush();
			}
			String uri = URLDecoder.decode(session.getUri(), "UTF-8");
			switch(uri) {
				case "/WiFiFileTransferFolderIcon":
					bufferedInputStream = new BufferedInputStream(context.getResources().openRawResource(R.raw.folder));
					menuActivity.activateWiFiLED();
					return newChunkedResponse(Response.Status.OK, "image/png", bufferedInputStream);
				case "/WiFiFileTransferFileIcon":
					bufferedInputStream = new BufferedInputStream(context.getResources().openRawResource(R.raw.file));
					menuActivity.activateWiFiLED();
					return newChunkedResponse(Response.Status.OK, "image/png", bufferedInputStream);
				case "/WiFiFileTransferAudioIcon":
					bufferedInputStream = new BufferedInputStream(context.getResources().openRawResource(R.raw.audio));
					menuActivity.activateWiFiLED();
					return newChunkedResponse(Response.Status.OK, "image/png", bufferedInputStream);
				case "/WiFiFileTransferVideoIcon":
					bufferedInputStream = new BufferedInputStream(context.getResources().openRawResource(R.raw.video));
					menuActivity.activateWiFiLED();
					return newChunkedResponse(Response.Status.OK, "image/png", bufferedInputStream);
				case "/WiFiFileTransferImageIcon":
					bufferedInputStream = new BufferedInputStream(context.getResources().openRawResource(R.raw.image));
					menuActivity.activateWiFiLED();
					return newChunkedResponse(Response.Status.OK, "image/png", bufferedInputStream);
				case "/WiFiFileTransferBackArrowIcon":
					bufferedInputStream = new BufferedInputStream(context.getResources().openRawResource(R.raw.back_arrow));
					menuActivity.activateWiFiLED();
					return newChunkedResponse(Response.Status.OK, "image/png", bufferedInputStream);
				default:
					htmlContent.setLength(0);
					currentPath = new File(Environment.getExternalStorageDirectory(), uri);
					if(currentPath.isDirectory()) {
						createHtmlHeader();
						if(!uri.equals("/")) {
							appendHtml("<tr>" +
									"<td height=\"60\" width=\"80\" align=\"center\"><img src=\"/WiFiFileTransferBackArrowIcon\" width=\"40\" height=\"40\"></td>" +
									"<td width=\"500\"/><a href=", encodeUri(currentPath.getParent()), ">Back</a><br>" +
									"</tr>");
						}
						File[] files = currentPath.listFiles();
						Arrays.sort(files);
						for(File file : files) {
							if(!file.isHidden()) {
								appendHtml("<tr>");
								if(file.isDirectory()) {
									appendHtml("<td height=\"60\" width=\"80\" align=\"center\"><img src=\"/WiFiFileTransferFolderIcon\" width=\"40\" height=\"40\"></td>");
								} else {
									String fileName = file.getName();
									if(fileName.endsWith("mp3") || fileName.endsWith("m3u") || fileName.endsWith("wma") || fileName.endsWith("wav")) {
										appendHtml("<td height=\"60\" width=\"80\" align=\"center\"><img src=\"/WiFiFileTransferAudioIcon\" width=\"40\" height=\"40\"></td>");
									} else if(fileName.endsWith("mp4") || fileName.endsWith("ogv") || fileName.endsWith("flv") || fileName.endsWith("mov")) {
										appendHtml("<td height=\"60\" width=\"80\" align=\"center\"><img src=\"/WiFiFileTransferVideoIcon\" width=\"40\" height=\"40\"></td>");
									} else if(fileName.endsWith("jpg") || fileName.endsWith("jpeg") || fileName.endsWith("png") || fileName.endsWith("gif") || fileName.endsWith("svg")) {
										appendHtml("<td height=\"60\" width=\"80\" align=\"center\"><img src=\"/WiFiFileTransferImageIcon\" width=\"40\" height=\"40\"></td>");
									} else {
										appendHtml("<td height=\"60\" width=\"80\" align=\"center\"><img src=\"/WiFiFileTransferFileIcon\" width=\"40\" height=\"40\"></td>");
									}
								}
								appendHtml("<td width=\"500\"><a href=\"" + encodeUri(file.getPath()) + "\" title=\"" + file.getPath() + "\">" + file.getName() + "</a></td>");
							}
						}
						htmlContent.append("</table></body></html>");
						menuActivity.activateWiFiLED();
						menuActivity.activateHDDLED();
						return newFixedLengthResponse(htmlContent.toString());
					} else if(currentPath.isFile()) {
						fileInputStream = new FileInputStream(currentPath);
						menuActivity.activateWiFiLED();
						menuActivity.activateHDDLED();
						return newFixedLengthResponse(Response.Status.OK, "application/octet-stream", fileInputStream, currentPath.length());
					}
			}
		} catch(ResponseException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void terminate() {
		try {
			if(fileInputStream != null) fileInputStream.close();
			if(bufferedInputStream != null) bufferedInputStream.close();
			if(bufferedOutputStream != null) bufferedOutputStream.close();
			stop();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	private void appendHtml(String... lines) {
		for(String line : lines) {
			htmlContent.append(line);
			htmlContent.append("\n");
		}
	}

	private String encodeUri(String path) {
		String subpath = path.substring(19);
		if(subpath.isEmpty()) {
			return "/";
		} else {
			String[] components = subpath.split("/");
			StringBuilder encodedUri = new StringBuilder();
			try {
				for(String component : components) {
					encodedUri.append("/");
					encodedUri.append(URLEncoder.encode(component, "UTF-8"));
				}
				return encodedUri.substring(1);
			} catch(UnsupportedEncodingException e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	private void createHtmlHeader() {
		try {
			appendHtml("<!DOCTYPE HTML>",
					"<html>",
					"<head>",
					"<title>WiFi File Transfer</title>",
					"<style>");
			BufferedReader cssReader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.style)));
			String line;
			while((line = cssReader.readLine()) != null) {
				appendHtml(line);
			}
			appendHtml("</style>",
					"</head>");
			if(!currentPath.toString().substring(19).equals("")) {
				appendHtml("<header>",
						"<h1>");
				File path = currentPath.getAbsoluteFile();
				ArrayList<File> components = new ArrayList<>();
				components.add(path);
				while(currentPath.getParent() != null) {
					path = path.getParentFile();
					if(path.equals(Environment.getExternalStorageDirectory()))
						break;
					components.add(path);
				}
				Collections.reverse(components);
				for(File component : components) {
					appendHtml("<a href=\"" + encodeUri(component.getPath()) + "\" title=\"" + component.getPath() + "\">/ " + component.getName() + " </a>");
				}
				appendHtml("</h1>",
						"</header>");
			} else {
				appendHtml("<header>" +
						"<h1> </h1>",
						"</header>");
			}
			appendHtml("<body>",
					"<form method=\"post\" enctype=\"multipart/form-data\">",
					"<input name=\"FileInput\" type=\"file\">",
					"<button type=\"submit\">Upload</button>",
					"</form>",
					"<table>");
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}

