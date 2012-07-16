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
package org.sonar.batch.bootstrap;

import com.google.common.base.Charsets;

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
import org.sonar.core.plugins.RemotePlugin;

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
      LOG.debug("Downloading JDBC driver to " + jdbcDriver);
      httpDownloader.download(new URI(url), jdbcDriver);
      return jdbcDriver;

    } catch (URISyntaxException e) {
      throw new SonarException("Fail to download the JDBC driver from : " + url, e);
    }
  }

  public List<File> downloadPlugin(RemotePlugin remote) {
    try {
      File targetDir = workingDirectories.getDir("plugins/" + remote.getKey());
      FileUtils.forceMkdir(targetDir);
      LOG.debug("Downloading plugin " + remote.getKey() + " into " + targetDir);

      List<File> files = Lists.newArrayList();
      for (String filename : remote.getFilenames()) {
        String url = baseUrl + "/deploy/plugins/" + remote.getKey() + "/" + filename;
        File toFile = new File(targetDir, filename);
        httpDownloader.download(new URI(url), toFile);
        files.add(toFile);
      }


      return files;

    } catch (Exception e) {
      throw new SonarException("Fail to download plugin: " + remote.getKey(), e);
    }
  }

  public List<RemotePlugin> downloadPluginIndex() {
    String url = baseUrl + "/deploy/plugins/index.txt";
    try {
      LOG.debug("Downloading index of plugins");
      String indexContent = httpDownloader.downloadPlainText(new URI(url), Charsets.UTF_8);
      String[] rows = StringUtils.split(indexContent, CharUtils.LF);
      List<RemotePlugin> remoteLocations = Lists.newArrayList();
      for (String row : rows) {
        remoteLocations.add(RemotePlugin.unmarshal(row));
      }
      return remoteLocations;

    } catch (Exception e) {
      throw new SonarException("Fail to download plugins index: " + url, e);
    }
  }

}
