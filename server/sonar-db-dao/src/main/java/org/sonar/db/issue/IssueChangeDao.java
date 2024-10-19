/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static java.util.Collections.singletonList;
import static org.sonar.db.DatabaseUtils.executeLargeInputs;
import static org.sonar.db.DatabaseUtils.executeLargeInputsWithoutOutput;
import static org.sonar.db.DatabaseUtils.executeLargeUpdates;

public class IssueChangeDao implements Dao {

  public List<FieldDiffs> selectChangelogByIssue(DbSession session, String issueKey) {
    return selectByTypeAndIssueKeys(session, singletonList(issueKey), IssueChangeDto.TYPE_FIELD_CHANGE)
      .stream()
      .map(IssueChangeDto::toFieldDiffs)
      .toList();
  }

  public List<IssueChangeDto> selectByTypeAndIssueKeys(DbSession session, Collection<String> issueKeys, String changeType) {
    return executeLargeInputs(issueKeys, issueKeys1 -> mapper(session).selectByIssuesAndType(issueKeys1, changeType));
  }

  public List<IssueChangeDto> selectByIssueKeys(DbSession session, Collection<String> issueKeys) {
    return executeLargeInputs(issueKeys, issueKeys1 -> mapper(session).selectByIssues(issueKeys1));
  }

  public Optional<IssueChangeDto> selectCommentByKey(DbSession session, String commentKey) {
    return Optional.ofNullable(mapper(session).selectByKeyAndType(commentKey, IssueChangeDto.TYPE_COMMENT));
  }

  public void scrollDiffChangesOfIssues(DbSession dbSession, Collection<String> issueKeys, ResultHandler<IssueChangeDto> handler) {
    if (issueKeys.isEmpty()) {
      return;
    }

    executeLargeInputsWithoutOutput(issueKeys, issueKeySubList -> mapper(dbSession).scrollDiffChangesOfIssues(issueKeySubList, handler));
  }

  public void insert(DbSession session, IssueChangeDto change) {
    mapper(session).insert(change);
  }

  public boolean deleteByKey(DbSession session, String key) {
    IssueChangeMapper mapper = mapper(session);
    int count = mapper.delete(key);
    session.commit();
    return count == 1;
  }

  public void deleteByUuids(DbSession session, Set<String> uuids) {
    IssueChangeMapper mapper = mapper(session);
    executeLargeUpdates(uuids, mapper::deleteByUuids);
  }

  public boolean update(DbSession dbSession, IssueChangeDto change) {
    int count = mapper(dbSession).update(change);
    return count == 1;
  }

  private static IssueChangeMapper mapper(DbSession session) {
    return session.getMapper(IssueChangeMapper.class);
  }
}
