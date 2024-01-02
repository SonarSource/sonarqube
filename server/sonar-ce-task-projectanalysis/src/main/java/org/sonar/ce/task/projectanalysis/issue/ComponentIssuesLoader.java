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
package org.sonar.ce.task.projectanalysis.issue;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.utils.System2;
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
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.groupingBy;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.server.issue.IssueFieldsSetter.FROM_BRANCH;
import static org.sonar.server.issue.IssueFieldsSetter.STATUS;

public class ComponentIssuesLoader {
  private static final int DEFAULT_CLOSED_ISSUES_MAX_AGE = 30;
  static final int NUMBER_STATUS_AND_BRANCH_CHANGES_TO_KEEP = 15;
  private static final String PROPERTY_CLOSED_ISSUE_MAX_AGE = "sonar.issuetracking.closedissues.maxage";

  private final DbClient dbClient;
  private final RuleRepository ruleRepository;
  private final ActiveRulesHolder activeRulesHolder;
  private final System2 system2;
  private final int closedIssueMaxAge;
  private final IssueChangesToDeleteRepository issueChangesToDeleteRepository;

  public ComponentIssuesLoader(DbClient dbClient, RuleRepository ruleRepository, ActiveRulesHolder activeRulesHolder,
    Configuration configuration, System2 system2, IssueChangesToDeleteRepository issueChangesToDeleteRepository) {
    this.dbClient = dbClient;
    this.activeRulesHolder = activeRulesHolder;
    this.ruleRepository = ruleRepository;
    this.system2 = system2;
    this.closedIssueMaxAge = configuration.get(PROPERTY_CLOSED_ISSUE_MAX_AGE)
      .map(ComponentIssuesLoader::safelyParseClosedIssueMaxAge)
      .filter(i -> i >= 0)
      .orElse(DEFAULT_CLOSED_ISSUES_MAX_AGE);
    this.issueChangesToDeleteRepository = issueChangesToDeleteRepository;
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

  /**
   * Loads all comments and changes EXCEPT old changes involving a status change or a move between branches.
   */
  public List<DefaultIssue> loadChanges(DbSession dbSession, Collection<DefaultIssue> issues) {
    Map<String, List<IssueChangeDto>> changeDtoByIssueKey = dbClient.issueChangeDao()
      .selectByIssueKeys(dbSession, issues.stream().map(DefaultIssue::key).toList())
      .stream()
      .collect(groupingBy(IssueChangeDto::getIssueKey));

    issues.forEach(i -> setFilteredChanges(changeDtoByIssueKey, i));
    return new ArrayList<>(issues);
  }

  /**
   * Loads the most recent diff changes of the specified issues which contain the latest status and resolution of the issue.
   */
  public void loadLatestDiffChangesForReopeningOfClosedIssues(Collection<DefaultIssue> issues) {
    if (issues.isEmpty()) {
      return;
    }

    try (DbSession dbSession = dbClient.openSession(false)) {
      loadLatestDiffChangesForReopeningOfClosedIssues(dbSession, issues);
    }
  }

  /**
   * Load closed issues for the specified Component, which have at least one line diff in changelog AND are
   * not manual vulnerabilities.
   * <p>
   * Closed issues do not have a line number in DB (it is unset when the issue is closed), this method
   * returns {@link DefaultIssue} objects which line number is populated from the most recent diff logging
   * the removal of the line. Closed issues which do not have such diff are not loaded.
   * <p>
   * To not depend on purge configuration of closed issues, only issues which close date is less than 30 days ago at
   * 00H00 are returned.
   */
  public List<DefaultIssue> loadClosedIssues(String componentUuid) {
    if (closedIssueMaxAge == 0) {
      return emptyList();
    }

    Date date = new Date(system2.now());
    long closeDateAfter = date.toInstant()
      .minus(closedIssueMaxAge, ChronoUnit.DAYS)
      .truncatedTo(ChronoUnit.DAYS)
      .toEpochMilli();
    try (DbSession dbSession = dbClient.openSession(false)) {
      return loadClosedIssues(dbSession, componentUuid, closeDateAfter);
    }
  }

  private static Integer safelyParseClosedIssueMaxAge(String str) {
    try {
      return Integer.parseInt(str);
    } catch (NumberFormatException e) {
      LoggerFactory.getLogger(ComponentIssuesLoader.class)
        .warn("Value of property {} should be an integer >= 0: {}", PROPERTY_CLOSED_ISSUE_MAX_AGE, str);
      return null;
    }
  }

  /**
   * To be efficient both in term of memory and speed:
   * <ul>
   *   <li>only diff changes are loaded from DB, sorted by issue and then change creation date</li>
   *   <li>data from DB is streamed</li>
   *   <li>only the latest change(s) with status and resolution are added to the {@link DefaultIssue} objects</li>
   * </ul>
   *
   * While loading the changes for the issues, this class will also collect old status changes that should be deleted.
   */
  private void loadLatestDiffChangesForReopeningOfClosedIssues(DbSession dbSession, Collection<DefaultIssue> issues) {
    Map<String, DefaultIssue> issuesByKey = issues.stream().collect(Collectors.toMap(DefaultIssue::key, Function.identity()));
    CollectIssueChangesToDeleteResultHandler collectChangesToDelete = new CollectIssueChangesToDeleteResultHandler(issueChangesToDeleteRepository);
    CollectLastStatusAndResolution collectLastStatusAndResolution = new CollectLastStatusAndResolution(issuesByKey);

    dbClient.issueChangeDao().scrollDiffChangesOfIssues(dbSession, issuesByKey.keySet(), resultContext -> {
        IssueChangeDto issueChangeDto = resultContext.getResultObject();
        FieldDiffs fieldDiffs = issueChangeDto.toFieldDiffs();

        collectChangesToDelete.handle(issueChangeDto, fieldDiffs);
        collectLastStatusAndResolution.handle(issueChangeDto, fieldDiffs);
      });
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
      issue.setSelectedAt(System.currentTimeMillis());
      result.add(issue);
    });
    return unmodifiableList(result);
  }

  private void setFilteredChanges(Map<String, List<IssueChangeDto>> changeDtoByIssueKey, DefaultIssue i) {
    List<IssueChangeDto> sortedIssueChanges = changeDtoByIssueKey.computeIfAbsent(i.key(), k -> emptyList()).stream()
      .sorted(Comparator.comparing(IssueChangeDto::getIssueChangeCreationDate).reversed())
      .toList();

    int statusCount = 0;
    int branchCount = 0;

    for (IssueChangeDto c : sortedIssueChanges) {
      switch (c.getChangeType()) {
        case IssueChangeDto.TYPE_FIELD_CHANGE:
          FieldDiffs fieldDiffs = c.toFieldDiffs();
          // To limit the amount of changes that copied issues carry over, we only keep the 15 most recent changes that involve a status change or a move between branches.
          if (fieldDiffs.get(STATUS) != null) {
            statusCount++;
            if (statusCount > NUMBER_STATUS_AND_BRANCH_CHANGES_TO_KEEP) {
              issueChangesToDeleteRepository.add(c.getUuid());
              break;
            }
          }
          if (fieldDiffs.get(FROM_BRANCH) != null) {
            branchCount++;
            if (branchCount > NUMBER_STATUS_AND_BRANCH_CHANGES_TO_KEEP) {
              issueChangesToDeleteRepository.add(c.getUuid());
              break;
            }
          }
          i.addChange(c.toFieldDiffs());
          break;
        case IssueChangeDto.TYPE_COMMENT:
          i.addComment(c.toComment());
          break;
        default:
          throw new IllegalStateException("Unknown change type: " + c.getChangeType());
      }
    }
  }

  private boolean isActive(RuleKey ruleKey) {
    return activeRulesHolder.get(ruleKey).isPresent();
  }

  private static List<DefaultIssue> loadClosedIssues(DbSession dbSession, String componentUuid, long closeDateAfter) {
    ClosedIssuesResultHandler handler = new ClosedIssuesResultHandler();
    dbSession.getMapper(IssueMapper.class).scrollClosedByComponentUuid(componentUuid, closeDateAfter, handler);
    return unmodifiableList(handler.issues);
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
      issue.setSelectedAt(System.currentTimeMillis());

      issues.add(issue);
    }
  }

  private static class CollectLastStatusAndResolution {
    private final Map<String, DefaultIssue> issuesByKey;
    private DefaultIssue currentIssue = null;
    private boolean previousStatusFound = false;
    private boolean previousResolutionFound = false;

    private CollectLastStatusAndResolution(Map<String, DefaultIssue> issuesByKey) {
      this.issuesByKey = issuesByKey;
    }

    /**
     * Assumes that changes are sorted by issue key and date desc
     */
    public void handle(IssueChangeDto issueChangeDto, FieldDiffs fieldDiffs) {
      if (currentIssue == null || !currentIssue.key().equals(issueChangeDto.getIssueKey())) {
        currentIssue = issuesByKey.get(issueChangeDto.getIssueKey());
        previousStatusFound = false;
        previousResolutionFound = false;
      }

      if (currentIssue != null) {
        boolean hasPreviousStatus = fieldDiffs.get("status") != null;
        boolean hasPreviousResolution = fieldDiffs.get("resolution") != null;

        if ((!previousStatusFound && hasPreviousStatus) || (!previousResolutionFound && hasPreviousResolution)) {
          currentIssue.addChange(fieldDiffs);
        }
        previousStatusFound |= hasPreviousStatus;
        previousResolutionFound |= hasPreviousResolution;
      }
    }
  }

  /**
   * Collects issue changes related to status changes that should be cleaned up.
   * If we have more than 15 status changes recorded for an issue, only the 15 most recent ones should be kept.
   */
  private static class CollectIssueChangesToDeleteResultHandler {
    private final IssueChangesToDeleteRepository issueChangesToDeleteRepository;
    private String currentIssueKey;
    private int statusChangeCount;

    public CollectIssueChangesToDeleteResultHandler(IssueChangesToDeleteRepository issueChangesToDeleteRepository) {
      this.issueChangesToDeleteRepository = issueChangesToDeleteRepository;
    }

    /**
     * Assumes that changes are sorted by issue key and date desc
     */
    public void handle(IssueChangeDto dto, FieldDiffs fieldDiffs) {
      if (currentIssueKey == null || !currentIssueKey.equals(dto.getIssueKey())) {
        currentIssueKey = dto.getIssueKey();
        statusChangeCount = 0;
      }
      if (fieldDiffs.get(STATUS) != null) {
        statusChangeCount++;
        if (statusChangeCount > NUMBER_STATUS_AND_BRANCH_CHANGES_TO_KEEP) {
          issueChangesToDeleteRepository.add(dto.getUuid());
        }
      }
    }
  }
}
