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
package org.sonar.server.mavendeployer;

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;
import org.sonar.server.platform.ServerSettings;
import org.sonar.server.platform.DefaultServerFileSystem;

import java.io.File;
import java.io.IOException;

public class MavenRepository {

  private final DefaultServerFileSystem installation;
  private final String serverId;
  private File rootDir;

  public MavenRepository(Settings settings, DefaultServerFileSystem fileSystem, Server server) throws IOException {
    this.installation = fileSystem;
    this.serverId = server.getId();
    initRootDir(settings);
  }

  /**
   * for unit tests
   */
  protected MavenRepository(DefaultServerFileSystem installation, String serverId, File rootDir) {
    this.installation = installation;
    this.serverId = serverId;
    this.rootDir = rootDir;
  }

  public void start() {
    try {
      Artifact maven2Plugin = Mojo.createMaven2Plugin(serverId, installation.getMaven2Plugin());
      maven2Plugin.deployTo(rootDir);

    } catch (Exception e) {
      LoggerFactory.getLogger(getClass()).error("Fail to deploy Maven 2 plugin to: " + rootDir, e);
      throw new IllegalStateException("Fail to deploy Maven 2 plugin to: " + rootDir, e);
    }
  }


  private void initRootDir(Settings settings) throws IOException {
    this.rootDir = new File(settings.getString(ServerSettings.DEPLOY_DIR), "maven");
    File orgDir = new File(rootDir, "/org/");
    if (orgDir.exists()) {
      FileUtils.forceDelete(orgDir);
    }
  }
}
