package me.jaredhewitt.skindepot.upload;

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
import java.net.URL;
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
            this.uploadMultipart();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        this.callback.onUploadComplete(this.getResponse());
    }
    
    protected void uploadMultipart() throws IOException {
        // open a URL connection
        URL url = new URL(this.urlString);

        // Open a HTTP connection to the URL
        this.httpClient = (HttpURLConnection) url.openConnection();
        this.httpClient.setDoOutput(true);
        this.httpClient.setDoInput(true);
        this.httpClient.setUseCaches(false);

        this.httpClient.setRequestMethod(this.method);
        //this.httpClient.setRequestProperty("Connection", "Close");
        this.httpClient.setRequestProperty("Accept", "*/*");
        this.httpClient.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36"); // For CloudFlare

        if (this.sourceData.size() > 0) {
            this.httpClient.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        }

        if (this.authorization != null) {
            this.httpClient.addRequestProperty("Authorization", this.authorization);
        }

        OutputStream outputStream = this.httpClient.getOutputStream();
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

        InputStream httpStream = this.httpClient.getInputStream();

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
