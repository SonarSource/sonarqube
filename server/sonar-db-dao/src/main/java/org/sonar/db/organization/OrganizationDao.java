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
package org.sonar.db.organization;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.component.BranchType;
import org.sonar.db.permission.template.DefaultTemplates;
import org.sonar.db.property.InternalPropertiesDao;
import org.sonar.db.user.GroupDto;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeUpdates;

public class OrganizationDao implements Dao {
  /**
   * The UUID of the default organization.
   * Can't be null unless SQ is strongly corrupted.
   */
  public static final String DEFAULT_ORGANIZATION = "organization.default";

  private final System2 system2;
  private final InternalPropertiesDao internalPropertiesDao;

  public OrganizationDao(System2 system2, InternalPropertiesDao internalPropertiesDao) {
    this.system2 = system2;
    this.internalPropertiesDao = internalPropertiesDao;
  }

  public void insert(DbSession dbSession, OrganizationDto organization, boolean newProjectPrivate) {
    checkDto(organization);
    long now = system2.now();
    organization.setCreatedAt(now);
    organization.setUpdatedAt(now);
    getMapper(dbSession).insert(organization, newProjectPrivate);
  }

  // TODO remove after getting rid of organization code
  public OrganizationDto getDefaultOrganization(DbSession dbSession) {
    Optional<String> uuid = internalPropertiesDao.selectByKey(dbSession, DEFAULT_ORGANIZATION);
    checkState(uuid.isPresent() && !uuid.get().isEmpty(), "No Default organization uuid configured");
    return getMapper(dbSession).selectByUuid(uuid.get());
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

  public List<OrganizationDto> selectByPermission(DbSession dbSession, String userUuid, String permission) {
    return getMapper(dbSession).selectByPermission(userUuid, permission);
  }

  public List<OrganizationDto> selectOrgsForUserAndRole(DbSession dbSession, String userUuid, String permission) {
    return getMapper(dbSession).selectOrgsForUserAndRole(userUuid, permission);
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

  public Optional<String> getDefaultGroupUuid(DbSession dbSession, String organizationUuid) {
    checkUuid(organizationUuid);
    return Optional.ofNullable(getMapper(dbSession).selectDefaultGroupUuidByUuid(organizationUuid));
  }

  public void setDefaultGroupUuid(DbSession dbSession, String uuid, GroupDto defaultGroup) {
    checkUuid(uuid);
    String defaultGroupUuid = requireNonNull(defaultGroup, "Default group cannot be null").getUuid();
    getMapper(dbSession).updateDefaultGroupUuid(uuid, requireNonNull(defaultGroupUuid, "Default group uuid cannot be null"), system2.now());
  }

  public boolean getNewProjectPrivate(DbSession dbSession, OrganizationDto organization) {
    return getMapper(dbSession).selectNewProjectPrivateByUuid(organization.getUuid());
  }

  public int update(DbSession dbSession, OrganizationDto organization) {
    checkDto(organization);
    organization.setUpdatedAt(system2.now());
    return getMapper(dbSession).update(organization);
  }

  public int deleteByUuid(DbSession dbSession, String uuid) {
    return getMapper(dbSession).deleteByUuid(uuid);
  }

//  public List<OrganizationWithNclocDto> selectOrganizationsWithNcloc(DbSession dbSession, List<String> organizationUuids) {
//    List<OrganizationWithNclocDto> result = new ArrayList<>();
//    executeLargeUpdates(organizationUuids, chunk -> result.addAll(getMapper(dbSession).selectOrganizationsWithNcloc(NCLOC_KEY, chunk, BranchType.BRANCH)));
//    return result;
//  }

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
