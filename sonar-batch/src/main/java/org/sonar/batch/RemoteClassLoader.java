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
package org.sonar.batch;

import org.apache.commons.configuration.Configuration;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

/**
 * Create classloader from remote URLs.
 *
 * IMPORTANT : it generates URLClassLoaders, which use the parent first delegation mode. It finds classes in the parent classloader THEN
 * in the plugin classloader.
 * Using a child-first delegation mode can avoid some conflicts with API dependencies (xml-api, antlr). It's
 * not possible for now, but it would be simple to implement by replacing the URLClassLoader by
 * the class ChildFirstClassLoader (see http://articles.qos.ch/classloader.html)
 */
public class RemoteClassLoader {

  private URLClassLoader classLoader;

  public RemoteClassLoader(URL[] urls, ClassLoader parent) {
    ClassLoader parentClassLoader = (parent==null ? RemoteClassLoader.class.getClassLoader() : parent);
    classLoader = URLClassLoader.newInstance(urls, parentClassLoader);
  }

  public RemoteClassLoader(URL[] urls) {
    this(urls, null);
  }

  public RemoteClassLoader(Collection<URL> urls, ClassLoader parent) {
    this(urls.toArray(new URL[urls.size()]), parent);
  }

  public URLClassLoader getClassLoader() {
    return classLoader;
  }

  public static RemoteClassLoader createForJdbcDriver(Configuration conf) {
    String baseUrl = ServerMetadata.getUrl(conf);
    String url = baseUrl + "/deploy/jdbc-driver.jar";
    try {
      return new RemoteClassLoader(new URL[]{new URL(url)});

    } catch (MalformedURLException e) {
      throw new RuntimeException("Fail to download the JDBC driver from server: " + url, e);
    }
  }
}
