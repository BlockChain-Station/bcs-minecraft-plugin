package com.enjin.rpc;

import com.enjin.core.Enjin;
import com.enjin.core.config.EnjinConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2Session;
import com.thetransactioncompany.jsonrpc2.client.JSONRPC2SessionOptions;
import lombok.Getter;
import lombok.Setter;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

public class EnjinRPC {
    public static Gson gson = new GsonBuilder().create();

    private static final int READ_TIMEOUT = 15000;
    private static final int CONNECT_TIMEOUT = 15000;

    @Setter
    private static Logger logger;
    private static int nextRequestId = 0;

    private static URL getUrl(String clazz) {
        try {
            EnjinConfig config = Enjin.getConfiguration();
            String apiUrl = config.getApiUrl();
            StringBuilder builder = new StringBuilder(config.isHttps() ? "https" : "http");

            if (apiUrl.startsWith("https")) {
                builder.append(apiUrl.replaceFirst("https", ""));
            } else if (apiUrl.startsWith("http")) {
                builder.append(apiUrl.replaceFirst("http", ""));
            } else {
                builder.append(apiUrl);
            }

            if (!apiUrl.endsWith("/")) {
                builder.append("/");
            }

            builder.append(clazz);

            URL url =  new URL(builder.toString());
            debug("Enjin API URL: " + url.toString());
            return url;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void debug(String s) {
        if (Enjin.getConfiguration().isDebug()) {
            if (logger == null) {
                System.out.println("Enjin Debug: " + (s == null ? "null" : s));
            } else {
                logger.info("Enjin Debug: " + (s == null ? "null" : s));
            }
        }
    }

    private static JSONRPC2SessionOptions getOptions() {
        JSONRPC2SessionOptions options = new JSONRPC2SessionOptions();
        options.setReadTimeout(READ_TIMEOUT);
        options.setConnectTimeout(CONNECT_TIMEOUT);
        options.ignoreVersion(true);
        return options;
    }

    public static JSONRPC2Session getSession(String clazz) {
        URL url = getUrl(clazz);

        if (url == null) {
            debug("Api url is null.");
            return null;
        }

        JSONRPC2Session session = new JSONRPC2Session(url);
        session.setOptions(getOptions());
        return session;
    }

    public static int getNextRequestId() {
        return ++nextRequestId;
    }
}
