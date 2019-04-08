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
package org.sonar.api.batch.scm;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.api.CoreProperties;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.scanner.ScannerSide;

/**
 * @since 5.0
 */
@ScannerSide
@ExtensionPoint
public abstract class ScmProvider {

  /**
   * Unique identifier of the provider. Can be passed to {@link CoreProperties#SCM_PROVIDER_KEY}
   * Can be used in SCM URL to define the provider to use.
   */
  public abstract String key();

  /**
   * Whether this provider is able to manage files located in this directory.
   * Used by autodetection. Not considered if user has forced the provider key.
   *
   * @return false by default
   */
  public boolean supports(File baseDir) {
    return false;
  }

  public BlameCommand blameCommand() {
    throw new UnsupportedOperationException(formatUnsupportedMessage("Blame command"));
  }

  /**
   * Return absolute path of the files changed in the current branch, compared to the provided target branch.
   *
   * @return null if the SCM provider was not able to compute the list of files.
   * @since 7.0
   */
  @CheckForNull
  public Set<Path> branchChangedFiles(String targetBranchName, Path rootBaseDir) {
    return null;
  }

  /**
   * Return a map between paths given as argument and the corresponding line numbers which are new compared to the provided target branch.
   * If null is returned or if a path is not included in the map, an imprecise fallback mechanism will be used to detect which lines
   * are new (based on SCM dates).
   *
   * @param files Absolute path of files of interest
   * @return null if the SCM provider was not able to compute the new lines
   * @since 7.4
   */
  @CheckForNull
  public Map<Path, Set<Integer>> branchChangedLines(String targetBranchName, Path rootBaseDir, Set<Path> files) {
    return null;
  }

  /**
   * The relative path from SCM root
   * @since 7.0
   */
  public Path relativePathFromScmRoot(Path path) {
    throw new UnsupportedOperationException(formatUnsupportedMessage("Getting relative path from SCM root"));
  }

  /**
   * @since 7.7
   */
  public IgnoreCommand ignoreCommand() {
    throw new UnsupportedOperationException(formatUnsupportedMessage("Checking for ignored files"));
  }

  /**
   * The current revision id of the analyzed code,
   * for example the SHA1 of the current HEAD in a Git branch.
   * @since 7.0
   */
  public String revisionId(Path path) {
    throw new UnsupportedOperationException(formatUnsupportedMessage("Getting revision id"));
  }

  private String formatUnsupportedMessage(String prefix) {
    return prefix + " is not supported by " + key() + " provider";
  }
}
