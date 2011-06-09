/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.ServerMetadata;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class ArtifactDownloader implements BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(ArtifactDownloader.class);

  private HttpDownloader httpDownloader;
  private TempDirectories workingDirectories;
  private String baseUrl;

  public ArtifactDownloader(HttpDownloader httpDownloader, TempDirectories workingDirectories, ServerMetadata server) {
    this.httpDownloader = httpDownloader;
    this.workingDirectories = workingDirectories;
    this.baseUrl = server.getURL();
  }

  public File downloadJdbcDriver() {
    String url = baseUrl + "/deploy/jdbc-driver.jar";
    try {
      File jdbcDriver = new File(workingDirectories.getRoot(), "jdbc-driver.jar");
      LOG.info("Download JDBC driver to " + jdbcDriver);
      httpDownloader.download(new URI(url), jdbcDriver);
      return jdbcDriver;

    } catch (URISyntaxException e) {
      throw new SonarException("Fail to download the JDBC driver from : " + url, e);
    }
  }

  public File downloadPlugin(RemotePluginLocation remote) {
    File targetFile = new File(workingDirectories.getDir("plugins/" + remote.getPluginKey()), remote.getFilename());
    String url = baseUrl + "/deploy/plugins/" + remote.getRemotePath();
    try {
      FileUtils.forceMkdir(targetFile.getParentFile());
      LOG.info("Download plugin to " + targetFile);
      httpDownloader.download(new URI(url), targetFile);
      return targetFile;

    } catch (Exception e) {
      throw new SonarException("Fail to download extension: " + url, e);
    }
  }

  public List<RemotePluginLocation> downloadPluginIndex() {
    String url = baseUrl + "/deploy/plugins/index.txt";
    try {
      String indexContent = httpDownloader.downloadPlainText(new URI(url), "UTF-8");
      String[] rows = StringUtils.split(indexContent, CharUtils.LF);
      List<RemotePluginLocation> remoteLocations = Lists.newArrayList();
      for (String row : rows) {
        remoteLocations.add(RemotePluginLocation.createFromRow(row));
      }
      return remoteLocations;

    } catch (Exception e) {
      throw new SonarException("Fail to download plugins index: " + url, e);
    }
  }

  public static final class RemotePluginLocation {
    private String pluginKey;
    private String remotePath;
    private boolean core;

    private RemotePluginLocation(String pluginKey, String remotePath, boolean core) {
      this.pluginKey = pluginKey;
      this.remotePath = remotePath;
      this.core = core;
    }

    static RemotePluginLocation create(String key) {
      return new RemotePluginLocation(key, null, false);
    }

    static RemotePluginLocation createFromRow(String row) {
      String[] fields = StringUtils.split(row, ",");
      return new RemotePluginLocation(fields[0], fields[1], Boolean.parseBoolean(fields[2]));
    }

    public String getPluginKey() {
      return pluginKey;
    }

    public String getRemotePath() {
      return remotePath;
    }

    public String getFilename() {
      return StringUtils.substringAfterLast(remotePath, "/");
    }

    public boolean isCore() {
      return core;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      RemotePluginLocation that = (RemotePluginLocation) o;
      return pluginKey.equals(that.pluginKey);
    }

    @Override
    public int hashCode() {
      return pluginKey.hashCode();
    }
  }
}
