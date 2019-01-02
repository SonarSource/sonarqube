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
package org.sonar.server.ce.queue;

import java.util.Map;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeTaskCharacteristicDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.ce.queue.BranchSupport.ComponentKey;

@ServerSide
public interface BranchSupportDelegate {
  /**
   * Creates a {@link ComponentKey} for the specified projectKey and the specified {@code characteristics}.
   *
   * @throws IllegalArgumentException if {@code characteristics} is empty
   * @throws IllegalArgumentException if does not contain a supported value for {@link CeTaskCharacteristicDto#BRANCH_TYPE_KEY BRANCH_TYPE_KEY}
   * @throws IllegalArgumentException if does not contain a value for expected
   *         {@link CeTaskCharacteristicDto#BRANCH_KEY BRANCH_KEY} or {@link CeTaskCharacteristicDto#PULL_REQUEST PULL_REQUEST}
   *         given the value of {@link CeTaskCharacteristicDto#BRANCH_TYPE_KEY BRANCH_TYPE_KEY}
   * @throws IllegalArgumentException if incorrectly contains a value in
   *         {@link CeTaskCharacteristicDto#BRANCH_KEY BRANCH_KEY} or {@link CeTaskCharacteristicDto#PULL_REQUEST PULL_REQUEST}
   *         given the value of {@link CeTaskCharacteristicDto#BRANCH_TYPE_KEY BRANCH_TYPE_KEY}
   */
  ComponentKey createComponentKey(String projectKey, Map<String, String> characteristics);

  /**
   * Creates the ComponentDto for the branch described in {@code componentKey} which belongs to the specified
   * {@code mainComponentDto} in the specified {@code organization}.
   *
   * @throws IllegalArgumentException if arguments are inconsistent (such as {@code mainComponentDto} not having the same
   *         key as {@code componentKey.getKey()}, ...)
   */
  ComponentDto createBranchComponent(DbSession dbSession, ComponentKey componentKey,
    OrganizationDto organization, ComponentDto mainComponentDto, BranchDto mainComponentBranchDto);
}
