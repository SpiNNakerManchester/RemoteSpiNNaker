package uk.ac.manchester.cs.spinnaker.utils;

import static java.io.File.createTempFile;
import static java.nio.file.Files.copy;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jboss.resteasy.util.ParameterParser;

/**
 * A class for downloading files.
 */
public final class FileDownloader {

    /**
     * Stops instantiation.
     */
    private FileDownloader() {

        // Does Nothing
    }

    /**
     * Get the filename from the content disposition header.
     *
     * @param contentDisposition The header
     * @return The filename
     */
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

    /**
     * Downloads a file from a URL.
     *
     * @param url
     *            The URL to download the file from
     * @param workingDirectory
     *            The directory to output the file to
     * @param defaultFilename
     *            The name of the file to use if none can be worked out from the
     *            URL or headers, or <tt>null</tt> to use a generated name
     * @return The file downloaded
     * @throws IOException
     *          If anything goes wrong.
     */
    public static File downloadFile(final URL url, final File workingDirectory,
            final String defaultFilename) throws IOException {
        requireNonNull(workingDirectory);

        // Open a connection
        final URLConnection urlConnection =
                requireNonNull(url).openConnection();
        urlConnection.setDoInput(true);

        if (urlConnection instanceof HttpsURLConnection) {
            initVeryTrustingSSLContext((HttpsURLConnection) urlConnection);
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
            final HttpsURLConnection connection) throws IOException {
        // Set up to trust everyone
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(final X509Certificate[] certs,
                        final String authType) {
                }

                @Override
                public void checkServerTrusted(final X509Certificate[] certs,
                        final String authType) {
                }
            } }, new SecureRandom());

            connection.setSSLSocketFactory(sc.getSocketFactory());
            connection.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(
                        final String hostname, final SSLSession session) {
                    return true;
                }
            });
        } catch (Exception e) {
            throw new IOException("Error processing HTTPS request", e);
        }
    }

    /**
     * Get the file to write to.
     *
     * @param url The URL of the file
     * @param workingDirectory The directory to put the file in
     * @param defaultFilename The default file name if nothing else can be used
     * @param urlConnection The connection where the file has been downloaded
     *     from
     * @return The file to write to.
     * @throws IOException If the file cannot be created
     */
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
     *            The URL to download the file from
     * @param workingDirectory
     *            The directory to output the file to
     * @param defaultFilename
     *            The name of the file to use if none can be worked out from the
     *            URL or headers, or <tt>null</tt> to use a generated name
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
