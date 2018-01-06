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
package org.sonar.server.computation.task.projectanalysis.component;

/**
 * The tree of components defined in the scanner report.
 */
public interface TreeRootHolder {
  /**
   * The root of the tree, for example the project or the portfolio.
   * With branches, it will refer to the root component of the branch.
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

}
