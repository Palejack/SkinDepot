package net.shadowboxx.skindepot.upload;

import com.google.common.io.Files;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Uploader for Multipart form data
 *
 * @author Adam Mummery-Smith
 */
public class ThreadMultipartPostUpload extends Thread {
    protected final Map<String, ?> sourceData;

    protected final String method;

    protected final String authorization;

    protected final String urlString;

    protected final IUploadCompleteCallback callback;
    
    protected Boolean SSL = false;

    protected HttpsURLConnection httpsClient;
    
    protected HttpURLConnection httpClient;

    protected static final String CRLF = "\r\n";

    protected static final String twoHyphens = "--";

    protected static final String boundary = "----WebKitFormBoundaryqZ4IlT8wYfikAclf";

    public String response;

    public ThreadMultipartPostUpload(String method, String url, Map<String, ?> sourceData, String authorization, IUploadCompleteCallback callback) {
        this.method = method;
        this.urlString = url;
        this.sourceData = sourceData;
        this.authorization = authorization;
        this.callback = callback;
        this.SSL = url.contains("https");
    }

    public ThreadMultipartPostUpload(String url, Map<String, ?> sourceData, IUploadCompleteCallback callback) {
        this("POST", url, sourceData, null, callback);
    }

    public String getResponse() {
        return this.response == null ? "" : this.response.trim();
    }

    @Override
    public void run() {
        try {
            this.uploadMultipart(this.SSL);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        this.callback.onUploadComplete(this.getResponse());
    }
    
    protected void uploadMultipart(boolean ssl) throws IOException {
        // open a URL connection
        URL url = new URL(this.urlString);
        URLConnection client;
        
        if (ssl) {
        	this.httpsClient = (HttpsURLConnection) url.openConnection();
        	SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        	this.httpsClient.setSSLSocketFactory(socketFactory);
        	client = this.httpsClient;
        } else {
        	this.httpClient = (HttpURLConnection) url.openConnection();
        	client = this.httpClient;
        }

        // Open a HTTP connection to the URL
        //this.httpClient = (HttpURLConnection) url.openConnection();
        client.setDoOutput(true);
        client.setDoInput(true);
        client.setUseCaches(false);

        if (ssl) {
        	((HttpsURLConnection) client).setRequestMethod(this.method);
        } else {
        	((HttpURLConnection) client).setRequestMethod(this.method);
        }
        //this.httpClient.setRequestProperty("Connection", "Close");
        client.setRequestProperty("Accept", "*/*");
        client.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36"); // For CloudFlare

        if (this.sourceData.size() > 0) {
            client.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        }

        if (this.authorization != null) {
            client.addRequestProperty("Authorization", this.authorization);
        }

        OutputStream outputStream = client.getOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);

        for (Entry<String, ?> data : this.sourceData.entrySet()) {
            writer.append(twoHyphens + boundary).append(CRLF);

            String paramName = data.getKey();
            Object paramData = data.getValue();

            if (paramData instanceof File) {
                File uploadFile = (File) paramData;
                writer.append("Content-Disposition: form-data; name=\"" + paramName + "\"; filename=\"" + uploadFile.getName() + "\"").append(CRLF);
                writer.append("Content-Type: image/png").append(CRLF).append(CRLF);
                writer.flush();
                Files.copy(uploadFile, outputStream);
				outputStream.flush();
                writer.append(CRLF);
                writer.flush();

            } else {
                writer.append("Content-Disposition: form-data; name=\"" + paramName + "\"").append(CRLF).append(CRLF);
                writer.append(paramData.toString()).append(CRLF);
                LiteLoaderLogger.info(outputStream.toString());
            }
            
        }

        writer.append(twoHyphens + boundary + twoHyphens).append(CRLF);
        writer.append(CRLF).flush();
        outputStream.flush();

        InputStream httpStream = client.getInputStream();

        try {
            StringBuilder readString = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(httpStream));

            String readLine;
            while ((readLine = reader.readLine()) != null) {
                readString.append(readLine).append("\n");
            }

            reader.close();
            this.response = readString.toString();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        outputStream.close();
    }

}
