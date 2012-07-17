/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.utils;

import com.google.common.annotations.VisibleForTesting;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;

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

/**
 * This component downloads HTTP files
 *
 * @since 2.2
 */
public class HttpDownloader extends UriReader.SchemeProcessor implements BatchComponent, ServerComponent {

  public static final int TIMEOUT_MILLISECONDS = 20 * 1000;
  private static final List<String> PROXY_SETTINGS = ImmutableList.of(
      "http.proxyHost", "http.proxyPort", "http.nonProxyHosts",
      "http.auth.ntlm.domain", "socksProxyHost", "socksProxyPort");

  private String userAgent;

  public HttpDownloader(Server server, Settings settings) {
    this(settings, server.getVersion());
  }

  public HttpDownloader(Settings settings) {
    this(settings, null);
  }

  private HttpDownloader(Settings settings, String userAgent) {
    initProxy(settings);
    initUserAgent(userAgent);
  }

  private void initProxy(Settings settings) {
    propagateProxySystemProperties(settings);
    if (requiresProxyAuthentication(settings)) {
      registerProxyCredentials(settings);
    }
  }

  private void initUserAgent(String sonarVersion) {
    userAgent = (sonarVersion == null ? "Sonar" : String.format("Sonar %s", sonarVersion));
    System.setProperty("http.agent", userAgent);
  }

  public String getProxySynthesis(URI uri) {
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

  private void registerProxyCredentials(Settings settings) {
    Authenticator.setDefault(new ProxyAuthenticator(settings.getString("http.proxyUser"), settings
        .getString("http.proxyPassword")));
  }

  private boolean requiresProxyAuthentication(Settings settings) {
    return settings.getString("http.proxyUser") != null;
  }

  private void propagateProxySystemProperties(Settings settings) {
    for (String key : PROXY_SETTINGS) {
      if (settings.getString(key) != null) {
        System.setProperty(key, settings.getString(key));
      }
    }
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
    try {
      return ByteStreams.toByteArray(new HttpInputSupplier(uri));
    } catch (IOException e) {
      throw failToDownload(uri, e);
    }
  }

  @Override
  String readString(URI uri, Charset charset) {
    try {
      return CharStreams.toString(CharStreams.newReaderSupplier(new HttpInputSupplier(uri), charset));
    } catch (IOException e) {
      throw failToDownload(uri, e);
    }
  }

  public byte[] download(URI uri) {
    return readBytes(uri);
  }

  public String downloadPlainText(URI uri, String encoding) {
    return readString(uri, Charset.forName(encoding));
  }

  public InputStream openStream(URI uri) {
    try {
      return new HttpInputSupplier(uri).getInput();
    } catch (Exception e) {
      throw new SonarException("Fail to download the file: " + uri + " (" + getProxySynthesis(uri) + ")", e);
    }
  }

  public void download(URI uri, File toFile) {
    try {
      Files.copy(new HttpInputSupplier(uri), toFile);
    } catch (IOException e) {
      FileUtils.deleteQuietly(toFile);
      throw failToDownload(uri, e);
    }
  }

  private SonarException failToDownload(URI uri, IOException e) {
    return new SonarException(String.format("Fail to download the file: %s (%s)", uri, getProxySynthesis(uri)), e);
  }

  class HttpInputSupplier implements InputSupplier<InputStream> {
    private final URI uri;

    HttpInputSupplier(URI uri) {
      this.uri = uri;
    }

    public InputStream getInput() throws IOException {
      LoggerFactory.getLogger(getClass()).debug("Download: " + uri + " (" + getProxySynthesis(uri) + ")");

      HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
      connection.setConnectTimeout(TIMEOUT_MILLISECONDS);
      connection.setReadTimeout(TIMEOUT_MILLISECONDS);
      connection.setUseCaches(true);
      connection.setInstanceFollowRedirects(true);
      connection.setRequestProperty("User-Agent", userAgent);
      return connection.getInputStream();
    }
  }
}

class ProxyAuthenticator extends Authenticator {
  private PasswordAuthentication auth;

  ProxyAuthenticator(String user, String password) {
    auth = new PasswordAuthentication(user, password == null ? new char[0] : password.toCharArray());
  }

  @Override
  protected PasswordAuthentication getPasswordAuthentication() {
    return auth;
  }
}
