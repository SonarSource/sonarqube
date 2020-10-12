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
package org.sonar.db.organization;

import java.util.Arrays;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;

public class OrganizationDbTester {
  private final DbTester db;

  public OrganizationDbTester(DbTester db) {
    this.db = db;
  }

  public OrganizationDto getDefaultOrganization() {
    return db.getDefaultOrganization();
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

  public void setDefaultTemplates(String projectDefaultTemplateUuid, @Nullable String applicationDefaultTemplateUuid, @Nullable String portfoliosDefaultTemplateUuid) {
    DbSession dbSession = db.getSession();
    db.getDbClient().organizationDao().setDefaultTemplates(dbSession, db.getDefaultOrganization().getUuid(),
      new DefaultTemplates()
        .setProjectUuid(projectDefaultTemplateUuid)
        .setApplicationsUuid(applicationDefaultTemplateUuid)
        .setPortfoliosUuid(portfoliosDefaultTemplateUuid));
    dbSession.commit();
  }

  public void addMember(OrganizationDto organization, UserDto... users) {
    Arrays.stream(users)
      .forEach(
        u -> db.getDbClient().organizationMemberDao().insert(db.getSession(), new OrganizationMemberDto().setOrganizationUuid(organization.getUuid()).setUserUuid(u.getUuid())));
    db.commit();
  }

  public void setNewProjectPrivate(OrganizationDto organization, boolean newProjectPrivate) {
    db.getDbClient().organizationDao().setNewProjectPrivate(db.getSession(), organization, newProjectPrivate);
    db.commit();
  }

  public boolean getNewProjectPrivate(OrganizationDto organization) {
    return db.getDbClient().organizationDao().getNewProjectPrivate(db.getSession(), organization);
  }
}
