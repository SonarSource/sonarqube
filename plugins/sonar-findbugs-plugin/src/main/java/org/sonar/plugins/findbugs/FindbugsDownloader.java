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
package org.sonar.plugins.findbugs;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.BatchExtension;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.SonarException;

public class FindbugsDownloader implements BatchExtension {

  private static final String FINDBUGS_URL = "/deploy/plugins/findbugs";

  private static List<File> libs;

  private HttpDownloader downloader;
  private String host;

  public FindbugsDownloader(Configuration configuration, HttpDownloader downloader) {
    this.downloader = downloader;
    host = StringUtils.chomp(configuration.getString("sonar.host.url", "http://localhost:9000"), "/");
  }

  public synchronized List<File> getLibs() {
    if (libs == null) {
      libs = Arrays.asList(downloadLib(getUrlForAnnotationsJar()), downloadLib(getUrlForJsrJar()));
    }
    return libs;
  }

  /**
   * Visibility has been relaxed to make the code testable.
   */
  protected String getUrlForAnnotationsJar() {
    return host + FINDBUGS_URL + "/annotations-" + FindbugsVersion.getVersion() + ".jar";
  }

  /**
   * Visibility has been relaxed to make the code testable.
   */
  protected String getUrlForJsrJar() {
    return host + FINDBUGS_URL + "/jsr305-" + FindbugsVersion.getVersion() + ".jar";
  }

  protected File downloadLib(String url) {
    try {
      URI uri = new URI(url);
      File temp = File.createTempFile("findbugs", ".jar");
      FileUtils.forceDeleteOnExit(temp);
      downloader.download(uri, temp);
      return temp;
    } catch (URISyntaxException e) {
      throw new SonarException(e);
    } catch (IOException e) {
      throw new SonarException(e);
    }
  }
}
