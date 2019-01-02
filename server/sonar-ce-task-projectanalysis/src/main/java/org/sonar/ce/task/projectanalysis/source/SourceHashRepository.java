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
package org.sonar.ce.task.projectanalysis.source;

import org.sonar.ce.task.projectanalysis.component.Component;

public interface SourceHashRepository {

  /**
   * The hash of the source of the specified FILE component in the analysis report.
   * <p>
   * The source hash will be cached by the repository so that only the first call to this method will cost a file
   * access on disk.
   * </p>
   *
   * @throws NullPointerException if specified component is {@code null}
   * @throws IllegalArgumentException if specified component if not a {@link Component.Type#FILE}
   * @throws IllegalStateException if source hash for the specified component can not be computed
   */
  String getRawSourceHash(Component file);

}
