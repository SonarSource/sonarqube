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
package org.sonar.scanner.scm;

import java.nio.file.Path;
import java.util.Collection;
import javax.annotation.CheckForNull;
import org.picocontainer.annotations.Nullable;
import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.scm.ScmProvider;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.util.ScannerUtils;

public class ScmChangedFilesProvider extends ProviderAdapter {
  private static final Logger LOG = Loggers.get(ScmChangedFilesProvider.class);
  private static final String LOG_MSG = "SCM collecting changed files in the branch";

  private ScmChangedFiles scmBranchChangedFiles;

  /*
   * ScmConfiguration is not available in issues mode
   */
  public ScmChangedFiles provide(@Nullable ScmConfiguration scmConfiguration, BranchConfiguration branchConfiguration, DefaultInputProject project) {
    if (scmBranchChangedFiles == null) {
      if (scmConfiguration == null) {
        scmBranchChangedFiles = new ScmChangedFiles(null);
      } else {
        Path rootBaseDir = project.getBaseDir();
        Collection<Path> changedFiles = loadChangedFilesIfNeeded(scmConfiguration, branchConfiguration, rootBaseDir);
        validatePaths(changedFiles);
        scmBranchChangedFiles = new ScmChangedFiles(changedFiles);
      }
    }
    return scmBranchChangedFiles;
  }

  private static void validatePaths(@javax.annotation.Nullable Collection<Path> paths) {
    if (paths != null && paths.stream().anyMatch(p -> !p.isAbsolute())) {
      throw new IllegalStateException("SCM provider returned a changed file with a relative path but paths must be absolute. Please fix the provider.");
    }
  }

  @CheckForNull
  private static Collection<Path> loadChangedFilesIfNeeded(ScmConfiguration scmConfiguration, BranchConfiguration branchConfiguration, Path rootBaseDir) {
    String targetScmBranch = branchConfiguration.targetScmBranch();
    if (branchConfiguration.isShortOrPullRequest() && targetScmBranch != null) {
      ScmProvider scmProvider = scmConfiguration.provider();
      if (scmProvider != null) {
        Profiler profiler = Profiler.create(LOG).startInfo(LOG_MSG);
        Collection<Path> changedFiles = scmProvider.branchChangedFiles(targetScmBranch, rootBaseDir);
        profiler.stopInfo();
        if (changedFiles != null) {
          LOG.debug("SCM reported {} {} changed in the branch", changedFiles.size(), ScannerUtils.pluralize("file", changedFiles.size()));
          return changedFiles;
        }
      }

      LOG.debug("SCM information about changed files in the branch is not available");
    }
    return null;
  }

}
