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
package org.sonar.server.issue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.CheckForNull;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.BatchSession;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueChangeMapper;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.issue.index.IssueIndexer;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.sonar.core.util.stream.MoreCollectors.toSet;

/**
 * Save issues into database. It is executed :
 * <ul>
 * <li>once at the end of scan, even on multi-module projects</li>
 * <li>on each server-side action initiated by UI or web service</li>
 * </ul>
 */
@ServerSide
public class WebIssueStorage extends IssueStorage {

  private final System2 system2;
  private final RuleFinder ruleFinder;
  private final DbClient dbClient;
  private final IssueIndexer indexer;

  public WebIssueStorage(System2 system2, DbClient dbClient, RuleFinder ruleFinder, IssueIndexer indexer) {
    this.system2 = system2;
    this.dbClient = dbClient;
    this.ruleFinder = ruleFinder;
    this.indexer = indexer;
  }

  protected DbClient getDbClient() {
    return dbClient;
  }

  public Collection<IssueDto> save(DbSession dbSession, Iterable<DefaultIssue> issues) {
    // Batch session can not be used for updates. It does not return the number of updated rows,
    // required for detecting conflicts.
    long now = system2.now();

    Map<Boolean, List<DefaultIssue>> issuesNewOrUpdated = StreamSupport.stream(issues.spliterator(), true).collect(Collectors.groupingBy(DefaultIssue::isNew));
    List<DefaultIssue> issuesToInsert = firstNonNull(issuesNewOrUpdated.get(true), emptyList());
    List<DefaultIssue> issuesToUpdate = firstNonNull(issuesNewOrUpdated.get(false), emptyList());

    Collection<IssueDto> inserted = insert(dbSession, issuesToInsert, now);
    Collection<IssueDto> updated = update(issuesToUpdate, now);

    doAfterSave(dbSession, Stream.concat(inserted.stream(), updated.stream())
      .collect(toSet(issuesToInsert.size() + issuesToUpdate.size())));

    return Stream.concat(inserted.stream(), updated.stream())
      .collect(toSet(issuesToInsert.size() + issuesToUpdate.size()));
  }

  private void doAfterSave(DbSession dbSession, Collection<IssueDto> issues) {
    indexer.commitAndIndexIssues(dbSession, issues);
  }

  /**
   * @return the keys of the inserted issues
   */
  private Collection<IssueDto> insert(DbSession session, Iterable<DefaultIssue> issuesToInsert, long now) {
    List<IssueDto> inserted = newArrayList();
    int count = 0;
    IssueChangeMapper issueChangeMapper = session.getMapper(IssueChangeMapper.class);
    for (DefaultIssue issue : issuesToInsert) {
      IssueDto issueDto = doInsert(session, now, issue);
      inserted.add(issueDto);
      insertChanges(issueChangeMapper, issue);
      if (count > BatchSession.MAX_BATCH_SIZE) {
        session.commit();
        count = 0;
      }
      count++;
    }
    session.commit();
    return inserted;
  }

  private IssueDto doInsert(DbSession session, long now, DefaultIssue issue) {
    ComponentDto component = component(session, issue);
    ComponentDto project = project(session, issue);
    int ruleId = requireNonNull(getRuleId(issue), "Rule not found: " + issue.ruleKey());
    IssueDto dto = IssueDto.toDtoForServerInsert(issue, component, project, ruleId, now);

    getDbClient().issueDao().insert(session, dto);
    return dto;
  }

  ComponentDto component(DbSession session, DefaultIssue issue) {
    return getDbClient().componentDao().selectOrFailByUuid(session, issue.componentUuid());
  }

  ComponentDto project(DbSession session, DefaultIssue issue) {
    return getDbClient().componentDao().selectOrFailByUuid(session, issue.projectUuid());
  }

  /**
   * @return the keys of the updated issues
   */
  private Collection<IssueDto> update(List<DefaultIssue> issuesToUpdate, long now) {
    Collection<IssueDto> updated = new ArrayList<>();
    if (!issuesToUpdate.isEmpty()) {
      try (DbSession dbSession = dbClient.openSession(false)) {
        IssueChangeMapper issueChangeMapper = dbSession.getMapper(IssueChangeMapper.class);
        for (DefaultIssue issue : issuesToUpdate) {
          IssueDto issueDto = doUpdate(dbSession, now, issue);
          updated.add(issueDto);
          insertChanges(issueChangeMapper, issue);
        }
        dbSession.commit();
      }
    }
    return updated;
  }

  private IssueDto doUpdate(DbSession session, long now, DefaultIssue issue) {
    IssueDto dto = IssueDto.toDtoForUpdate(issue, now);
    getDbClient().issueDao().update(session, dto);
    // Rule id does not exist in DefaultIssue
    Integer ruleId = getRuleId(issue);
    return dto.setRuleId(ruleId);
  }

  @CheckForNull
  protected Integer getRuleId(Issue issue) {
    Rule rule = ruleFinder.findByKey(issue.ruleKey());
    return rule != null ? rule.getId() : null;
  }
}
