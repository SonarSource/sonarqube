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
package org.sonar.batch.bootstrap;

import org.slf4j.LoggerFactory;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.ServerMetadata;
import org.sonar.core.plugin.JpaPluginFile;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public final class ExtensionDownloader {

  private HttpDownloader httpDownloader;
  private TempDirectories workingDirectories;
  private String baseUrl;

  public ExtensionDownloader(HttpDownloader httpDownloader, TempDirectories workingDirectories, ServerMetadata server) {
    this.httpDownloader = httpDownloader;
    this.workingDirectories = workingDirectories;
    this.baseUrl = server.getURL();
  }

  public File downloadJdbcDriver() {
    String url = baseUrl + "/deploy/jdbc-driver.jar";
    try {
      File jdbcDriver = new File(workingDirectories.getRoot(), "jdbc-driver.jar");
      LoggerFactory.getLogger(getClass()).debug("Download JDBC Driver from " + url + " to " + jdbcDriver.getAbsolutePath());
      httpDownloader.download(new URI(url), jdbcDriver);
      return jdbcDriver;

    } catch (URISyntaxException e) {
      throw new SonarException("Fail to download the JDBC driver from : " + url, e);
    }
  }

  public File downloadExtension(JpaPluginFile extension) {
    File targetFile = new File(workingDirectories.getDir(extension.getPluginKey()), extension.getFilename());
    String url = baseUrl + "/deploy/plugins/" + extension.getPluginKey() + "/" + extension.getFilename();
    LoggerFactory.getLogger(getClass()).debug("Download " + url + " to " + targetFile.getAbsolutePath());
    try {
      httpDownloader.download(new URI(url), targetFile);
      return targetFile;

    } catch (URISyntaxException e) {
      throw new SonarException("Can not download extension: " + url, e);
    }
  }
}
