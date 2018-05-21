package uk.ac.manchester.cs.spinnaker.utils;

import static java.io.File.createTempFile;
import static java.nio.file.Files.copy;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Map;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.jboss.resteasy.util.ParameterParser;

public abstract class FileDownloader {
	private FileDownloader() {
	}

	private static String getFileName(final String contentDisposition) {
        if (contentDisposition != null) {
            final String cdl = contentDisposition.toLowerCase();
            if (cdl.startsWith("form-data") || cdl.startsWith("attachment")) {
                final ParameterParser parser = new ParameterParser();
                parser.setLowerCaseNames(true);
                final Map<String, String> params =
                        parser.parse(contentDisposition, ';');
                if (params.containsKey("filename")) {
                    return params.get("filename").trim();
                }
            }
        }
        return null;
    }

    private static URLConnection createConnectionWithAuth(final URL url,
            final String userInfo) throws IOException {
        URLConnection urlConnection =
                requireNonNull(url).openConnection();
        urlConnection.setDoInput(true);

        if (urlConnection instanceof HttpsURLConnection) {
            initVeryTrustingSSLContext((HttpsURLConnection) urlConnection);
        }

        urlConnection.setRequestProperty("Accept", "*/*");
        if (userInfo != null && urlConnection instanceof HttpURLConnection) {
            HttpURLConnection httpConnection =
                (HttpURLConnection) urlConnection;
            String basicAuth = "Basic " + Base64.encodeBase64URLSafeString(
                userInfo.getBytes("UTF8"));
            httpConnection.setRequestProperty("Authorization", basicAuth);
            httpConnection.setInstanceFollowRedirects(false);
        }
        return urlConnection;
    }

    /**
     * Downloads a file from a URL.
     *
     * @param url
     *            The url to download the file from
     * @param workingDirectory
     *            The directory to output the file to
     * @param defaultFilename
     *            The name of the file to use if none can be worked out from the
     *            url or headers, or <tt>null</tt> to use a generated name
     * @return The file downloaded
     * @throws IOException
     *          If anything goes wrong.
     */
    public static File downloadFile(final URL url, final File workingDirectory,
            final String defaultFilename) throws IOException {
        requireNonNull(workingDirectory);

        // Open a connection
        String userInfo = URLDecoder.decode(url.getUserInfo(), "UTF8");
        URLConnection urlConnection = createConnectionWithAuth(url, userInfo);

        if (urlConnection instanceof HttpURLConnection) {
            boolean redirect = false;
            do {
                redirect = false;
                HttpURLConnection httpConnection =
                    (HttpURLConnection) urlConnection;
                httpConnection.connect();
                int responseCode = httpConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                        || responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                    String location = httpConnection.getHeaderField("Location");
                    if (location == null) {
                        location = url.toString();
                    }
                    urlConnection = createConnectionWithAuth(
                        new URL(location), userInfo);
                    redirect = true;

                }
            } while (redirect);
        }

        // Work out the output filename
        final File output = getTargetFile(url, workingDirectory,
            defaultFilename, urlConnection);

        // Write the file
        copy(urlConnection.getInputStream(), output.toPath());

        return output;
    }

    /**
     * Sets the given connection to trust any host for the purposes of HTTPS.
     * This is wildly unsafe.
     *
     * @param connection
     *            The connection to configure.
     * @throws IOException
     *             If anything goes wrong.
     */
    private static void initVeryTrustingSSLContext(
            HttpsURLConnection connection) throws IOException {
        // Set up to trust everyone
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            TrustManager tm = new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs,
                        String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs,
                        String authType) {
                }
            };
            sc.init(null, new TrustManager[] {tm}, new SecureRandom());

            connection.setSSLSocketFactory(sc.getSocketFactory());
            connection.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        } catch (Exception e) {
            throw new IOException("Error processing HTTPS request", e);
        }
    }

    private static File getTargetFile(final URL url,
            final File workingDirectory, final String defaultFilename,
            final URLConnection urlConnection) throws IOException {
        final String filename = getFileName(
                urlConnection.getHeaderField("Content-Disposition"));
        if (filename != null) {
            return new File(workingDirectory, filename);
        }
        if (defaultFilename != null) {
            return new File(workingDirectory, defaultFilename);
        }
        final String path = url.getPath();
        if (path.isEmpty()) {
            return createTempFile("download", "file", workingDirectory);
        }
        return new File(workingDirectory, new File(path).getName());
    }

    /**
     * Downloads a file from a URL.
     *
     * @param url
     *            The url to download the file from
     * @param workingDirectory
     *            The directory to output the file to
     * @param defaultFilename
     *            The name of the file to use if none can be worked out from the
     *            url or headers, or <tt>null</tt> to use a generated name
     * @return The file downloaded
     * @throws IOException
     *          If anything goes wrong.
     */
    public static File downloadFile(final String url,
            final File workingDirectory, final String defaultFilename)
            throws IOException {
        return downloadFile(new URL(url), workingDirectory, defaultFilename);
    }
}
