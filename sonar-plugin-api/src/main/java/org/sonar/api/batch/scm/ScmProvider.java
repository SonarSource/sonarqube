/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.CoreProperties;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.ScannerSide;

/**
 * See {@link CoreProperties#LINKS_SOURCES_DEV} to get old Maven URL format.
 * @since 5.0
 */
@ScannerSide
@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
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
   * @return null if SCM provider was not able to compute the list of files.
   */
  @Nullable
  public Set<Path> branchChangedFiles(String targetBranchName, Path rootBaseDir) {
    return null;
  }

  /**
  * The relative path from SCM root
  */
  public Path relativePathFromScmRoot(Path path) {
    throw new UnsupportedOperationException(formatUnsupportedMessage("Getting relative path from SCM root"));
  }

  /**
   * The current revision id of the analyzed code,
   * for example the SHA1 of the current HEAD in a Git branch.
   */
  public String revisionId(Path path) {
    throw new UnsupportedOperationException(formatUnsupportedMessage("Getting revision id"));
  }

  private String formatUnsupportedMessage(String prefix) {
    return prefix + " is not supported by " + key() + " provider";
  }
}
