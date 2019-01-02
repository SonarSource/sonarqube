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
 package org.sonar.server.es;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.server.es.ProjectIndexer.Cause;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ProjectIndexersImplTest {

  @Test
  public void commitAndIndex_indexes_project() {
    OrganizationDto organization = OrganizationTesting.newOrganizationDto();
    ComponentDto project = ComponentTesting.newPublicProjectDto(organization);

    FakeIndexers underTest = new FakeIndexers();
    underTest.commitAndIndex(mock(DbSession.class), singletonList(project), Cause.PROJECT_CREATION);

    assertThat(underTest.calls).containsExactly(project.uuid());
  }

  @Test
  public void commitAndIndex_of_module_indexes_the_project() {
    OrganizationDto organization = OrganizationTesting.newOrganizationDto();
    ComponentDto project = ComponentTesting.newPublicProjectDto(organization);
    ComponentDto module = ComponentTesting.newModuleDto(project);

    FakeIndexers underTest = new FakeIndexers();
    underTest.commitAndIndex(mock(DbSession.class), singletonList(module), Cause.PROJECT_CREATION);

    assertThat(underTest.calls).containsExactly(project.uuid());
  }

  private static class FakeIndexers implements ProjectIndexers {
    private final List<String> calls = new ArrayList<>();

    @Override
    public void commitAndIndexByProjectUuids(DbSession dbSession, Collection<String> projectUuids, Cause cause) {
      calls.addAll(projectUuids);
    }
  }
}
