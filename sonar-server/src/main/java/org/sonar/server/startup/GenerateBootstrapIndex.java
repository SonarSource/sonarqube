/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.startup;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.home.cache.FileHashes;
import org.sonar.server.platform.DefaultServerFileSystem;

import javax.servlet.ServletContext;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * @since 3.5
 */
public final class GenerateBootstrapIndex {

  // JARs starting with one of these prefixes are excluded from batch
  private static final String[] IGNORE = {"jtds", "mysql", "postgresql", "jruby", "jfreechart", "eastwood",
    "elasticsearch", "lucene"};

  private final ServletContext servletContext;
  private final DefaultServerFileSystem fileSystem;

  public GenerateBootstrapIndex(DefaultServerFileSystem fileSystem, ServletContext servletContext) {
    this.servletContext = servletContext;
    this.fileSystem = fileSystem;
  }

  public void start() throws IOException {
    writeIndex(fileSystem.getBootstrapIndex());
  }

  void writeIndex(File indexFile) throws IOException {
    FileUtils.forceMkdir(indexFile.getParentFile());
    FileWriter writer = new FileWriter(indexFile, false);
    try {
      for (String path : getLibs(servletContext)) {
        writer.append(path);
        InputStream is = servletContext.getResourceAsStream("/WEB-INF/lib/" + path);
        writer.append("|").append(new FileHashes().of(is));
        writer.append(CharUtils.LF);
      }
      writer.flush();

    } finally {
      IOUtils.closeQuietly(writer);
    }
  }

  public static List<String> getLibs(ServletContext servletContext) {
    List<String> libs = Lists.newArrayList();
    Set<String> paths = servletContext.getResourcePaths("/WEB-INF/lib/");
    for (String path : paths) {
      if (StringUtils.endsWith(path, ".jar")) {
        String filename = StringUtils.removeStart(path, "/WEB-INF/lib/");
        if (!isIgnored(filename)) {
          libs.add(filename);
        }
      }
    }
    return libs;
  }

  /**
   * Dirty hack to disable downloading for certain files.
   */
  static boolean isIgnored(String filename) {
    for (String prefix : IGNORE) {
      if (StringUtils.startsWith(filename, prefix)) {
        return true;
      }
    }
    return false;
  }
}
