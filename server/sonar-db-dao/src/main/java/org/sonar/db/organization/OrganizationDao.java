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
package org.sonar.db.organization;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.qualitygate.QGateWithOrgDto;
import org.sonar.db.user.GroupDto;

import static java.util.Objects.requireNonNull;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class OrganizationDao implements Dao {

  private final System2 system2;

  public OrganizationDao(System2 system2) {
    this.system2 = system2;
  }

  public void insert(DbSession dbSession, OrganizationDto organization, boolean newProjectPrivate) {
    checkDto(organization);
    long now = system2.now();
    organization.setCreatedAt(now);
    organization.setUpdatedAt(now);
    getMapper(dbSession).insert(organization, newProjectPrivate);
  }

  public int countByQuery(DbSession dbSession, OrganizationQuery organizationQuery) {
    requireNonNull(organizationQuery, "organizationQuery can't be null");
    return getMapper(dbSession).countByQuery(organizationQuery);
  }

  public List<OrganizationDto> selectByQuery(DbSession dbSession, OrganizationQuery organizationQuery, Pagination pagination) {
    requireNonNull(organizationQuery, "organizationQuery can't be null");
    return getMapper(dbSession).selectByQuery(organizationQuery, pagination);
  }

  public Optional<OrganizationDto> selectByUuid(DbSession dbSession, String uuid) {
    checkUuid(uuid);
    return Optional.ofNullable(getMapper(dbSession).selectByUuid(uuid));
  }

  public Optional<OrganizationDto> selectByKey(DbSession dbSession, String key) {
    requireNonNull(key, "key can't be null");
    return Optional.ofNullable(getMapper(dbSession).selectByKey(key));
  }

  public List<OrganizationDto> selectByUuids(DbSession dbSession, Set<String> organizationUuids) {
    return executeLargeInputs(organizationUuids, getMapper(dbSession)::selectByUuids);
  }

  public List<OrganizationDto> selectByPermission(DbSession dbSession, Integer userId, String permission) {
    return getMapper(dbSession).selectByPermission(userId, permission);
  }

  public List<String> selectAllUuids(DbSession dbSession) {
    return getMapper(dbSession).selectAllUuids();
  }

  /**
   * Retrieve the default template of the specified organization if:
   * <ol>
   *   <li>the specified organization exists</li>
   *   <li>the project default permission template is defined</li>
   * </ol>
   */
  public Optional<DefaultTemplates> getDefaultTemplates(DbSession dbSession, String organizationUuid) {
    checkUuid(organizationUuid);
    return Optional.ofNullable(getMapper(dbSession).selectDefaultTemplatesByUuid(organizationUuid));
  }

  public void setDefaultTemplates(DbSession dbSession, String uuid, DefaultTemplates defaultTemplates) {
    checkUuid(uuid);
    checkDefaultTemplates(defaultTemplates);
    long now = system2.now();
    getMapper(dbSession).updateDefaultTemplates(uuid, defaultTemplates, now);
  }

  public Optional<Integer> getDefaultGroupId(DbSession dbSession, String organizationUuid) {
    checkUuid(organizationUuid);
    return Optional.ofNullable(getMapper(dbSession).selectDefaultGroupIdByUuid(organizationUuid));
  }

  public void setDefaultGroupId(DbSession dbSession, String uuid, GroupDto defaultGroup) {
    checkUuid(uuid);
    Integer defaultGroupId = requireNonNull(defaultGroup, "Default group cannot be null").getId();
    getMapper(dbSession).updateDefaultGroupId(uuid, requireNonNull(defaultGroupId, "Default group id cannot be null"), system2.now());
  }

  public void setDefaultQualityGate(DbSession dbSession, OrganizationDto organization, QGateWithOrgDto qualityGate) {
    getMapper(dbSession).updateDefaultQualityGate(organization.getUuid(), qualityGate.getUuid(), system2.now());
  }

  public boolean getNewProjectPrivate(DbSession dbSession, OrganizationDto organization) {
    return getMapper(dbSession).selectNewProjectPrivateByUuid(organization.getUuid());
  }

  public void setNewProjectPrivate(DbSession dbSession, OrganizationDto organization, boolean newProjectPrivate) {
    getMapper(dbSession).updateNewProjectPrivate(organization.getUuid(), newProjectPrivate, system2.now());
  }

  public int update(DbSession dbSession, OrganizationDto organization) {
    checkDto(organization);
    organization.setUpdatedAt(system2.now());
    return getMapper(dbSession).update(organization);
  }

  public int deleteByUuid(DbSession dbSession, String uuid) {
    return getMapper(dbSession).deleteByUuid(uuid);
  }

  private static void checkDto(OrganizationDto organization) {
    requireNonNull(organization, "OrganizationDto can't be null");
  }

  private static OrganizationMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(OrganizationMapper.class);
  }

  private static void checkUuid(String uuid) {
    requireNonNull(uuid, "uuid can't be null");
  }

  private static void checkDefaultTemplates(DefaultTemplates defaultTemplates) {
    requireNonNull(defaultTemplates, "defaultTemplates can't be null");
    requireNonNull(defaultTemplates.getProjectUuid());
  }
}
