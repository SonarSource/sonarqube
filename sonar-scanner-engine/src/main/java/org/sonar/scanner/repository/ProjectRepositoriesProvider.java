/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.repository;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.scanner.bootstrap.ScannerProperties;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.springframework.context.annotation.Bean;

public class ProjectRepositoriesProvider {
  private static final Logger LOG = Loggers.get(ProjectRepositoriesProvider.class);
  private static final String LOG_MSG = "Load project repositories";
  private final ProjectRepositoriesLoader loader;
  private final ScannerProperties scannerProperties;
  private final BranchConfiguration branchConfig;

  public ProjectRepositoriesProvider(ProjectRepositoriesLoader loader, ScannerProperties scannerProperties, BranchConfiguration branchConfig) {
    this.loader = loader;
    this.scannerProperties = scannerProperties;
    this.branchConfig = branchConfig;
  }

  @Bean
  public ProjectRepositories projectRepositories() {
    Profiler profiler = Profiler.create(LOG).startInfo(LOG_MSG);
    ProjectRepositories project = loader.load(scannerProperties.getProjectKey(), branchConfig.referenceBranchName());
    profiler.stopInfo();
    return project;
  }
}
