/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;

public class ComponentUuidFactoryImplTest {
  private final Branch mainBranch = new DefaultBranchImpl(DEFAULT_MAIN_BRANCH_NAME);
  private final Branch mockedBranch = mock(Branch.class);

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Test
  public void getOrCreateForKey_when_existingComponentsInDbForMainBranch_should_load() {
    ComponentDto project = db.components().insertPrivateProject();

    ComponentUuidFactory underTest = new ComponentUuidFactoryImpl(db.getDbClient(), db.getSession(), project.getKey(), mainBranch);

    assertThat(underTest.getOrCreateForKey(project.getKey())).isEqualTo(project.uuid());
  }

  @Test
  public void getOrCreateForKey_when_existingComponentsInDbForNonMainBranch_should_load() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project, b -> b.setKey("b1"));
    when(mockedBranch.getType()).thenReturn(BranchType.BRANCH);
    when(mockedBranch.isMain()).thenReturn(false);
    when(mockedBranch.getName()).thenReturn("b1");

    ComponentUuidFactory underTest = new ComponentUuidFactoryImpl(db.getDbClient(), db.getSession(), project.getKey(), mockedBranch);

    assertThat(underTest.getOrCreateForKey(project.getKey())).isEqualTo(branch.uuid());
  }

  @Test
  public void getOrCreateForKey_when_existingComponentsInDbForPr_should_load() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto pr = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST).setKey("pr1"));
    when(mockedBranch.getType()).thenReturn(BranchType.PULL_REQUEST);
    when(mockedBranch.isMain()).thenReturn(false);
    when(mockedBranch.getPullRequestKey()).thenReturn("pr1");

    ComponentUuidFactory underTest = new ComponentUuidFactoryImpl(db.getDbClient(), db.getSession(), project.getKey(), mockedBranch);

    assertThat(underTest.getOrCreateForKey(project.getKey())).isEqualTo(pr.uuid());
  }

  @Test
  public void getOrCreateForKey_when_componentsNotInDb_should_generate() {
    ComponentUuidFactory underTest = new ComponentUuidFactoryImpl(db.getDbClient(), db.getSession(), "theProjectKey", mainBranch);

    String generatedKey = underTest.getOrCreateForKey("foo");
    assertThat(generatedKey).isNotEmpty();

    // uuid is kept in memory for further calls with same key
    assertThat(underTest.getOrCreateForKey("foo")).isEqualTo(generatedKey);
  }
}
