/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.startup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharUtils;
import org.sonar.api.server.ServerSide;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginRepository;
import org.sonar.core.platform.RemotePlugin;
import org.sonar.server.platform.ServerFileSystem;

@ServerSide
public final class GeneratePluginIndex {

  private final ServerFileSystem fileSystem;
  private final PluginRepository repository;

  public GeneratePluginIndex(ServerFileSystem fileSystem, PluginRepository repository) {
    this.fileSystem = fileSystem;
    this.repository = repository;
  }

  public void start() throws IOException {
    writeIndex(fileSystem.getPluginIndex());
  }

  void writeIndex(File indexFile) throws IOException {
    FileUtils.forceMkdir(indexFile.getParentFile());
    Writer writer = new OutputStreamWriter(new FileOutputStream(indexFile), StandardCharsets.UTF_8);
    try {
      for (PluginInfo pluginInfo : repository.getPluginInfos()) {
        writer.append(RemotePlugin.create(pluginInfo).marshal());
        writer.append(CharUtils.LF);
      }
      writer.flush();

    } finally {
      IOUtils.closeQuietly(writer);
    }
  }
}
