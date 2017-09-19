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

package org.sonar.api.batch.scm;

import java.nio.file.Path;
import java.util.Collection;
import javax.annotation.Nullable;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.ScannerSide;

/**
 * A {@link ScmProvider} with the capability of finding out which files were changed in a branch.
 * @since 6.6
 */
@ScannerSide
@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
@ExtensionPoint
public abstract class ScmBranchProvider extends ScmProvider {

  /**
   * Return absolute path of files changed in the current branch, compared to the provided target branch.
   * @return null if SCM provider was not able to compute the list of files.
   */
  @Nullable
  public Collection<Path> branchChangedFiles(String targetBranchName, Path rootBaseDir) {
    return null;
  }
}
