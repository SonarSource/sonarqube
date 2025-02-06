/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.scanner.sca;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.System2;
import org.sonar.scanner.bootstrap.SonarUserHome;

/**
 * This class is responsible for checking the SQ server for the latest version of the CLI,
 * caching the CLI for use across different projects, updating the cached CLI to the latest
 * version, and holding on to the cached CLI's file location so that other service classes
 * can make use of it.
 */
public class CliCacheService {
  private static final Logger LOG = LoggerFactory.getLogger(CliCacheService.class);
  private final SonarUserHome sonarUserHome;

  public CliCacheService(SonarUserHome sonarUserHome) {
    this.sonarUserHome = sonarUserHome;
  }

  // Right now you need to have the CLI installed in ~/.sonar/cache/tidelift locally to have
  // the zip generation work.
  public File cacheCli(String osName, String arch) {
    LOG.debug("Requesting CLI for OS {} and arch {}", osName, arch);
    return cliFile();
  }

  public File cliFile() {
    return sonarUserHome.getPath().resolve("cache").resolve(fileName()).toFile();
  }

  private static String fileName() {
    return System2.INSTANCE.isOsWindows() ? "tidelift.exe" : "tidelift";
  }
}
