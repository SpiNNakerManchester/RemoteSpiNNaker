/*
 * Copyright (c) 2014-2019 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.cs.spinnaker.utils;

import static java.io.File.createTempFile;
import static java.nio.file.Files.copy;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.jboss.resteasy.util.ParameterParser;

/**
 * Utilities for downloading a file.
 */
public abstract class FileDownloader {
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
        if (nonNull(contentDisposition)) {
            final var cdl = contentDisposition.toLowerCase();
            if (cdl.startsWith("form-data") || cdl.startsWith("attachment")) {
                final var parser = new ParameterParser();
                parser.setLowerCaseNames(true);
                final var params = parser.parse(contentDisposition, ';');
                if (params.containsKey("filename")) {
                    return params.get("filename").trim();
                }
            }
        }
        return null;
    }

    /**
     * Create an authenticated connection.
     *
     * @param url The URL to connect to
     * @param userInfo The authentication to use, as username:password
     * @throws IOException if an I/O error occurs
     * @return The created connection
     */
    private static URLConnection createConnectionWithAuth(final URL url,
            final String userInfo) throws IOException {
        var urlConnection = requireNonNull(url).openConnection();
        urlConnection.setDoInput(true);

        if (urlConnection instanceof HttpsURLConnection) {
            initVeryTrustingSSLContext((HttpsURLConnection) urlConnection);
        }

        urlConnection.setRequestProperty("Accept", "*/*");
        if (nonNull(userInfo) && urlConnection instanceof HttpURLConnection) {
            var httpConnection = (HttpURLConnection) urlConnection;
            var basicAuth = "Basic " + Base64.encodeBase64URLSafeString(
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
     *            The URL to download the file from
     * @param workingDirectory
     *            The directory to output the file to
     * @param defaultFilename
     *            The name of the file to use if none can be worked out from the
     *            URL or headers, or {@code null} to use a generated name
     * @return The file downloaded
     * @throws IOException
     *          If anything goes wrong.
     */
    public static File downloadFile(final URL url, final File workingDirectory,
            final String defaultFilename) throws IOException {
        requireNonNull(workingDirectory);

        // Open a connection
        var userInfo = url.getUserInfo();
        if (nonNull(userInfo)) {
            userInfo = URLDecoder.decode(url.getUserInfo(), "UTF8");
        }
        var urlConnection = createConnectionWithAuth(url, userInfo);

        if (urlConnection instanceof HttpURLConnection) {
            boolean redirect = false;
            do {
                redirect = false;
                var httpConnection = (HttpURLConnection) urlConnection;
                httpConnection.connect();
                int responseCode = httpConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                        || responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                    var location = httpConnection.getHeaderField("Location");
                    if (isNull(location)) {
                        location = url.toString();
                    }
                    urlConnection = createConnectionWithAuth(
                        new URL(location), userInfo);
                    redirect = true;

                }
            } while (redirect);
        }

        // Work out the output filename
        final var output = getTargetFile(url, workingDirectory,
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
            var sc = SSLContext.getInstance("SSL");
            var tm = new X509TrustManager() {
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
            };
            sc.init(null, new TrustManager[] {tm}, new SecureRandom());

            connection.setSSLSocketFactory(sc.getSocketFactory());
            connection.setHostnameVerifier((hostname, session) -> true);
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
        final var filename = getFileName(
                urlConnection.getHeaderField("Content-Disposition"));
        if (nonNull(filename)) {
            return new File(workingDirectory, filename);
        }
        if (nonNull(defaultFilename)) {
            return new File(workingDirectory, defaultFilename);
        }
        final var path = url.getPath();
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
     *            URL or headers, or {@code null} to use a generated name
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
