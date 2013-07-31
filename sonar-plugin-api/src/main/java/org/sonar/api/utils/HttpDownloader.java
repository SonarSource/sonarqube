/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.utils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * This component downloads HTTP files
 *
 * @since 2.2
 */
public class HttpDownloader extends UriReader.SchemeProcessor implements BatchComponent, ServerComponent {
  public static final int TIMEOUT_MILLISECONDS = 20 * 1000;

  private final BaseHttpDownloader downloader;
  private final Integer readTimeout;

  public HttpDownloader(Server server, Settings settings) {
    this(server, settings, null);
  }

  public HttpDownloader(Server server, Settings settings, @Nullable Integer readTimeout) {
    this.readTimeout = readTimeout;
    downloader = new BaseHttpDownloader(settings.getProperties(), server.getVersion());
  }

  public HttpDownloader(Settings settings) {
    this(settings, null);
  }

  public HttpDownloader(Settings settings, @Nullable Integer readTimeout) {
    this.readTimeout = readTimeout;
    downloader = new BaseHttpDownloader(settings.getProperties(), null);
  }

  @Override
  String description(URI uri) {
    return String.format("%s (%s)", uri.toString(), getProxySynthesis(uri));
  }

  @Override
  String[] getSupportedSchemes() {
    return new String[] {"http", "https"};
  }

  @Override
  byte[] readBytes(URI uri) {
    return download(uri);
  }

  @Override
  String readString(URI uri, Charset charset) {
    try {
      return CharStreams.toString(CharStreams.newReaderSupplier(downloader.newInputSupplier(uri, this.readTimeout), charset));
    } catch (IOException e) {
      throw failToDownload(uri, e);
    }
  }

  public String downloadPlainText(URI uri, String encoding) {
    return readString(uri, Charset.forName(encoding));
  }

  public byte[] download(URI uri) {
    try {
      return ByteStreams.toByteArray(downloader.newInputSupplier(uri, this.readTimeout));
    } catch (IOException e) {
      throw failToDownload(uri, e);
    }
  }

  public String getProxySynthesis(URI uri) {
    return downloader.getProxySynthesis(uri);
  }

  public InputStream openStream(URI uri) {
    try {
      return downloader.newInputSupplier(uri, this.readTimeout).getInput();
    } catch (IOException e) {
      throw failToDownload(uri, e);
    }
  }

  public void download(URI uri, File toFile) {
    try {
      Files.copy(downloader.newInputSupplier(uri, this.readTimeout), toFile);
    } catch (IOException e) {
      FileUtils.deleteQuietly(toFile);
      throw failToDownload(uri, e);
    }
  }

  private SonarException failToDownload(URI uri, IOException e) {
    throw new SonarException(String.format("Fail to download: %s (%s)", uri, getProxySynthesis(uri)), e);
  }

  public static class BaseHttpDownloader {
    private static final List<String> PROXY_SETTINGS = ImmutableList.of(
        "http.proxyHost", "http.proxyPort", "http.nonProxyHosts",
        "http.auth.ntlm.domain", "socksProxyHost", "socksProxyPort");

    private String userAgent;

    public BaseHttpDownloader(Map<String, String> settings, String userAgent) {
      initProxy(settings);
      initUserAgent(userAgent);
    }

    private void initProxy(Map<String, String> settings) {
      propagateProxySystemProperties(settings);
      if (requiresProxyAuthentication(settings)) {
        registerProxyCredentials(settings);
      }
    }

    private void initUserAgent(String sonarVersion) {
      userAgent = (sonarVersion == null ? "Sonar" : String.format("Sonar %s", sonarVersion));
      System.setProperty("http.agent", userAgent);
    }

    private String getProxySynthesis(URI uri) {
      return getProxySynthesis(uri, ProxySelector.getDefault());
    }

    @VisibleForTesting
    static String getProxySynthesis(URI uri, ProxySelector proxySelector) {
      List<Proxy> proxies = proxySelector.select(uri);
      if (proxies.size() == 1 && proxies.get(0).type().equals(Proxy.Type.DIRECT)) {
        return "no proxy";
      }

      List<String> descriptions = Lists.newArrayList();
      for (Proxy proxy : proxies) {
        if (proxy.type() != Proxy.Type.DIRECT) {
          descriptions.add("proxy: " + proxy.address());
        }
      }

      return Joiner.on(", ").join(descriptions);
    }

    private void registerProxyCredentials(Map<String, String> settings) {
      Authenticator.setDefault(new ProxyAuthenticator(
          settings.get("http.proxyUser"),
          settings.get("http.proxyPassword")));
    }

    private boolean requiresProxyAuthentication(Map<String, String> settings) {
      return settings.containsKey("http.proxyUser");
    }

    private void propagateProxySystemProperties(Map<String, String> settings) {
      for (String key : PROXY_SETTINGS) {
        if (settings.containsKey(key)) {
          System.setProperty(key, settings.get(key));
        }
      }
    }

    public InputSupplier<InputStream> newInputSupplier(URI uri) {
      return new HttpInputSupplier(uri, userAgent, null, null, TIMEOUT_MILLISECONDS);
    }

    public InputSupplier<InputStream> newInputSupplier(URI uri, @Nullable Integer readTimeoutMillis) {
      if (readTimeoutMillis != null) {
        return new HttpInputSupplier(uri, userAgent, null, null, readTimeoutMillis);
      }
      return new HttpInputSupplier(uri, userAgent, null, null, TIMEOUT_MILLISECONDS);
    }

    public InputSupplier<InputStream> newInputSupplier(URI uri, String login, String password) {
      return new HttpInputSupplier(uri, userAgent, login, password, TIMEOUT_MILLISECONDS);
    }

    public InputSupplier<InputStream> newInputSupplier(URI uri, String login, String password, @Nullable Integer readTimeoutMillis) {
      if (readTimeoutMillis != null) {
        return new HttpInputSupplier(uri, userAgent, login, password, readTimeoutMillis);
      }
      return new HttpInputSupplier(uri, userAgent, login, password, TIMEOUT_MILLISECONDS);
    }

    private static class HttpInputSupplier implements InputSupplier<InputStream> {
      private final String login;
      private final String password;
      private final URI uri;
      private final String userAgent;
      private final int readTimeoutMillis;

      HttpInputSupplier(URI uri, String userAgent, String login, String password, int readTimeoutMillis) {
        this.uri = uri;
        this.userAgent = userAgent;
        this.login = login;
        this.password = password;
        this.readTimeoutMillis = readTimeoutMillis;
      }

      public InputStream getInput() throws IOException {
        LoggerFactory.getLogger(getClass()).debug("Download: " + uri + " (" + getProxySynthesis(uri, ProxySelector.getDefault()) + ")");

        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        if (!Strings.isNullOrEmpty(login)) {
          String encoded = new String(Base64.encodeBase64((login + ":" + password).getBytes()));
          connection.setRequestProperty("Authorization", "Basic " + encoded);
        }
        connection.setConnectTimeout(TIMEOUT_MILLISECONDS);
        connection.setReadTimeout(readTimeoutMillis);
        connection.setUseCaches(true);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", userAgent);

        int responseCode = connection.getResponseCode();
        if (responseCode >= 400) {
          InputStream errorResponse = null;
          try {
            errorResponse = connection.getErrorStream();
            if (errorResponse != null) {
              String errorResponseContent = IOUtils.toString(errorResponse);
              throw new HttpException(uri, responseCode, errorResponseContent);
            }
            throw new HttpException(uri, responseCode);

          } finally {
            IOUtils.closeQuietly(errorResponse);
          }
        }

        return connection.getInputStream();
      }
    }

    private static class ProxyAuthenticator extends Authenticator {
      private final PasswordAuthentication auth;

      ProxyAuthenticator(String user, String password) {
        auth = new PasswordAuthentication(user, password == null ? new char[0] : password.toCharArray());
      }

      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return auth;
      }
    }
  }

  public static class HttpException extends RuntimeException {
    private final URI uri;
    private final int responseCode;
    private final String responseContent;

    public HttpException(URI uri, int responseContent) {
      this(uri, responseContent, null);
    }

    public HttpException(URI uri, int responseCode, String responseContent) {
      super("Fail to download [" + uri + "]. Response code: " + responseCode);
      this.uri = uri;
      this.responseCode = responseCode;
      this.responseContent = responseContent;
    }

    public int getResponseCode() {
      return responseCode;
    }

    public URI getUri() {
      return uri;
    }

    public String getResponseContent() {
      return responseContent;
    }
  }
}
