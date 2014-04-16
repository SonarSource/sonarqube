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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.platform.Server;
import org.sonar.home.cache.FileHashes;
import org.sonar.server.platform.DefaultServerFileSystem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @since 3.5
 */
public final class GenerateBootstrapIndex {

  // JARs starting with one of these prefixes are excluded from batch
  private static final String[] IGNORE = {"jtds", "mysql", "postgresql", "jruby", "jfreechart", "eastwood",
    "elasticsearch", "lucene"};

  private static final String LIB_DIR = "/web/WEB-INF/lib";

  private final Server server;
  private final DefaultServerFileSystem fileSystem;

  public GenerateBootstrapIndex(DefaultServerFileSystem fileSystem, Server server) {
    this.server = server;
    this.fileSystem = fileSystem;
  }

  public void start() throws IOException {
    writeIndex(fileSystem.getBootstrapIndex());
  }

  void writeIndex(File indexFile) throws IOException {
    FileUtils.forceMkdir(indexFile.getParentFile());
    FileWriter writer = new FileWriter(indexFile, false);
    try {
      File libDir = new File(server.getRootDir(), LIB_DIR);
      // TODO hack for Medium tests
      if (libDir.exists()) {
        for (String path : getLibs(libDir)) {
          writer.append(path);
          File is = new File(libDir, path);
          writer.append("|").append(new FileHashes().of(is));
          writer.append(CharUtils.LF);
        }
        writer.flush();
      }

    } finally {
      IOUtils.closeQuietly(writer);
    }
  }

  @VisibleForTesting
  static List<String> getLibs(File libDir) {
    List<String> libs = Lists.newArrayList();

    Collection<File> files = FileUtils.listFiles(libDir, HiddenFileFilter.VISIBLE, FileFilterUtils.directoryFileFilter());
    for (File file : files) {
      String path = file.getPath();
      if (StringUtils.endsWith(path, ".jar")) {
        String filename = StringUtils.removeStart(path, libDir.getAbsolutePath() + "/");
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
