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
package org.sonar.db.issue;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.WildcardPosition;
import org.sonar.db.component.ComponentDto;

import static org.sonar.db.DaoDatabaseUtils.buildLikeValue;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class IssueDao implements Dao {

  public Optional<IssueDto> selectByKey(DbSession session, String key) {
    return Optional.ofNullable(mapper(session).selectByKey(key));
  }

  public IssueDto selectOrFailByKey(DbSession session, String key) {
    Optional<IssueDto> issue = selectByKey(session, key);
    if (!issue.isPresent()) {
      throw new RowNotFoundException(String.format("Issue with key '%s' does not exist", key));
    }
    return issue.get();
  }

  /**
   * Gets a list issues by their keys. The result does NOT contain {@code null} values for issues not found, so
   * the size of result may be less than the number of keys. A single issue is returned
   * if input keys contain multiple occurrences of a key.
   * <p>Results may be in a different order as input keys.</p>
   */
  public List<IssueDto> selectByKeys(final DbSession session, Collection<String> keys) {
    return executeLargeInputs(keys, mapper(session)::selectByKeys);
  }

  public Set<String> selectComponentUuidsOfOpenIssuesForProjectUuid(DbSession session, String projectUuid) {
    return mapper(session).selectComponentUuidsOfOpenIssuesForProjectUuid(projectUuid);
  }

  public void scrollNonClosedByComponentUuid(DbSession dbSession, String componentUuid, ResultHandler<IssueDto> handler) {
    mapper(dbSession).scrollNonClosedByComponentUuid(componentUuid, handler);
  }

  public void scrollNonClosedByModuleOrProject(DbSession dbSession, ComponentDto module, ResultHandler<IssueDto> handler) {
    String likeModuleUuidPath = buildLikeValue(module.moduleUuidPath(), WildcardPosition.AFTER);
    mapper(dbSession).scrollNonClosedByModuleOrProject(module.projectUuid(), likeModuleUuidPath, handler);
  }

  public List<ShortBranchIssueDto> selectOpenByComponentUuids(DbSession dbSession, Collection<String> componentUuids) {
    return executeLargeInputs(componentUuids, mapper(dbSession)::selectOpenByComponentUuids);
  }

  public Collection<IssueGroupDto> selectIssueGroupsByBaseComponent(DbSession dbSession, ComponentDto baseComponent, long leakPeriodBeginningDate) {
    return mapper(dbSession).selectIssueGroupsByBaseComponent(baseComponent, leakPeriodBeginningDate);
  }

  public void insert(DbSession session, IssueDto dto) {
    mapper(session).insert(dto);
  }

  public void insert(DbSession session, IssueDto dto, IssueDto... others) {
    IssueMapper mapper = mapper(session);
    mapper.insert(dto);
    for (IssueDto other : others) {
      mapper.insert(other);
    }
  }

  public void update(DbSession session, IssueDto dto) {
    mapper(session).update(dto);
  }

  private static IssueMapper mapper(DbSession session) {
    return session.getMapper(IssueMapper.class);
  }

}
