/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.cursor.Cursor;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;
import org.sonar.db.RowNotFoundException;
import org.sonar.db.component.ComponentDto;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

public class IssueDao implements Dao {
  public static final int DEFAULT_PAGE_SIZE = 1000;
  public static final int BIG_PAGE_SIZE = 1000000;

  public Optional<IssueDto> selectByKey(DbSession session, String key) {
    return Optional.ofNullable(mapper(session).selectByKey(key));
  }

  public IssueDto selectOrFailByKey(DbSession session, String key) {
    Optional<IssueDto> issue = selectByKey(session, key);
    if (issue.isEmpty()) {
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
  public List<IssueDto> selectByKeys(DbSession session, Collection<String> keys) {
    return executeLargeInputs(keys, mapper(session)::selectByKeys);
  }

  public Set<String> selectIssueKeysByComponentUuid(DbSession session, String componentUuid) {
    return mapper(session).selectIssueKeysByComponentUuid(componentUuid);
  }

  public Set<String> selectIssueKeysByComponentUuid(DbSession session, String componentUuid,
    List<String> includingRepositories, List<String> excludingRepositories,
    List<String> languages, int page) {
    return mapper(session).selectIssueKeysByComponentUuidWithFilters(componentUuid, includingRepositories, excludingRepositories,
      languages, Pagination.forPage(page).andSize(BIG_PAGE_SIZE));
  }

  public Set<String> selectIssueKeysByComponentUuidAndChangedSinceDate(DbSession session, String componentUuid, long changedSince,
    List<String> includingRepositories, List<String> excludingRepositories,
    List<String> languages, int page) {
    return mapper(session).selectIssueKeysByComponentUuidAndChangedSinceDate(componentUuid, changedSince,
      includingRepositories, excludingRepositories, languages, Pagination.forPage(page).andSize(BIG_PAGE_SIZE));
  }

  public Set<String> selectComponentUuidsOfOpenIssuesForProjectUuid(DbSession session, String projectUuid) {
    return mapper(session).selectComponentUuidsOfOpenIssuesForProjectUuid(projectUuid);
  }

  public List<PrIssueDto> selectOpenByComponentUuids(DbSession dbSession, Collection<String> componentUuids) {
    return executeLargeInputs(componentUuids, mapper(dbSession)::selectOpenByComponentUuids);
  }

  public Collection<IssueGroupDto> selectIssueGroupsByComponent(DbSession dbSession, ComponentDto component, long leakPeriodBeginningDate) {
    return mapper(dbSession).selectIssueGroupsByComponent(component, leakPeriodBeginningDate);
  }

  public Collection<IssueImpactGroupDto> selectIssueImpactGroupsByComponent(DbSession dbSession, ComponentDto component, long leakPeriodBeginningDate) {
    return mapper(dbSession).selectIssueImpactGroupsByComponent(component, leakPeriodBeginningDate);
  }

  public Collection<IssueImpactSeverityGroupDto> selectIssueImpactSeverityGroupsByComponent(DbSession dbSession, ComponentDto component,
    long leakPeriodBeginningDate) {
    return mapper(dbSession).selectIssueImpactSeverityGroupsByComponent(component, leakPeriodBeginningDate);
  }

  public Cursor<IndexedIssueDto> scrollIssuesForIndexation(DbSession dbSession, @Nullable @Param("branchUuid") String branchUuid,
    @Nullable @Param("issueKeys") Collection<String> issueKeys) {
    return mapper(dbSession).scrollIssuesForIndexation(branchUuid, issueKeys);
  }

  public Cursor<IssueStatsDto> scrollIssuesForIssueStats(DbSession dbSession, @Param("branchUuid") String branchUuid) {
    return mapper(dbSession).scrollIssuesForIssueStats(branchUuid);
  }

  public AggregatedIssueStatsDto aggregateIssueStatsForBranchUuidAndRuleKey(DbSession dbSession,
    String branchUuid, RuleKey ruleKey) {
    return mapper(dbSession).aggregateIssueStatsForBranchUuidAndRuleKey(branchUuid, ruleKey.repository(), ruleKey.rule());
  }

  public void insert(DbSession session, IssueDto dto) {
    mapper(session).insert(dto);
    insertIssueImpacts(session, dto);
  }

  /**
   * In certain circumstances, most notably persisting issues in the CE, importing issues and web issue storage, we wish to avoid persisting
   * both issues and their issue impacts in the same batch transactions, as this introduces a significant performance regression. In those
   * situations, issues will be inserted first, and then subsequently their issue impacts using
   * {@link IssueDao#insertIssueImpacts(DbSession, IssueDto) ()}.
   */
  public void insertWithoutImpacts(DbSession session, IssueDto dto) {
    mapper(session).insert(dto);
  }

  public void insertIssueImpacts(DbSession session, IssueDto issueDto) {
    IssueMapper mapper = mapper(session);
    issueDto.getImpacts()
      .forEach(impact -> mapper.insertIssueImpact(issueDto.getKey(), impact));
  }

  private void updateIssueImpacts(DbSession session, IssueDto issueDto) {
    deleteIssueImpacts(session, issueDto);
    insertIssueImpacts(session, issueDto);
  }

  public void deleteIssueImpacts(DbSession session, IssueDto issueDto) {
    mapper(session).deleteIssueImpacts(issueDto.getKey());
  }

  public void insert(DbSession session, IssueDto dto, IssueDto... others) {
    insert(session, dto);
    for (IssueDto other : others) {
      insert(session, other);
    }
  }

  public void update(DbSession session, IssueDto dto) {
    mapper(session).update(dto);
    updateIssueImpacts(session, dto);
  }

  /**
   * Update issue without updating issue impacts. Used only in batch update.
   * Issue impacts should be updated separately.
   */
  public void updateWithoutIssueImpacts(DbSession session, IssueDto dto) {
    mapper(session).update(dto);
  }

  public boolean updateIfBeforeSelectedDate(DbSession session, IssueDto dto) {
    return mapper(session).updateIfBeforeSelectedDate(dto) != 0;
  }

  public List<IssueDto> selectByKeysIfNotUpdatedAt(DbSession session, List<String> keys, long updatedAt) {
    return mapper(session).selectByKeysIfNotUpdatedAt(keys, updatedAt);
  }

  public List<IssueCount> countSandboxIssuesPerProject(DbSession dbSession) {
    return mapper(dbSession).countSandboxIssuesPerProject();
  }

  public List<IssueCountByStatusAndResolution> countIssuesByStatusOnMainBranches(DbSession dbSession) {
    return mapper(dbSession).countIssuesByStatusOnMainBranches();
  }

  public void insertAsNewCodeOnReferenceBranch(DbSession session, NewCodeReferenceIssueDto dto) {
    mapper(session).insertAsNewCodeOnReferenceBranch(dto);
  }

  public void deleteAsNewCodeOnReferenceBranch(DbSession session, String issueKey) {
    mapper(session).deleteAsNewCodeOnReferenceBranch(issueKey);
  }

  private static IssueMapper mapper(DbSession session) {
    return session.getMapper(IssueMapper.class);
  }

  public List<IssueDto> selectByBranch(DbSession dbSession, Set<String> issueKeysSnapshot, IssueQueryParams issueQueryParams) {
    return mapper(dbSession).selectByBranch(issueKeysSnapshot, issueQueryParams.getChangedSince());
  }

  public List<String> selectRecentlyClosedIssues(DbSession dbSession, IssueQueryParams issueQueryParams) {
    return mapper(dbSession).selectRecentlyClosedIssues(issueQueryParams);
  }

  /**
   * Returned results are unordered.
   */
  public List<String> selectIssueKeysByQuery(DbSession dbSession, IssueListQuery issueListQuery, Pagination pagination) {
    return mapper(dbSession).selectIssueKeysByQuery(issueListQuery, pagination);
  }

  public int resetFlagFromSonarQubeUpdate(DbSession session) {
    return mapper(session).resetFlagFromSonarQubeUpdate(System.currentTimeMillis());
  }

}
