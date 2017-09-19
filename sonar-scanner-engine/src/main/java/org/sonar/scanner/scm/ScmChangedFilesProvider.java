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
package org.sonar.scanner.scm;

import java.nio.file.Path;
import java.util.Collection;
import javax.annotation.CheckForNull;
import org.picocontainer.annotations.Nullable;
import org.picocontainer.injectors.ProviderAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.api.batch.scm.ScmBranchProvider;
import org.sonar.api.batch.scm.ScmProvider;
import org.sonar.scanner.scan.branch.BranchConfiguration;

public class ScmChangedFilesProvider extends ProviderAdapter {
  private static final Logger LOG = LoggerFactory.getLogger(ScmChangedFilesProvider.class);

  private ScmChangedFiles scmBranchChangedFiles;

  /*
   * ScmConfiguration is not available in issues mode
   */
  public ScmChangedFiles provide(@Nullable ScmConfiguration scmConfiguration, BranchConfiguration branchConfiguration, InputModuleHierarchy inputModuleHierarchy) {
    if (scmBranchChangedFiles == null) {
      if (scmConfiguration == null) {
        scmBranchChangedFiles = new ScmChangedFiles(null);
      } else {
        Path rootBaseDir = inputModuleHierarchy.root().getBaseDir();
        Collection<Path> changedFiles = loadChangedFilesIfNeeded(scmConfiguration, branchConfiguration, rootBaseDir);
        scmBranchChangedFiles = new ScmChangedFiles(changedFiles);
      }
    }
    return scmBranchChangedFiles;
  }

  @CheckForNull
  private static Collection<Path> loadChangedFilesIfNeeded(ScmConfiguration scmConfiguration, BranchConfiguration branchConfiguration, Path rootBaseDir) {
    if (branchConfiguration.isShortLivingBranch()) {
      ScmProvider scmProvider = scmConfiguration.provider();
      if (scmProvider != null && (scmProvider instanceof ScmBranchProvider)) {
        ScmBranchProvider scmBranchProvider = (ScmBranchProvider) scmProvider;
        Collection<Path> changedFiles = scmBranchProvider.branchChangedFiles(branchConfiguration.branchTarget(), rootBaseDir);
        if (changedFiles != null) {
          LOG.debug("SCM reported {} files changed in the branch", changedFiles.size());
          return changedFiles;
        }
      }

      LOG.debug("SCM information about changed files in the branch is not available");
    }
    return null;
  }

}
