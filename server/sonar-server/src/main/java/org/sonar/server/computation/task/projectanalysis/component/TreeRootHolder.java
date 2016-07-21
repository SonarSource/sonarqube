/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.task.projectanalysis.component;

public interface TreeRootHolder {
  /**
   * The root of the tree of Component representing the component in the current ScannerReport.
   *
   * @throws IllegalStateException if the holder is empty (ie. there is no root yet)
   */
  Component getRoot();

  /**
   * Return a component by its batch reference
   *
   * @throws IllegalStateException if the holder is empty (ie. there is no root yet)
   * @throws IllegalArgumentException if there's no {@link Component} with the specified reference
   */
  Component getComponentByRef(int ref);

  /**
   * Retrieves the component with the specified key in the {@link Component} tree in the holder.
   *
   * @throws NullPointerException if {@code key} is {@code null}
   * @throws IllegalStateException if the holder is empty (ie. there is not root  yet)
   * @throws IllegalArgumentException if there is no {@link Component} with the specified key in the tree
   */
  Component getComponentByKey(String key);

  /**
   * Checks whether the {@link Component} with the specified key exists in the tree.
   * 
   * @throws NullPointerException if {@code key} is {@code null}
   * @throws IllegalStateException if the holder is empty (ie. there is not root  yet)
   */
  boolean hasComponentWithKey(String key);
}
