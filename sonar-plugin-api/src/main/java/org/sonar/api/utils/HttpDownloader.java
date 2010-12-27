/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.platform.Server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.List;

/**
 * This component downloads HTTP files
 * 
 * @since 2.2
 */
public class HttpDownloader implements BatchComponent, ServerComponent {

  public static final int TIMEOUT_MILLISECONDS = 20 * 1000;

  private String userAgent;

  public HttpDownloader(Server server, Configuration configuration) {
    this(configuration, server.getVersion());
  }

  public HttpDownloader(Configuration configuration) {
    this(configuration, null);
  }

  /**
   * Should be package protected for unit tests only, but public is still required for jacoco 0.2.
   */
  public HttpDownloader() {
    this(new PropertiesConfiguration(), null);
  }

  private HttpDownloader(Configuration configuration, String userAgent) {
    initProxy(configuration);
    initUserAgent(userAgent);
  }

  private void initProxy(Configuration configuration) {
    propagateProxySystemProperties(configuration);
    if (requiresProxyAuthentication(configuration)) {
      registerProxyCredentials(configuration);
    }
  }

  private void initUserAgent(String sonarVersion) {
    String userAgent = (sonarVersion == null ? "Sonar" : String.format("Sonar %s", sonarVersion));
    System.setProperty("http.agent", userAgent);
    this.userAgent = userAgent;
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

  private void registerProxyCredentials(Configuration configuration) {
    Authenticator.setDefault(new ProxyAuthenticator(configuration.getString("http.proxyUser"), configuration
        .getString("http.proxyPassword")));
  }

  private boolean requiresProxyAuthentication(Configuration configuration) {
    return configuration.getString("http.proxyUser") != null;
  }

  private void propagateProxySystemProperties(Configuration configuration) {
    propagateSystemProperty(configuration, "http.proxyHost");
    propagateSystemProperty(configuration, "http.proxyPort");
    propagateSystemProperty(configuration, "http.nonProxyHosts");
    propagateSystemProperty(configuration, "http.auth.ntlm.domain");
    propagateSystemProperty(configuration, "socksProxyHost");
    propagateSystemProperty(configuration, "socksProxyPort");
  }

  private void propagateSystemProperty(Configuration configuration, String key) {
    if (configuration.getString(key) != null) {
      System.setProperty(key, configuration.getString(key));
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
      IOUtils.closeQuietly(output);
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

  public InputStream openStream(URI uri) {
    try {
      HttpURLConnection connection = newHttpConnection(uri);
      return connection.getInputStream();

    } catch (Exception e) {
      throw new SonarException("Fail to download the file: " + uri + " (" + getProxySynthesis(uri) + ")", e);
    }
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

  protected PasswordAuthentication getPasswordAuthentication() {
    return auth;
  }
}
