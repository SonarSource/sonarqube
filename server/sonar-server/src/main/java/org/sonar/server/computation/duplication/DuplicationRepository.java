/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.duplication;

import java.util.Set;
import org.sonar.server.computation.component.Component;

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
  Set<Duplication> getDuplications(Component file);

  /**
   * Adds a project duplication of the specified original {@link TextBlock} in the specified file {@link Component}
   * which is duplicated by the specified duplicate {@link TextBlock} in the same file.
   * <p>
   * This method can be called multiple times with the same {@code file} and {@code original} but a different
   * {@code duplicate} to add multiple duplications of the same block.
   * </p>
   * <p>
   * It must not, however, be called twice with {@code original} and {@code duplicate} swapped, this would raise
   * an {@link IllegalArgumentException} as the duplication already exists.
   * </p>
   *
   * @throws NullPointerException if any argument is {@code null}
   * @throws IllegalArgumentException if the type of the {@link Component} argument is not {@link Component.Type#FILE}
   * @throws IllegalStateException if {@code original} and {@code duplicate} are the same
   * @throws IllegalStateException if the specified duplication already exists in the repository
   */
  void addDuplication(Component file, TextBlock original, TextBlock duplicate);

  /**
   * Adds a project duplication of the specified original {@link TextBlock} in the specified file {@link Component} as
   * duplicated by the duplicate {@link TextBlock} in the other file {@link Component}.
   * <p>
   * Note: the reverse duplication relationship between files is not added automatically (which leaves open the
   * possibility of inconsistent duplication information). This means that it is the responsibility of the repository's
   * user to call this method again with the {@link Component} arguments and the {@link TextBlock} arguments swapped.
   * </p>
   *
   * @throws NullPointerException if any argument is {@code null}
   * @throws IllegalArgumentException if the type of any of the {@link Component} arguments is not {@link Component.Type#FILE}
   * @throws IllegalArgumentException if {@code file} and {@code otherFile} are the same
   * @throws IllegalStateException if the specified duplication already exists in the repository
   */
  void addDuplication(Component file, TextBlock original, Component otherFile, TextBlock duplicate);

  /**
   * Adds a cross-project duplication of the specified original {@link TextBlock} in the specified file {@link Component},
   * as duplicated by the specified duplicate {@link TextBlock} in a file of another project identified by its key.
   *
   * @throws NullPointerException if any argument is {@code null}
   * @throws IllegalArgumentException if the type of the {@link Component} argument is not {@link Component.Type#FILE}
   * @throws IllegalArgumentException if {@code otherFileKey} is the key of a file in the project
   * @throws IllegalStateException if the specified duplication already exists in the repository
   */
  void addDuplication(Component file, TextBlock original, String otherFileKey, TextBlock duplicate);
}
