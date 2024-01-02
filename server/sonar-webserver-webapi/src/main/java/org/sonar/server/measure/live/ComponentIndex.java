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
package org.sonar.server.measure.live;

import java.util.List;
import java.util.Set;
import org.sonar.db.component.ComponentDto;

/**
 * Provides all components needed for the computation of live measures.
 * The components needed for the computation are:
 * 1) Components for which issues were modified
 * 2) All ancestors of 1), up to the root
 * 3) All immediate children of 1) and 2). The measures in these components won't be recomputed,
 * but their measures are needed to recompute the measures for components in 1) and 2).
 */
public interface ComponentIndex {
  /**
   * Immediate children of a component that are relevant for the computation
   */
  List<ComponentDto> getChildren(ComponentDto component);

  /**
   * Uuids of all components relevant for the computation
   */
  Set<String> getAllUuids();

  /**
   * All components that need the measures recalculated, sorted depth first. It corresponds to the points 1) and 2) in the list mentioned in the javadoc of this class.
   */
  List<ComponentDto> getSortedTree();

  /**
   * Branch being recomputed
   */
  ComponentDto getBranch();
}
