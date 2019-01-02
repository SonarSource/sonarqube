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
package org.sonar.db.mapping;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

public class ProjectMappingsDao implements Dao {

  public static final String BITBUCKETCLOUD_REPO_MAPPING = "bitbucketcloud.repo";
  private final System2 system2;
  private final UuidFactory uuidFactory;

  public ProjectMappingsDao(System2 system2, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  public void put(DbSession dbSession, String keyType, String key, String projectUuid) {
    checkKeyType(keyType);
    checkKey(key);
    checkArgument(isNotEmpty(projectUuid), "projectUuid can't be null nor empty");

    ProjectMappingsMapper mapper = getMapper(dbSession);
    mapper.deleteByKey(keyType, key);
    long now = system2.now();
    mapper.put(uuidFactory.create(), keyType, key, projectUuid, now);
  }

  public Optional<ProjectMappingDto> get(DbSession dbSession, String keyType, String key) {
    checkKeyType(keyType);
    checkKey(key);

    ProjectMappingsMapper mapper = getMapper(dbSession);
    return Optional.ofNullable(mapper.selectByKey(keyType, key));
  }

  public void clear(DbSession dbSession, String keyType, String key) {
    checkKeyType(keyType);
    checkKey(key);
    ProjectMappingsMapper mapper = getMapper(dbSession);
    mapper.deleteByKey(keyType, key);
  }

  private static void checkKeyType(@Nullable String keyType) {
    checkArgument(keyType != null && !keyType.isEmpty(), "key type can't be null nor empty");
  }

  private static void checkKey(@Nullable String key) {
    checkArgument(key != null && !key.isEmpty(), "key can't be null nor empty");
  }

  private static ProjectMappingsMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(ProjectMappingsMapper.class);
  }
}
