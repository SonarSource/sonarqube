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
package org.sonar.ce.task.projectanalysis.issue;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRulesHolder;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueMapper;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;

public class ComponentIssuesLoader {
  private final DbClient dbClient;
  private final RuleRepository ruleRepository;
  private final ActiveRulesHolder activeRulesHolder;

  public ComponentIssuesLoader(DbClient dbClient, RuleRepository ruleRepository, ActiveRulesHolder activeRulesHolder) {
    this.dbClient = dbClient;
    this.activeRulesHolder = activeRulesHolder;
    this.ruleRepository = ruleRepository;
  }

  public List<DefaultIssue> loadOpenIssues(String componentUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return loadOpenIssues(componentUuid, dbSession);
    }
  }

  public List<DefaultIssue> loadOpenIssuesWithChanges(String componentUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<DefaultIssue> result = loadOpenIssues(componentUuid, dbSession);

      return loadChanges(dbSession, result);
    }
  }

  public void loadChanges(Collection<DefaultIssue> issues) {
    if (issues.isEmpty()) {
      return;
    }

    try (DbSession dbSession = dbClient.openSession(false)) {
      loadChanges(dbSession, issues);
    }
  }

  public List<DefaultIssue> loadChanges(DbSession dbSession, Collection<DefaultIssue> issues) {
    Map<String, List<IssueChangeDto>> changeDtoByIssueKey = dbClient.issueChangeDao()
      .selectByIssueKeys(dbSession, issues.stream().map(DefaultIssue::key).collect(toList()))
      .stream()
      .collect(groupingBy(IssueChangeDto::getIssueKey));

    return issues
      .stream()
      .peek(i -> setChanges(changeDtoByIssueKey, i))
      .collect(toList());
  }

  private List<DefaultIssue> loadOpenIssues(String componentUuid, DbSession dbSession) {
    List<DefaultIssue> result = new ArrayList<>();
    dbSession.getMapper(IssueMapper.class).scrollNonClosedByComponentUuid(componentUuid, resultContext -> {
      DefaultIssue issue = (resultContext.getResultObject()).toDefaultIssue();
      Rule rule = ruleRepository.getByKey(issue.ruleKey());

      // TODO this field should be set outside this class
      if ((!rule.isExternal() && !isActive(issue.ruleKey())) || rule.getStatus() == RuleStatus.REMOVED) {
        issue.setOnDisabledRule(true);
        // TODO to be improved, why setOnDisabledRule(true) is not enough ?
        issue.setBeingClosed(true);
      }
      // FIXME
      issue.setSelectedAt(System.currentTimeMillis());
      result.add(issue);
    });
    return ImmutableList.copyOf(result);
  }

  private static void setChanges(Map<String, List<IssueChangeDto>> changeDtoByIssueKey, DefaultIssue i) {
    changeDtoByIssueKey.computeIfAbsent(i.key(), k -> emptyList()).forEach(c -> {
      switch (c.getChangeType()) {
        case IssueChangeDto.TYPE_FIELD_CHANGE:
          i.addChange(c.toFieldDiffs());
          break;
        case IssueChangeDto.TYPE_COMMENT:
          i.addComment(c.toComment());
          break;
        default:
          throw new IllegalStateException("Unknow change type: " + c.getChangeType());
      }
    });
  }

  private boolean isActive(RuleKey ruleKey) {
    return activeRulesHolder.get(ruleKey).isPresent();
  }

  /**
   * Load closed issues for the specified Component, which have at least one line diff in changelog AND are
   * neither hotspots nor manual vulnerabilities.
   * <p>
   * Closed issues do not have a line number in DB (it is unset when the issue is closed), this method
   * returns {@link DefaultIssue} objects which line number is populated from the most recent diff logging
   * the removal of the line. Closed issues which do not have such diff are not loaded.
   */
  public List<DefaultIssue> loadClosedIssues(String componentUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return loadClosedIssues(componentUuid, dbSession);
    }
  }

  private static List<DefaultIssue> loadClosedIssues(String componentUuid, DbSession dbSession) {
    ClosedIssuesResultHandler handler = new ClosedIssuesResultHandler();
    dbSession.getMapper(IssueMapper.class).scrollClosedByComponentUuid(componentUuid, handler);
    return ImmutableList.copyOf(handler.issues);
  }

  private static class ClosedIssuesResultHandler implements ResultHandler<IssueDto> {
    private final List<DefaultIssue> issues = new ArrayList<>();
    private String previousIssueKey = null;

    @Override
    public void handleResult(ResultContext<? extends IssueDto> resultContext) {
      IssueDto resultObject = resultContext.getResultObject();

      // issue are ordered by most recent change first, only the first row for a given issue is of interest
      if (previousIssueKey != null && previousIssueKey.equals(resultObject.getKey())) {
        return;
      }

      FieldDiffs fieldDiffs = FieldDiffs.parse(resultObject.getClosedChangeData()
        .orElseThrow(() -> new IllegalStateException("Close change data should be populated")));
      checkState(Optional.ofNullable(fieldDiffs.get("status"))
        .map(FieldDiffs.Diff::newValue)
        .filter(STATUS_CLOSED::equals)
        .isPresent(), "Close change data should have a status diff with new value %s", STATUS_CLOSED);
      Integer line = Optional.ofNullable(fieldDiffs.get("line"))
        .map(diff -> (String) diff.oldValue())
        .filter(str -> !str.isEmpty())
        .map(Integer::parseInt)
        .orElse(null);

      previousIssueKey = resultObject.getKey();
      DefaultIssue issue = resultObject.toDefaultIssue();
      issue.setLine(line);
      // FIXME
      issue.setSelectedAt(System.currentTimeMillis());

      issues.add(issue);
    }
  }
}
