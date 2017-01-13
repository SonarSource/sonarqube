/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static java.util.Objects.requireNonNull;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class OrganizationDao implements Dao {

  private final System2 system2;

  public OrganizationDao(System2 system2) {
    this.system2 = system2;
  }

  public void insert(DbSession dbSession, OrganizationDto organization) {
    checkDto(organization);
    long now = system2.now();
    organization.setCreatedAt(now);
    organization.setUpdatedAt(now);
    getMapper(dbSession).insert(organization);
  }

  public List<OrganizationDto> selectByQuery(DbSession dbSession, OrganizationQuery organizationQuery, int offset, int limit) {
    requireNonNull(organizationQuery, "organizationQuery can't be null");
    return getMapper(dbSession).selectByQuery(organizationQuery, offset, limit);
  }

  public Optional<OrganizationDto> selectByUuid(DbSession dbSession, String uuid) {
    requireNonNull(uuid, "uuid can't be null");
    return Optional.ofNullable(getMapper(dbSession).selectByUuid(uuid));
  }

  public Optional<OrganizationDto> selectByKey(DbSession dbSession, String key) {
    requireNonNull(key, "key can't be null");
    return Optional.ofNullable(getMapper(dbSession).selectByKey(key));
  }

  public int update(DbSession dbSession, OrganizationDto organization) {
    checkDto(organization);
    organization.setUpdatedAt(system2.now());
    return getMapper(dbSession).update(organization);
  }

  public int deleteByUuid(DbSession dbSession, String uuid) {
    return getMapper(dbSession).deleteByUuid(uuid);
  }

  public int deleteByKey(DbSession dbSession, String key) {
    return getMapper(dbSession).deleteByKey(key);
  }

  private static void checkDto(OrganizationDto organization) {
    requireNonNull(organization, "OrganizationDto can't be null");
  }

  private static OrganizationMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(OrganizationMapper.class);
  }

  public List<OrganizationDto> selectByUuids(DbSession dbSession, Set<String> organizationUuids) {
    if (organizationUuids.size() == 1) {
      return Collections.singletonList(getMapper(dbSession).selectByUuid(organizationUuids.iterator().next()));
    }
    return executeLargeInputs(organizationUuids, getMapper(dbSession)::selectByUuids);
  }
}
