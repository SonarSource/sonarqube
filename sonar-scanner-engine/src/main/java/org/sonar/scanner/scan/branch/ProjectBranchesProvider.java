/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.scanner.scan.branch;

import com.google.common.collect.ImmutableList;
import org.picocontainer.annotations.Nullable;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.scanner.bootstrap.ProcessedScannerProperties;

public class ProjectBranchesProvider extends ProviderAdapter {

  private static final Logger LOG = Loggers.get(ProjectBranchesProvider.class);
  private static final String LOG_MSG = "Load project branches";

  private ProjectBranches branches = null;

  public ProjectBranches provide(@Nullable ProjectBranchesLoader loader, ProcessedScannerProperties scannerProperties) {
    if (this.branches != null) {
      return this.branches;
    }

    if (loader == null) {
      this.branches = new ProjectBranches(ImmutableList.of());
      return this.branches;
    }

    Profiler profiler = Profiler.create(LOG).startInfo(LOG_MSG);
    this.branches = loader.load(scannerProperties.getProjectKey());
    profiler.stopInfo();
    return this.branches;
  }
}
