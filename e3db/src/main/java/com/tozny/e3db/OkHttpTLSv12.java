package com.tozny.e3db;

import android.os.Build;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;

public final class OkHttpTLSv12 {
    // From https://github.com/square/okhttp/issues/2372#issuecomment-244807676.
    public static OkHttpClient.Builder enable(OkHttpClient.Builder client) {
        if (Platform.isAndroid() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT < 21) {//Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                // From javadoc for okhttp3.OkHttpClient.Builder.sslSocketFactory(javax.net.ssl.SSLSocketFactory, javax.net.ssl.X509TrustManager)
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init((KeyStore) null);
                TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                    throw new IllegalStateException("Unexpected default trust managers:"
                            + Arrays.toString(trustManagers));
                }
                X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
                SSLContext sc = SSLContext.getInstance(TlsVersion.TLS_1_2.javaName());
                sc.init(null, new TrustManager[] { trustManager }, null);
                client.sslSocketFactory(new ClientImpl.Tls12SocketFactory(sc.getSocketFactory()), trustManager);

                ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2)
                        .build();

                List<ConnectionSpec> specs = Arrays.asList(cs,
                        ConnectionSpec.COMPATIBLE_TLS,
                        ConnectionSpec.CLEARTEXT);
                client.connectionSpecs(specs);
            } catch (KeyStoreException | KeyManagementException | NoSuchAlgorithmException exc) {
                throw new RuntimeException(exc);
            }
        }

        return client;
    }
}
