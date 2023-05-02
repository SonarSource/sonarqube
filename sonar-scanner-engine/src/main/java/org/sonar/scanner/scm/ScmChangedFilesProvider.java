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
package org.sonar.scanner.scm;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.scm.ScmProvider;
import org.sonar.api.impl.utils.ScannerUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scm.git.ChangedFile;
import org.sonar.scm.git.GitScmProvider;
import org.springframework.context.annotation.Bean;

import static java.util.stream.Collectors.toSet;

public class ScmChangedFilesProvider {
  private static final Logger LOG = Loggers.get(ScmChangedFilesProvider.class);
  private static final String LOG_MSG = "SCM collecting changed files in the branch";

  @Bean("ScmChangedFiles")
  public ScmChangedFiles provide(ScmConfiguration scmConfiguration, BranchConfiguration branchConfiguration, DefaultInputProject project) {
    Path rootBaseDir = project.getBaseDir();
    Set<ChangedFile> changedFiles = loadChangedFilesIfNeeded(scmConfiguration, branchConfiguration, rootBaseDir);

    if (changedFiles != null) {
      validatePaths(getAbsoluteFilePaths(changedFiles));
    }

    return new ScmChangedFiles(changedFiles);
  }

  private static void validatePaths(Set<Path> changedFilePaths) {
    if (changedFilePaths.stream().anyMatch(p -> !p.isAbsolute())) {
      throw new IllegalStateException("SCM provider returned a changed file with a relative path but paths must be absolute. Please fix the provider.");
    }
  }

  private static Set<Path> getAbsoluteFilePaths(Collection<ChangedFile> changedFiles) {
    return changedFiles
      .stream()
      .map(ChangedFile::getAbsolutFilePath)
      .collect(toSet());
  }

  @CheckForNull
  private static Set<ChangedFile> loadChangedFilesIfNeeded(ScmConfiguration scmConfiguration, BranchConfiguration branchConfiguration, Path rootBaseDir) {
    final String targetBranchName = branchConfiguration.targetBranchName();
    if (branchConfiguration.isPullRequest() && targetBranchName != null) {
      ScmProvider scmProvider = scmConfiguration.provider();
      if (scmProvider != null) {
        Profiler profiler = Profiler.create(LOG).startInfo(LOG_MSG);
        Set<ChangedFile> changedFiles = getChangedFilesByScm(scmProvider, targetBranchName, rootBaseDir);
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

  private static Set<ChangedFile> getChangedFilesByScm(ScmProvider scmProvider, String targetBranchName, Path rootBaseDir) {
    if (scmProvider instanceof GitScmProvider) {
      return ((GitScmProvider) scmProvider).branchChangedFilesWithFileMovementDetection(targetBranchName, rootBaseDir);
    }

    return toChangedFiles(scmProvider.branchChangedFiles(targetBranchName, rootBaseDir));
  }

  @CheckForNull
  private static Set<ChangedFile> toChangedFiles(@Nullable Set<Path> changedPaths) {
    if (changedPaths == null) {
      return null;
    }

    return changedPaths
      .stream()
      .map(ChangedFile::of)
      .collect(toSet());
  }
}
