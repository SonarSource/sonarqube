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
package org.sonar.server.mavendeployer;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.sonar.api.platform.Server;
import org.sonar.server.configuration.CoreConfiguration;
import org.sonar.server.platform.DefaultServerFileSystem;

import java.io.File;
import java.io.IOException;

public class MavenRepository {

  private final DefaultServerFileSystem installation;
  private final String serverId;
  private File rootDir;

  public MavenRepository(Configuration configuration, DefaultServerFileSystem fileSystem, Server server) throws IOException {
    this.installation = fileSystem;
    this.serverId = server.getId();
    initRootDir(configuration);
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
      maven2Plugin.deployTo(rootDir, false);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  private void initRootDir(Configuration configuration) throws IOException {
    this.rootDir = new File(configuration.getString(CoreConfiguration.DEPLOY_DIR), "maven");
    File orgDir = new File(rootDir, "/org/");
    if (orgDir.exists()) {
      FileUtils.forceDelete(orgDir);
    }
  }
}
