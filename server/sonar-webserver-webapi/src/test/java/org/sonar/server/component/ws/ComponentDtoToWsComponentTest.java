/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.component.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.project.ProjectDto;

import static org.sonar.server.component.ws.ComponentDtoToWsComponent.componentDtoToWsComponent;
import static org.sonar.server.component.ws.ComponentDtoToWsComponent.projectOrAppToWsComponent;

public class ComponentDtoToWsComponentTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void componentDtoToWsComponent_throws_IAE_if_organization_uuid_of_component_does_not_match_organizationDto_uuid() {
    OrganizationDto organizationDto1 = OrganizationTesting.newOrganizationDto();
    OrganizationDto organizationDto2 = OrganizationTesting.newOrganizationDto();

    ProjectDto parentProjectDto = ComponentTesting.createPrivateProjectDto(organizationDto1);
    ComponentDto componentDto = ComponentTesting.newBranchComponent(parentProjectDto,
      ComponentTesting.newBranchDto(parentProjectDto.getUuid(), BranchType.BRANCH));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("OrganizationUuid (" + organizationDto1.getUuid() + ") of ComponentDto to convert " +
      "to Ws Component is not the same as the one (" + organizationDto2.getUuid() + ") of the specified OrganizationDto");

    componentDtoToWsComponent(componentDto, parentProjectDto, organizationDto2, null);
  }

  @Test
  public void projectOrAppToWsComponent_throws_IAE_if_organization_uuid_of_component_does_not_match_organizationDto_uuid() {
    OrganizationDto organizationDto1 = OrganizationTesting.newOrganizationDto();
    OrganizationDto organizationDto2 = OrganizationTesting.newOrganizationDto();
    ProjectDto projectDto = ComponentTesting.createPrivateProjectDto(organizationDto1);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("OrganizationUuid (" + organizationDto1.getUuid() + ") of ComponentDto to convert " +
      "to Ws Component is not the same as the one (" + organizationDto2.getUuid() + ") of the specified OrganizationDto");

    projectOrAppToWsComponent(projectDto, organizationDto2, null);
  }

}
