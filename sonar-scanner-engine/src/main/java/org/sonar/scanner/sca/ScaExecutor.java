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
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.internal.apachecommons.lang3.SystemUtils;

/**
 * The ScaExecutor class is the main entrypoint for generating manifest dependency
 * data during a Sonar scan and passing that data in the report so that it can
 * be analyzed further by SQ server.
 */
public class ScaExecutor {
  private static final Logger LOG = LoggerFactory.getLogger(ScaExecutor.class);

  private final CliCacheService cliCacheService;
  private final CliService cliService;

  public ScaExecutor(CliCacheService cliCacheService, CliService cliService) {
    this.cliCacheService = cliCacheService;
    this.cliService = cliService;
  }

  public void execute(DefaultInputModule root) {
    if (cliCacheService.cacheCli(SystemUtils.OS_NAME, SystemUtils.OS_ARCH).exists()) {
      try {
        File generatedZip = cliService.generateManifestsZip(root);
        LOG.debug("Zip ready for report: {}", generatedZip);
      } catch (IOException | IllegalStateException e) {
        LOG.error("Error gathering manifests", e);
      }
    }
  }
}
