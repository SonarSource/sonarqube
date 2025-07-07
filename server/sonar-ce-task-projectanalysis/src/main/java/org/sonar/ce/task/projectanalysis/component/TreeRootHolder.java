/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.component;

import java.util.Optional;

/**
 * The tree of components defined in the scanner report.
 */
public interface TreeRootHolder {
  /**
   * @return true if the holder is empty
   */
  boolean isEmpty();

  /**
   * The root of the tree, for example the project or the portfolio.
   * With branches, it will refer to the root component of the branch.
   *
   * On a pull request, it contains ONLY:
   * 1. The PROJECT component (tree root)
   * 2. The FILE components whose status is not SAME
   * 3. Intermediary MODULE and DIRECTORY components that lead to FILE leafs that are not SAME
   *
   * @throws IllegalStateException if the holder is empty (ie. there is no root yet)
   */
  Component getRoot();

  /**
   * The root of the components that were in the scanner report.
   * This tree may include components that are not persisted,
   * just kept in memory for computation purposes, such as overall coverage
   * in pull requests.
   *
   * @throws IllegalStateException if the holder is empty (ie. there is no root yet)
   */
  Component getReportTreeRoot();

  /**
   * Return a component by its batch reference
   *
   * @throws IllegalStateException if the holder is empty (ie. there is no root yet)
   * @throws IllegalArgumentException if there's no {@link Component} with the specified reference
   */
  Component getComponentByRef(int ref);

  /**
   * Return a component by its uuid
   *
   * @throws IllegalStateException if the holder is empty (ie. there is no root yet)
   * @throws IllegalArgumentException if there's no {@link Component} with the specified reference
   */
  Component getComponentByUuid(String uuid);

  /**
   * Return a component by its scanner reference. Returns {@link Optional#empty()} if there's
   * no {@link Component} with the specified reference. Note that on PRs, unchanged components are not indexed.
   *
   * @throws IllegalStateException if the holder is empty (ie. there is no root yet)
   */
  Optional<Component> getOptionalComponentByRef(int ref);

  /**
   * Return a component from the report tree (see {@link #getReportTreeRoot()}, by its batch reference.
   *
   * @throws IllegalStateException if the holder is empty (ie. there is no root yet)
   * @throws IllegalArgumentException if there's no {@link Component} with the specified reference
   */
  Component getReportTreeComponentByRef(int ref);

  /**
   * Number of components, including root.
   *
   * @throws IllegalStateException if the holder is empty (ie. there is no root yet)
   */
  int getSize();
}
