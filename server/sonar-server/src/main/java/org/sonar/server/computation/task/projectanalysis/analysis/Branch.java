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
package org.sonar.server.computation.task.projectanalysis.analysis;

import java.util.Optional;
import javax.annotation.concurrent.Immutable;
import org.sonar.db.component.BranchType;
import org.sonar.server.computation.task.projectanalysis.component.ComponentKeyGenerator;

@Immutable
public interface Branch extends ComponentKeyGenerator {

  BranchType getType();

  boolean isMain();

  /**
   * Whether branch has been created through the legacy configuration
   * (scanner parameter sonar.branch) or not
   */
  boolean isLegacyFeature();

  /**
   * Name of the branch
   */
  String getName();

  /**
   * Indicates the branch from which it was forked.
   * It will be empty for main branches or legacy branches.
   */
  Optional<String> getMergeBranchUuid();

  /**
   * Whether the cross-project duplication tracker must be enabled
   * or not.
   */
  boolean supportsCrossProjectCpd();
}
