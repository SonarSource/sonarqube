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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.Charset;
import java.util.List;

/**
 * This component downloads HTTP files
 *
 * @since 2.2
 */
public class HttpDownloader extends UriReader.SchemeProcessor implements BatchComponent, ServerComponent {

  public static final int TIMEOUT_MILLISECONDS = 20 * 1000;

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

  static String getProxySynthesis(URI uri, ProxySelector proxySelector) {
    List<String> descriptions = Lists.newArrayList();
    List<Proxy> proxies = proxySelector.select(uri);
    if (proxies.size() == 1 && proxies.get(0).type().equals(Proxy.Type.DIRECT)) {
      descriptions.add("no proxy");
    } else {
      for (Proxy proxy : proxies) {
        if (!proxy.type().equals(Proxy.Type.DIRECT)) {
          descriptions.add("proxy: " + proxy.address().toString());
        }
      }
    }
    return Joiner.on(", ").join(descriptions);
  }

  @Override
  String description(URI uri) {
    return String.format("%s (%s)", uri.toString(), getProxySynthesis(uri));
  }

  private void registerProxyCredentials(Settings settings) {
    Authenticator.setDefault(new ProxyAuthenticator(settings.getString("http.proxyUser"), settings
      .getString("http.proxyPassword")));
  }

  private boolean requiresProxyAuthentication(Settings settings) {
    return settings.getString("http.proxyUser") != null;
  }

  private void propagateProxySystemProperties(Settings settings) {
    propagateSystemProperty(settings, "http.proxyHost");
    propagateSystemProperty(settings, "http.proxyPort");
    propagateSystemProperty(settings, "http.nonProxyHosts");
    propagateSystemProperty(settings, "http.auth.ntlm.domain");
    propagateSystemProperty(settings, "socksProxyHost");
    propagateSystemProperty(settings, "socksProxyPort");
  }

  private void propagateSystemProperty(Settings settings, String key) {
    if (settings.getString(key) != null) {
      System.setProperty(key, settings.getString(key));
    }
  }

  public void download(URI uri, File toFile) {
    InputStream input = null;
    FileOutputStream output = null;
    try {
      HttpURLConnection connection = newHttpConnection(uri);
      output = new FileOutputStream(toFile, false);
      input = connection.getInputStream();
      IOUtils.copy(input, output);

    } catch (Exception e) {
      FileUtils.deleteQuietly(toFile);
      throw new SonarException("Fail to download the file: " + uri + " (" + getProxySynthesis(uri) + ")", e);

    } finally {
      IOUtils.closeQuietly(input);
      IOUtils.closeQuietly(output);
    }
  }

  public byte[] download(URI uri) {
    InputStream input = null;
    try {
      HttpURLConnection connection = newHttpConnection(uri);
      input = connection.getInputStream();
      return IOUtils.toByteArray(input);

    } catch (Exception e) {
      throw new SonarException("Fail to download the file: " + uri + " (" + getProxySynthesis(uri) + ")", e);

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  public String downloadPlainText(URI uri, Charset charset) {
    InputStream input = null;
    try {
      HttpURLConnection connection = newHttpConnection(uri);
      input = connection.getInputStream();
      return IOUtils.toString(input, charset.name());

    } catch (Exception e) {
      throw new SonarException("Fail to download the file: " + uri + " (" + getProxySynthesis(uri) + ")", e);

    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  @Override
  String[] getSupportedSchemes() {
    return new String[]{"http", "https"};
  }

  @Override
  byte[] readBytes(URI uri) {
    return download(uri);
  }

  @Override
  String readString(URI uri, Charset charset) {
    return downloadPlainText(uri, charset);
  }

  private HttpURLConnection newHttpConnection(URI uri) throws IOException {
    LoggerFactory.getLogger(getClass()).debug("Download: " + uri + " (" + getProxySynthesis(uri) + ")");
    HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
    connection.setConnectTimeout(TIMEOUT_MILLISECONDS);
    connection.setReadTimeout(TIMEOUT_MILLISECONDS);
    connection.setUseCaches(true);
    connection.setInstanceFollowRedirects(true);
    connection.setRequestProperty("User-Agent", userAgent);
    return connection;
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
