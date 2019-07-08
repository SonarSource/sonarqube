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
package org.sonar.db.alm;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

/**
 * Store instances of installed app in external ALM like GitHub or Bitbucket Cloud.
 */
public class AlmAppInstallDao implements Dao {

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public AlmAppInstallDao(System2 system2, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  public Optional<AlmAppInstallDto> selectByUuid(DbSession dbSession, String uuid) {
    AlmAppInstallMapper mapper = getMapper(dbSession);
    return Optional.ofNullable(mapper.selectByUuid(uuid));
  }

  public Optional<AlmAppInstallDto> selectByOrganizationAlmId(DbSession dbSession, ALM alm, String organizationAlmId) {
    checkAlm(alm);
    checkOrganizationAlmId(organizationAlmId);

    AlmAppInstallMapper mapper = getMapper(dbSession);
    return Optional.ofNullable(mapper.selectByOrganizationAlmId(alm.getId(), organizationAlmId));
  }

  public Optional<AlmAppInstallDto> selectByInstallationId(DbSession dbSession, ALM alm, String installationId) {
    AlmAppInstallMapper mapper = getMapper(dbSession);
    return Optional.ofNullable(mapper.selectByInstallationId(alm.getId(), installationId));
  }

  public Optional<AlmAppInstallDto> selectByOrganization(DbSession dbSession, OrganizationDto organization) {
    AlmAppInstallMapper mapper = getMapper(dbSession);
    return Optional.ofNullable(mapper.selectByOrganizationUuid(organization.getUuid()));
  }

  public List<AlmAppInstallDto> selectByOrganizations(DbSession dbSession, List<OrganizationDto> organizations) {
    Set<String> organizationUuids = organizations.stream().map(OrganizationDto::getUuid).collect(Collectors.toSet());
    return executeLargeInputs(organizationUuids, uuids -> getMapper(dbSession).selectByOrganizationUuids(organizationUuids));
  }

  public List<AlmAppInstallDto> selectUnboundByUserExternalId(DbSession dbSession, String userExternalId) {
    return getMapper(dbSession).selectUnboundByUserExternalId(userExternalId);
  }

  public void insertOrUpdate(DbSession dbSession, ALM alm, String organizationAlmId, @Nullable Boolean isOwnerUser, String installId, @Nullable String userExternalId) {
    checkAlm(alm);
    checkOrganizationAlmId(organizationAlmId);
    checkArgument(isNotEmpty(installId), "installId can't be null nor empty");

    AlmAppInstallMapper mapper = getMapper(dbSession);
    long now = system2.now();

    if (mapper.update(alm.getId(), organizationAlmId, isOwnerUser, installId, userExternalId, now) == 0) {
      mapper.insert(uuidFactory.create(), alm.getId(), organizationAlmId, isOwnerUser, installId, userExternalId, now);
    }
  }

  public void delete(DbSession dbSession, ALM alm, String organizationAlmId) {
    checkAlm(alm);
    checkOrganizationAlmId(organizationAlmId);

    AlmAppInstallMapper mapper = getMapper(dbSession);
    mapper.delete(alm.getId(), organizationAlmId);
  }

  private static void checkAlm(@Nullable ALM alm) {
    requireNonNull(alm, "alm can't be null");
  }

  private static void checkOrganizationAlmId(@Nullable String organizationAlmId) {
    checkArgument(isNotEmpty(organizationAlmId), "organizationAlmId can't be null nor empty");
  }

  private static AlmAppInstallMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(AlmAppInstallMapper.class);
  }
}
