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
package org.sonar.db.alm;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;

import static java.util.Optional.ofNullable;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class OrganizationAlmBindingDao implements Dao {

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public OrganizationAlmBindingDao(System2 system2, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  public Optional<OrganizationAlmBindingDto> selectByOrganization(DbSession dbSession, OrganizationDto organization) {
    return selectByOrganizationUuid(dbSession, organization.getUuid());
  }

  public Optional<OrganizationAlmBindingDto> selectByOrganizationUuid(DbSession dbSession, String organizationUuid) {
    return ofNullable(getMapper(dbSession).selectByOrganizationUuid(organizationUuid));
  }

  public List<OrganizationAlmBindingDto> selectByOrganizations(DbSession dbSession, Collection<OrganizationDto> organizations) {
    return executeLargeInputs(organizations.stream().map(OrganizationDto::getUuid).collect(MoreCollectors.toSet()),
      organizationUuids -> getMapper(dbSession).selectByOrganizationUuids(organizationUuids));
  }

  public Optional<OrganizationAlmBindingDto> selectByAlmAppInstall(DbSession dbSession, AlmAppInstallDto almAppInstall) {
    return ofNullable(getMapper(dbSession).selectByInstallationUuid(almAppInstall.getUuid()));
  }

  public void insert(DbSession dbSession, OrganizationDto organization, AlmAppInstallDto almAppInstall, String url, String userUuid) {
    long now = system2.now();
    getMapper(dbSession).insert(new OrganizationAlmBindingDto()
      .setUuid(uuidFactory.create())
      .setOrganizationUuid(organization.getUuid())
      .setAlmAppInstallUuid(almAppInstall.getUuid())
      .setAlmId(almAppInstall.getAlm())
      .setUrl(url)
      .setUserUuid(userUuid)
      .setCreatedAt(now));
  }

  public void deleteByOrganization(DbSession dbSession, OrganizationDto organization) {
    getMapper(dbSession).deleteByOrganizationUuid(organization.getUuid());
  }

  public void deleteByAlmAppInstall(DbSession dbSession, AlmAppInstallDto almAppInstall) {
    getMapper(dbSession).deleteByAlmAppInstallUuid(almAppInstall.getUuid());
  }

  private static OrganizationAlmBindingMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(OrganizationAlmBindingMapper.class);
  }
}
