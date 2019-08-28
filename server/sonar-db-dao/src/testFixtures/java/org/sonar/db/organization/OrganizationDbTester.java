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
package org.sonar.db.organization;

import java.util.Arrays;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupMembershipDto;
import org.sonar.db.user.GroupMembershipQuery;
import org.sonar.db.user.UserDto;

import static com.google.common.base.Preconditions.checkArgument;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.user.GroupMembershipQuery.IN;

public class OrganizationDbTester {
  private final DbTester db;

  public OrganizationDbTester(DbTester db) {
    this.db = db;
  }

  /**
   * Insert an {@link OrganizationDto} and commit the session
   */
  public OrganizationDto insert() {
    return insert(OrganizationTesting.newOrganizationDto());
  }

  public OrganizationDto insert(Consumer<OrganizationDto> populator) {
    OrganizationDto dto = OrganizationTesting.newOrganizationDto();
    populator.accept(dto);
    return insert(dto);
  }

  public OrganizationDto insertForKey(String key) {
    return insert(dto -> dto.setKey(key));
  }

  public OrganizationDto insertForUuid(String organizationUuid) {
    return insert(dto -> dto.setUuid(organizationUuid));
  }

  /**
   * Insert the provided {@link OrganizationDto} and commit the session
   */
  public OrganizationDto insert(OrganizationDto dto) {
    DbSession dbSession = db.getSession();
    db.getDbClient().organizationDao().insert(dbSession, dto, false);
    dbSession.commit();
    return dto;
  }

  public void setDefaultTemplates(PermissionTemplateDto projectDefaultTemplate, @Nullable PermissionTemplateDto applicationDefaultTemplate,
    @Nullable PermissionTemplateDto portfolioDefaultTemplate) {
    checkArgument(portfolioDefaultTemplate == null
      || portfolioDefaultTemplate.getOrganizationUuid().equals(projectDefaultTemplate.getOrganizationUuid()),
      "default template for project and portfolio must belong to the same organization");
    checkArgument(applicationDefaultTemplate == null
      || applicationDefaultTemplate.getOrganizationUuid().equals(projectDefaultTemplate.getOrganizationUuid()),
      "default template for project and application must belong to the same organization");

    DbSession dbSession = db.getSession();
    db.getDbClient().organizationDao().setDefaultTemplates(dbSession, projectDefaultTemplate.getOrganizationUuid(),
      new DefaultTemplates()
        .setProjectUuid(projectDefaultTemplate.getUuid())
        .setPortfoliosUuid(portfolioDefaultTemplate == null ? null : portfolioDefaultTemplate.getUuid())
        .setApplicationsUuid(applicationDefaultTemplate == null ? null : applicationDefaultTemplate.getUuid()));
    dbSession.commit();
  }

  public void setDefaultTemplates(OrganizationDto defaultOrganization, String projectDefaultTemplateUuid,
    @Nullable String applicationDefaultTemplateUuid, @Nullable String portfoliosDefaultTemplateUuid) {
    DbSession dbSession = db.getSession();
    db.getDbClient().organizationDao().setDefaultTemplates(dbSession, defaultOrganization.getUuid(),
      new DefaultTemplates()
        .setProjectUuid(projectDefaultTemplateUuid)
        .setApplicationsUuid(applicationDefaultTemplateUuid)
        .setPortfoliosUuid(portfoliosDefaultTemplateUuid));
    dbSession.commit();
  }

  public void addMember(OrganizationDto organization, UserDto... users) {
    Arrays.stream(users)
      .forEach(u -> db.getDbClient().organizationMemberDao().insert(db.getSession(), new OrganizationMemberDto().setOrganizationUuid(organization.getUuid()).setUserId(u.getId())));
    db.commit();
  }

  public void setNewProjectPrivate(OrganizationDto organization, boolean newProjectPrivate) {
    db.getDbClient().organizationDao().setNewProjectPrivate(db.getSession(), organization, newProjectPrivate);
    db.commit();
  }

  public boolean getNewProjectPrivate(OrganizationDto organization) {
    return db.getDbClient().organizationDao().getNewProjectPrivate(db.getSession(), organization);
  }

  public void assertUserIsMemberOfOrganization(OrganizationDto organization, UserDto user) {
    assertThat(db.getDbClient().organizationMemberDao().select(db.getSession(), organization.getUuid(), user.getId())).as("User is not member of the organization").isPresent();
    Integer defaultGroupId = db.getDbClient().organizationDao().getDefaultGroupId(db.getSession(), organization.getUuid()).get();
    assertThat(db.getDbClient().groupMembershipDao().selectGroups(
      db.getSession(),
      GroupMembershipQuery.builder().membership(IN).organizationUuid(organization.getUuid()).build(),
      user.getId(), 0, 10))
        .extracting(GroupMembershipDto::getId)
        .as("User is not member of the default group of the organization")
        .containsOnly(defaultGroupId.longValue());
  }

  public void assertUserIsNotMemberOfOrganization(OrganizationDto organization, UserDto user) {
    assertThat(db.getDbClient().organizationMemberDao().select(db.getSession(), organization.getUuid(), user.getId())).as("User is still member of the organization")
      .isNotPresent();
    assertThat(db.getDbClient().groupMembershipDao().countGroups(db.getSession(),
      GroupMembershipQuery.builder().membership(IN).organizationUuid(organization.getUuid()).build(),
      user.getId())).isZero();
  }

}
