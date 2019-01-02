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
package org.sonar.ce.task.projectanalysis.duplication;

import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.Component;

/**
 * Repository of code duplications in files of the project.
 * <p>
 * It stores:
 * <ul>
 *   <li>inner duplications (ie. duplications of blocks inside the same file)</li>
 *   <li>project duplications (ie. duplications of blocks between two files of the current project)</li>
 *   <li>cross-project duplications (ie. duplications of blocks of code between a file of the current project and a file of another project)</li>
 * </ul>
 * </p>
 */
public interface DuplicationRepository {
  /**
   * Returns the duplications in the specified file {@link Component}, if any.
   *
   * @throws NullPointerException if {@code file} is {@code null}
   * @throws IllegalArgumentException if the type of the {@link Component} argument is not {@link Component.Type#FILE}
   */
  Iterable<Duplication> getDuplications(Component file);

  /**
   * Adds a project duplication in the specified file {@link Component} to the repository.
   *
   * @throws NullPointerException if any argument is {@code null}
   * @throws IllegalArgumentException if the type of the {@link Component} argument is not {@link Component.Type#FILE}
   */
  void add(Component file, Duplication duplication);

}
