package tech.ydb.core.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class YandexTrustManagersProvider {
    private static final Logger logger = LoggerFactory.getLogger(YandexTrustManagerFactory.class);

    private static final String CA_STORE = "certificates/YandexAllCAs.pkcs";
    private static final String CA_KEYPHRASE = "certificates/YandexAllCAs.password";

    private final TrustManager[] trustManagers;

    private YandexTrustManagersProvider() {
        try {
            List<TrustManager> customTrustManagers = getCustomTrustManagers();
            List<TrustManager> defaultTrustManagers = getDefaultTrustManagers();

            List<X509TrustManager> x509TrustManagers = Stream
                    .concat(customTrustManagers.stream(), defaultTrustManagers.stream())
                    .filter(X509TrustManager.class::isInstance)
                    .map(X509TrustManager.class::cast)
                    .collect(Collectors.toList());
            List<TrustManager> allTrustManagers = Stream
                    .concat(customTrustManagers.stream(), defaultTrustManagers.stream())
                    .filter(x -> !(x instanceof X509TrustManager))
                    .collect(Collectors.toCollection(ArrayList::new));
            X509TrustManager composite = new MultiX509TrustManager(x509TrustManagers);
            allTrustManagers.add(composite);
            trustManagers = allTrustManagers.toArray(new TrustManager[0]);
        } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException e) {
            String msg = "Can't init yandex root CA setting";
            logger.debug(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    private List<TrustManager> getDefaultTrustManagers() throws NoSuchAlgorithmException, KeyStoreException {
        return getTrustManagersFromKeyStore(null);
    }

    private List<TrustManager> getCustomTrustManagers() throws KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream pis = YandexTrustManagersProvider.class.getClassLoader().getResourceAsStream(CA_KEYPHRASE)) {
            String passPhrase = new String(ByteStreams.toByteArray(pis), StandardCharsets.UTF_8);
            try (InputStream is = YandexTrustManagersProvider.class.getClassLoader().getResourceAsStream(CA_STORE)) {
                keyStore.load(is, passPhrase.toCharArray());
            }
        }
        return getTrustManagersFromKeyStore(keyStore);
    }

    private List<TrustManager> getTrustManagersFromKeyStore(KeyStore keyStore)
            throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        return Arrays.asList(trustManagerFactory.getTrustManagers());
    }

    private static final class LazyHolder {
        private static final YandexTrustManagersProvider INSTANCE = new YandexTrustManagersProvider();
    }

    public static YandexTrustManagersProvider getInstance() {
        return LazyHolder.INSTANCE;
    }

    public TrustManager[] getTrustManagers() {
        return trustManagers.clone();
    }

}
