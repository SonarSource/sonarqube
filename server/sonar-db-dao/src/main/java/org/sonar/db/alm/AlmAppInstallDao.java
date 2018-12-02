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

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

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

  public Optional<AlmAppInstallDto> selectByOwnerId(DbSession dbSession, ALM alm, String ownerId) {
    checkAlm(alm);
    checkOwnerId(ownerId);

    AlmAppInstallMapper mapper = getMapper(dbSession);
    return Optional.ofNullable(mapper.selectByOwnerId(alm.getId(), ownerId));
  }

  public Optional<AlmAppInstallDto> selectByInstallationId(DbSession dbSession, ALM alm, String installationId) {
    AlmAppInstallMapper mapper = getMapper(dbSession);
    return Optional.ofNullable(mapper.selectByInstallationId(alm.getId(), installationId));
  }

  public Optional<AlmAppInstallDto> selectByOrganization(DbSession dbSession, ALM alm, OrganizationDto organization) {
    AlmAppInstallMapper mapper = getMapper(dbSession);
    return Optional.ofNullable(mapper.selectByOrganizationUuid(alm.getId(), organization.getUuid()));
  }


  public List<AlmAppInstallDto> selectUnboundByUserExternalId(DbSession dbSession, String userExternalId) {
    return getMapper(dbSession).selectUnboundByUserExternalId(userExternalId);
  }

  /**
   * @param alm Unique identifier of the ALM, like 'bitbucketcloud' or 'github', can't be null
   * @param ownerId ALM specific identifier of the owner of the app, like team or user uuid for Bitbucket Cloud or organization id for Github, can't be null
   * @param installId ALM specific identifier of the app installation, can't be null
   */
  public void insertOrUpdate(DbSession dbSession, ALM alm, String ownerId, @Nullable Boolean isOwnerUser, String installId, @Nullable String userExternalId) {
    checkAlm(alm);
    checkOwnerId(ownerId);
    checkArgument(isNotEmpty(installId), "installId can't be null nor empty");

    AlmAppInstallMapper mapper = getMapper(dbSession);
    long now = system2.now();

    if (mapper.update(alm.getId(), ownerId, isOwnerUser, installId, userExternalId, now) == 0) {
      mapper.insert(uuidFactory.create(), alm.getId(), ownerId, isOwnerUser, installId, userExternalId, now);
    }
  }

  public void delete(DbSession dbSession, ALM alm, String ownerId) {
    checkAlm(alm);
    checkOwnerId(ownerId);

    AlmAppInstallMapper mapper = getMapper(dbSession);
    mapper.delete(alm.getId(), ownerId);
  }

  private static void checkAlm(@Nullable ALM alm) {
    requireNonNull(alm, "alm can't be null");
  }

  private static void checkOwnerId(@Nullable String ownerId) {
    checkArgument(isNotEmpty(ownerId), "ownerId can't be null nor empty");
  }

  private static AlmAppInstallMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(AlmAppInstallMapper.class);
  }
}
