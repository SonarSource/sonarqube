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

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

/**
 * Store instances of installed app in external ALM like GitHub or Bitbucket Cloud.
 */
public class AlmAppInstallDao implements Dao {

  public enum ALM {
    BITBUCKETCLOUD,
    GITHUB;

    String getId() {
      return this.name().toLowerCase(Locale.ENGLISH);
    }
  }

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public AlmAppInstallDao(System2 system2, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  /**
   * @param alm Unique identifier of the ALM, like 'bitbucketcloud' or 'github', can't be null
   * @param ownerId ALM specific identifier of the owner of the app, like team or user uuid for Bitbucket Cloud or organization id for Github, can't be null
   * @param installId ALM specific identifier of the app installation, can't be null
   */
  public void insertOrUpdate(DbSession dbSession, ALM alm, String ownerId, String installId) {
    checkAlm(alm);
    checkOwnerId(ownerId);
    checkArgument(isNotEmpty(installId), "installId can't be null nor empty");

    AlmAppInstallMapper mapper = getMapper(dbSession);
    long now = system2.now();

    if (mapper.update(alm.getId(), ownerId, installId, now) == 0) {
      mapper.insert(uuidFactory.create(), alm.getId(), ownerId, installId, now);
    }
  }

  public Optional<String> getInstallId(DbSession dbSession, ALM alm, String ownerId) {
    checkAlm(alm);
    checkOwnerId(ownerId);

    AlmAppInstallMapper mapper = getMapper(dbSession);
    return Optional.ofNullable(mapper.selectInstallId(alm.getId(), ownerId));
  }

  public void delete(DbSession dbSession, ALM alm, String ownerId) {
    checkAlm(alm);
    checkOwnerId(ownerId);

    AlmAppInstallMapper mapper = getMapper(dbSession);
    mapper.delete(alm.getId(), ownerId);
  }

  private static void checkAlm(@Nullable ALM alm) {
    Objects.requireNonNull(alm, "alm can't be null");
  }

  private static void checkOwnerId(@Nullable String ownerId) {
    checkArgument(isNotEmpty(ownerId), "ownerId can't be null nor empty");
  }

  private static AlmAppInstallMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(AlmAppInstallMapper.class);
  }
}
