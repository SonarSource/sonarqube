/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.analysis;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.ce.task.projectanalysis.component.ComponentKeyGenerator;
import org.sonar.db.component.BranchType;

import static org.apache.logging.log4j.util.Strings.trimToNull;
import static org.sonar.core.component.ComponentKeys.createEffectiveKey;

@Immutable
public interface Branch extends ComponentKeyGenerator {

  BranchType getType();

  boolean isMain();

  /**
   * Name of the branch
   */
  String getName();

  /**
   * Indicates the UUID of the branch used as reference
   *
   * @throws IllegalStateException for main branches or legacy branches.
   */
  String getReferenceBranchUuid();

  /**
   * Whether the cross-project duplication tracker can be enabled
   * or not.
   */
  boolean supportsCrossProjectCpd();

  /**
   * @throws IllegalStateException if this branch configuration is not a pull request.
   */
  String getPullRequestKey();

  /**
   * The target/base branch name of a PR.
   * Correspond to <pre>sonar.pullrequest.base</pre> or <pre>sonar.branch.target</pre>
   * It's not guaranteed to exist.
   *
   * @throws IllegalStateException if this branch configuration is not a pull request.
   */
  String getTargetBranchName();

  @Override
  default String generateKey(String projectKey, @Nullable String fileOrDirPath) {
    if (fileOrDirPath == null) {
      return projectKey;
    } else {
      return createEffectiveKey(projectKey, trimToNull(fileOrDirPath));
    }
  }
}
