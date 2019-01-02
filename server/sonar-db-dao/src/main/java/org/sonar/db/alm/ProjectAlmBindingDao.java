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

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class ProjectAlmBindingDao implements Dao {

  private final System2 system2;
  private final UuidFactory uuidFactory;

  public ProjectAlmBindingDao(System2 system2, UuidFactory uuidFactory) {
    this.system2 = system2;
    this.uuidFactory = uuidFactory;
  }

  public void insertOrUpdate(DbSession dbSession, ALM alm, String repoId, String projectUuid, @Nullable String githubSlug, String url) {
    checkAlm(alm);
    checkRepoId(repoId);
    checkArgument(isNotEmpty(projectUuid), "projectUuid can't be null nor empty");
    checkArgument(isNotEmpty(url), "url can't be null nor empty");

    ProjectAlmBindingMapper mapper = getMapper(dbSession);
    long now = system2.now();

    if (mapper.update(alm.getId(), repoId, projectUuid, githubSlug, url, now) == 0) {
      mapper.insert(uuidFactory.create(), alm.getId(), repoId, projectUuid, githubSlug, url, now);
    }
  }

  public Optional<ProjectAlmBindingDto> selectByProjectUuid(DbSession session, String projectUuid) {
    return Optional.ofNullable(getMapper(session).selectByProjectUuid(projectUuid));
  }

  /**
   * Gets a list of bindings by their repo_id. The result does NOT contain {@code null} values for bindings not found, so
   * the size of result may be less than the number of ids.
   * <p>Results may be in a different order as input ids.</p>
   */
  public List<ProjectAlmBindingDto> selectByRepoIds(final DbSession session, ALM alm, Collection<String> repoIds) {
    return executeLargeInputs(repoIds, partitionedIds -> getMapper(session).selectByRepoIds(alm.getId(), partitionedIds));
  }

  public Optional<ProjectAlmBindingDto> selectByRepoId(final DbSession session, ALM alm, String repoId) {
    return Optional.ofNullable(getMapper(session).selectByRepoId(alm.getId(), repoId));
  }

  public Optional<String> findProjectKey(DbSession dbSession, ALM alm, String repoId) {
    checkAlm(alm);
    checkRepoId(repoId);

    ProjectAlmBindingMapper mapper = getMapper(dbSession);
    return Optional.ofNullable(mapper.selectProjectKey(alm.getId(), repoId));
  }

  private static void checkAlm(@Nullable ALM alm) {
    Objects.requireNonNull(alm, "alm can't be null");
  }

  private static void checkRepoId(@Nullable String repoId) {
    checkArgument(isNotEmpty(repoId), "repoId can't be null nor empty");
  }

  private static ProjectAlmBindingMapper getMapper(DbSession dbSession) {
    return dbSession.getMapper(ProjectAlmBindingMapper.class);
  }

}
