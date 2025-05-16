/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.issue.index;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.rule.RuleType;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.issue.IssueFixedDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.issue.SearchRequest;
import org.sonar.server.issue.index.IssueQuery.PeriodStart;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Collections2.transform;
import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.sonar.api.issue.Issue.STATUSES;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.api.measures.CoreMetrics.ANALYSIS_FROM_SONARQUBE_9_4_KEY;
import static org.sonar.api.utils.DateUtils.longToDate;
import static org.sonar.api.utils.DateUtils.parseEndingDateOrDateTime;
import static org.sonar.api.utils.DateUtils.parseStartingDateOrDateTime;
import static org.sonar.db.permission.ProjectPermission.SCAN;
import static org.sonar.db.permission.ProjectPermission.USER;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_AFTER;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_IN_LAST;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_FIXED_IN_PULL_REQUEST;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_IN_NEW_CODE_PERIOD;

/**
 * This component is used to create an IssueQuery, in order to transform the component and component roots keys into uuid.
 */
@ServerSide
public class IssueQueryFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(IssueQueryFactory.class);

  public static final String UNKNOWN = "<UNKNOWN>";
  public static final List<String> ISSUE_STATUSES = STATUSES.stream()
    .filter(s -> !s.equals(STATUS_TO_REVIEW))
    .filter(s -> !s.equals(STATUS_REVIEWED))
    .collect(ImmutableList.toImmutableList());
  public static final Set<String> ISSUE_TYPE_NAMES = Arrays.stream(RuleType.values())
    .filter(t -> t != RuleType.SECURITY_HOTSPOT)
    .map(Enum::name)
    .collect(Collectors.toSet());
  private static final ComponentDto UNKNOWN_COMPONENT = new ComponentDto().setUuid(UNKNOWN).setBranchUuid(UNKNOWN);
  private static final Set<String> QUALIFIERS_WITHOUT_LEAK_PERIOD = new HashSet<>(Arrays.asList(ComponentQualifiers.APP, ComponentQualifiers.VIEW,
    ComponentQualifiers.SUBVIEW));
  private final DbClient dbClient;
  private final Clock clock;
  private final UserSession userSession;

  public IssueQueryFactory(DbClient dbClient, Clock clock, UserSession userSession) {
    this.dbClient = dbClient;
    this.clock = clock;
    this.userSession = userSession;
  }

  public IssueQuery create(SearchRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      final ZoneId timeZone = parseTimeZone(request.getTimeZone()).orElse(clock.getZone());

      Collection<RuleDto> ruleDtos = ruleKeysToRuleId(dbSession, request.getRules());
      Collection<String> ruleUuids = ruleDtos.stream().map(RuleDto::getUuid).collect(Collectors.toSet());
      Collection<String> issueKeys = collectIssueKeys(dbSession, request);

      if (request.getRules() != null && request.getRules().stream().collect(Collectors.toSet()).size() != ruleDtos.size()) {
        ruleUuids.add("non-existing-uuid");
      }

      IssueQuery.Builder builder = IssueQuery.builder()
        .issueKeys(issueKeys)
        .severities(request.getSeverities())
        .cleanCodeAttributesCategories(request.getCleanCodeAttributesCategories())
        .impactSoftwareQualities(request.getImpactSoftwareQualities())
        .impactSeverities(request.getImpactSeverities())
        .statuses(request.getStatuses())
        .resolutions(request.getResolutions())
        .issueStatuses(request.getIssueStatuses())
        .resolved(request.getResolved())
        .prioritizedRule(request.getPrioritizedRule())
        .rules(ruleDtos)
        .ruleUuids(ruleUuids)
        .assigneeUuids(request.getAssigneeUuids())
        .authors(request.getAuthors())
        .scopes(request.getScopes())
        .languages(request.getLanguages())
        .tags(request.getTags())
        .types(request.getTypes())
        .pciDss32(request.getPciDss32())
        .pciDss40(request.getPciDss40())
        .owaspAsvs40(request.getOwaspAsvs40())
        .owaspAsvsLevel(request.getOwaspAsvsLevel())
        .owaspMobileTop10For2024(request.getOwaspMobileTop10For2024())
        .owaspTop10(request.getOwaspTop10())
        .owaspTop10For2021(request.getOwaspTop10For2021())
        .stigAsdR5V3(request.getStigAsdV5R3())
        .casa(request.getCasa())
        .sansTop25(request.getSansTop25())
        .cwe(request.getCwe())
        .sonarsourceSecurity(request.getSonarsourceSecurity())
        .assigned(request.getAssigned())
        .createdAt(parseStartingDateOrDateTime(request.getCreatedAt(), timeZone))
        .createdBefore(parseEndingDateOrDateTime(request.getCreatedBefore(), timeZone))
        .facetMode(request.getFacetMode())
        .timeZone(timeZone)
        .codeVariants(request.getCodeVariants());

      List<ComponentDto> allComponents = new ArrayList<>();
      boolean effectiveOnComponentOnly = mergeDeprecatedComponentParameters(dbSession, request, allComponents);
      addComponentParameters(builder, dbSession, effectiveOnComponentOnly, allComponents, request);

      setCreatedAfterFromRequest(dbSession, builder, request, allComponents, timeZone);
      String sort = request.getSort();
      if (!isNullOrEmpty(sort)) {
        builder.sort(sort);
        builder.asc(request.getAsc());
      }
      return builder.build();
    }
  }

  private Collection<String> collectIssueKeys(DbSession dbSession, SearchRequest request) {
    Collection<String> issueKeys = null;
    if (request.getFixedInPullRequest() != null) {
      issueKeys = getIssuesFixedByPullRequest(dbSession, request);
    }
    List<String> requestIssues = request.getIssues();
    if (requestIssues != null && !requestIssues.isEmpty()) {
      if (issueKeys == null) {
        issueKeys = new ArrayList<>();
      }
      issueKeys.addAll(requestIssues);
    }
    return issueKeys;
  }

  private Collection<String> getIssuesFixedByPullRequest(DbSession dbSession, SearchRequest request) {
    String fixedInPullRequest = request.getFixedInPullRequest();
    checkArgument(StringUtils.isNotBlank(fixedInPullRequest), "Parameter '%s' is empty", PARAM_FIXED_IN_PULL_REQUEST);
    List<String> componentKeys = request.getComponentKeys();
    if (componentKeys == null || componentKeys.size() != 1) {
      throw new IllegalArgumentException("Exactly one project needs to be provided in the " +
        "'" + PARAM_COMPONENTS + "' param when used together with '" + PARAM_FIXED_IN_PULL_REQUEST + "' param");
    }
    String projectKey = componentKeys.get(0);
    ProjectDto projectDto = dbClient.projectDao().selectProjectByKey(dbSession, projectKey)
      .orElseThrow(() -> new IllegalArgumentException("Project with key '" + projectKey + "' does not exist"));
    BranchDto pullRequest = dbClient.branchDao().selectByPullRequestKey(dbSession, projectDto.getUuid(), fixedInPullRequest)
      .orElseThrow(() -> new IllegalArgumentException("Pull request with key '" + fixedInPullRequest + "' does not exist for project " +
        projectKey));

    String branch = request.getBranch();
    if (branch != null) {
      BranchDto targetBranch = dbClient.branchDao().selectByBranchKey(dbSession, projectDto.getUuid(), branch)
        .orElseThrow(() -> new IllegalArgumentException("Branch with key '" + branch + "' does not exist"));
      if (!Objects.equals(targetBranch.getUuid(), pullRequest.getMergeBranchUuid())) {
        throw new IllegalArgumentException("Pull request with key '" + fixedInPullRequest + "' does not target branch '" + branch + "'");
      }
    }
    return dbClient.issueFixedDao().selectByPullRequest(dbSession, pullRequest.getUuid())
      .stream()
      .map(IssueFixedDto::issueKey)
      .collect(Collectors.toSet());
  }

  private static Optional<ZoneId> parseTimeZone(@Nullable String timeZone) {
    if (timeZone == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(ZoneId.of(timeZone));
    } catch (DateTimeException e) {
      LOGGER.warn("TimeZone '" + timeZone + "' cannot be parsed as a valid zone ID");
      return Optional.empty();
    }
  }

  private void setCreatedAfterFromDates(IssueQuery.Builder builder, @Nullable Date createdAfter, @Nullable String createdInLast,
    boolean createdAfterInclusive) {
    Date actualCreatedAfter = createdAfter;
    if (createdInLast != null) {
      actualCreatedAfter = Date.from(
        OffsetDateTime.now(clock)
          .minus(Period.parse("P" + createdInLast.toUpperCase(Locale.ENGLISH)))
          .toInstant());
    }
    builder.createdAfter(actualCreatedAfter, createdAfterInclusive);
  }

  private void setCreatedAfterFromRequest(DbSession dbSession, IssueQuery.Builder builder, SearchRequest request,
    List<ComponentDto> componentUuids, ZoneId timeZone) {
    Date createdAfter = parseStartingDateOrDateTime(request.getCreatedAfter(), timeZone);
    String createdInLast = request.getCreatedInLast();

    if (notInNewCodePeriod(request)) {
      checkArgument(createdAfter == null || createdInLast == null, format("Parameters %s and %s cannot be set simultaneously",
        PARAM_CREATED_AFTER, PARAM_CREATED_IN_LAST));
      setCreatedAfterFromDates(builder, createdAfter, createdInLast, true);
    } else {
      // If the filter is on leak period
      checkArgument(createdAfter == null, "Parameters '%s' and '%s' cannot be set simultaneously", PARAM_CREATED_AFTER,
        PARAM_IN_NEW_CODE_PERIOD);
      checkArgument(createdInLast == null,
        format("Parameters '%s' and '%s' cannot be set simultaneously", PARAM_CREATED_IN_LAST, PARAM_IN_NEW_CODE_PERIOD));

      checkArgument(componentUuids.size() == 1, "One and only one component must be provided when searching in new code period");
      ComponentDto component = componentUuids.iterator().next();

      if (!QUALIFIERS_WITHOUT_LEAK_PERIOD.contains(component.qualifier()) && request.getPullRequest() == null) {
        Optional<SnapshotDto> snapshot = getLastAnalysis(dbSession, component);
        if (!snapshot.isEmpty() && isLastAnalysisFromReAnalyzedReferenceBranch(dbSession, snapshot.get())) {
          builder.newCodeOnReference(true);
          return;
        }
        // if last analysis has no period date, then no issue should be considered new.
        Date createdAfterFromSnapshot = findCreatedAfterFromComponentUuid(snapshot);
        setCreatedAfterFromDates(builder, createdAfterFromSnapshot, null, false);
      }
    }
  }

  private static boolean notInNewCodePeriod(SearchRequest request) {
    Boolean inNewCodePeriod = request.getInNewCodePeriod();
    inNewCodePeriod = Boolean.TRUE.equals(inNewCodePeriod);
    return !inNewCodePeriod;
  }

  private Date findCreatedAfterFromComponentUuid(Optional<SnapshotDto> snapshot) {
    return snapshot.map(s -> longToDate(s.getPeriodDate())).orElseGet(() -> new Date(clock.millis()));
  }

  private static boolean isLastAnalysisUsingReferenceBranch(SnapshotDto snapshot) {
    return !isNullOrEmpty(snapshot.getPeriodMode()) && snapshot.getPeriodMode().equals(REFERENCE_BRANCH.name());
  }

  private boolean isLastAnalysisFromSonarQube94Onwards(DbSession dbSession, String componentUuid) {
    return dbClient.measureDao().selectByComponentUuid(dbSession, componentUuid)
      .filter(m -> m.getMetricValues().containsKey(ANALYSIS_FROM_SONARQUBE_9_4_KEY))
      .isPresent();
  }

  private Optional<SnapshotDto> getLastAnalysis(DbSession dbSession, ComponentDto component) {
    return dbClient.snapshotDao().selectLastAnalysisByComponentUuid(dbSession, component.uuid());
  }

  private List<SnapshotDto> getLastAnalysis(DbSession dbSession, Set<String> projectUuids) {
    return dbClient.snapshotDao().selectLastAnalysesByRootComponentUuids(dbSession, projectUuids);
  }

  private boolean mergeDeprecatedComponentParameters(DbSession session, SearchRequest request, List<ComponentDto> allComponents) {
    Boolean onComponentOnly = request.getOnComponentOnly();
    Collection<String> componentKeys = request.getComponentKeys();
    Collection<String> componentUuids = request.getComponentUuids();
    String branch = request.getBranch();
    String pullRequest = request.getPullRequest();

    boolean effectiveOnComponentOnly = false;

    checkArgument(atMostOneNonNullElement(componentKeys, componentUuids),
      "At most one of the following parameters can be provided: %s and %s", PARAM_COMPONENTS, PARAM_COMPONENT_UUIDS);

    if (componentKeys != null) {
      allComponents.addAll(getComponentsFromKeys(session, componentKeys, branch, pullRequest));
      effectiveOnComponentOnly = BooleanUtils.isTrue(onComponentOnly);
    } else if (componentUuids != null) {
      allComponents.addAll(getComponentsFromUuids(session, componentUuids));
      effectiveOnComponentOnly = BooleanUtils.isTrue(onComponentOnly);
    }

    return effectiveOnComponentOnly;
  }

  private static boolean atMostOneNonNullElement(Object... objects) {
    return Arrays.stream(objects)
      .filter(Objects::nonNull)
      .count() <= 1;
  }

  private void addComponentParameters(IssueQuery.Builder builder, DbSession session, boolean onComponentOnly, List<ComponentDto> components,
    SearchRequest request) {
    builder.onComponentOnly(onComponentOnly);
    if (onComponentOnly) {
      builder.componentUuids(components.stream().map(ComponentDto::uuid).toList());
      setBranch(builder, components.get(0), request.getBranch(), request.getPullRequest(), session);
      return;
    }

    List<String> projectKeys = request.getProjectKeys();
    if (projectKeys != null) {
      List<ComponentDto> branchComponents = getComponentsFromKeys(session, projectKeys, request.getBranch(), request.getPullRequest());
      Set<String> projectUuids = retrieveProjectUuidsFromComponents(session, branchComponents);
      builder.projectUuids(projectUuids);
      setBranch(builder, branchComponents.get(0), request.getBranch(), request.getPullRequest(), session);
    }
    builder.directories(request.getDirectories());
    builder.files(request.getFiles());

    addComponentsBasedOnQualifier(builder, session, components, request);
  }

  @NotNull
  private Set<String> retrieveProjectUuidsFromComponents(DbSession session, List<ComponentDto> branchComponents) {
    Set<String> branchUuids = branchComponents.stream().map(ComponentDto::branchUuid).collect(Collectors.toSet());
    return dbClient.branchDao().selectByUuids(session, branchUuids).stream()
      .map(BranchDto::getProjectUuid)
      .collect(Collectors.toSet());
  }

  private void addComponentsBasedOnQualifier(IssueQuery.Builder builder, DbSession dbSession, List<ComponentDto> components,
    SearchRequest request) {
    if (components.isEmpty()) {
      return;
    }
    if (components.stream().map(ComponentDto::uuid).anyMatch(uuid -> uuid.equals(UNKNOWN))) {
      builder.componentUuids(singleton(UNKNOWN));
      return;
    }

    Set<String> qualifiers = components.stream().map(ComponentDto::qualifier).collect(Collectors.toSet());
    checkArgument(qualifiers.size() == 1, "All components must have the same qualifier, found %s", String.join(",", qualifiers));

    setBranch(builder, components.get(0), request.getBranch(), request.getPullRequest(), dbSession);
    String qualifier = qualifiers.iterator().next();
    switch (qualifier) {
      case ComponentQualifiers.VIEW, ComponentQualifiers.SUBVIEW:
        addViewsOrSubViews(builder, components);
        break;
      case ComponentQualifiers.APP:
        addApplications(builder, dbSession, components, request);
        addProjectUuidsForApplication(builder, dbSession, request);
        break;
      case ComponentQualifiers.PROJECT:
        builder.projectUuids(retrieveProjectUuidsFromComponents(dbSession, components));
        break;
      case ComponentQualifiers.DIRECTORY:
        addDirectories(builder, components);
        break;
      case ComponentQualifiers.FILE, ComponentQualifiers.UNIT_TEST_FILE:
        builder.componentUuids(components.stream().map(ComponentDto::uuid).toList());
        break;
      default:
        throw new IllegalArgumentException("Unable to set search root context for components " + Joiner.on(',').join(components));
    }
  }

  private BranchDto findComponentBranch(DbSession dbSession, ComponentDto componentDto) {
    Optional<BranchDto> optionalBranch = dbClient.branchDao().selectByUuid(dbSession, componentDto.branchUuid());
    checkArgument(optionalBranch.isPresent(), "All components must belong to a branch. This error may indicate corrupted data.");
    return optionalBranch.get();
  }

  private void addProjectUuidsForApplication(IssueQuery.Builder builder, DbSession session, SearchRequest request) {
    List<String> projectKeys = request.getProjectKeys();
    if (projectKeys != null) {
      // On application, branch should only be applied on the application, not on projects
      List<ComponentDto> appBranchComponents = getComponentsFromKeys(session, projectKeys, null, null);
      Set<String> appUuids = retrieveProjectUuidsFromComponents(session, appBranchComponents);
      builder.projectUuids(appUuids);
    }
  }

  private void addViewsOrSubViews(IssueQuery.Builder builder, Collection<ComponentDto> viewOrSubViewUuids) {
    List<String> filteredViewUuids = viewOrSubViewUuids.stream()
      .filter(uuid -> (userSession.hasComponentPermission(USER, uuid) || userSession.hasComponentPermission(SCAN, uuid) || userSession.hasPermission(GlobalPermission.SCAN)))
      .map(ComponentDto::uuid)
      .collect(Collectors.toCollection(ArrayList::new));
    if (filteredViewUuids.isEmpty()) {
      filteredViewUuids.add(UNKNOWN);
    }
    builder.viewUuids(filteredViewUuids);
  }

  private void addApplications(IssueQuery.Builder builder, DbSession dbSession, List<ComponentDto> appBranchComponents,
    SearchRequest request) {
    Set<String> authorizedAppBranchUuids = appBranchComponents.stream()
      .filter(app -> userSession.hasComponentPermission(USER, app) && userSession.hasChildProjectsPermission(USER, app))
      .map(ComponentDto::uuid)
      .collect(Collectors.toSet());

    builder.viewUuids(authorizedAppBranchUuids.isEmpty() ? singleton(UNKNOWN) : authorizedAppBranchUuids);
    addCreatedAfterByProjects(builder, dbSession, request, authorizedAppBranchUuids);
  }

  private void addCreatedAfterByProjects(IssueQuery.Builder builder, DbSession dbSession, SearchRequest request,
    Set<String> appBranchUuids) {
    if (notInNewCodePeriod(request) || request.getPullRequest() != null) {
      return;
    }

    Set<String> projectBranchUuids = appBranchUuids.stream()
      .flatMap(app -> dbClient.componentDao().selectProjectBranchUuidsFromView(dbSession, app, app).stream())
      .collect(Collectors.toSet());

    List<SnapshotDto> snapshots = getLastAnalysis(dbSession, projectBranchUuids);

    Set<String> newCodeReferenceByProjects = snapshots
      .stream()
      .filter(s -> isLastAnalysisFromReAnalyzedReferenceBranch(dbSession, s))
      .map(SnapshotDto::getRootComponentUuid)
      .collect(Collectors.toSet());

    Map<String, PeriodStart> leakByProjects = snapshots
      .stream()
      .filter(s -> s.getPeriodDate() != null && !isLastAnalysisFromReAnalyzedReferenceBranch(dbSession, s))
      .collect(Collectors.toMap(SnapshotDto::getRootComponentUuid, s1 -> new PeriodStart(longToDate(s1.getPeriodDate()), false)));

    builder.createdAfterByProjectUuids(leakByProjects);
    builder.newCodeOnReferenceByProjectUuids(newCodeReferenceByProjects);
  }

  private boolean isLastAnalysisFromReAnalyzedReferenceBranch(DbSession dbSession, SnapshotDto snapshot) {
    return isLastAnalysisUsingReferenceBranch(snapshot) &&
      isLastAnalysisFromSonarQube94Onwards(dbSession, snapshot.getRootComponentUuid());
  }

  private static void addDirectories(IssueQuery.Builder builder, List<ComponentDto> directories) {
    Set<String> paths = directories.stream().map(ComponentDto::path).collect(Collectors.toSet());
    builder.directories(paths);
  }

  private List<ComponentDto> getComponentsFromKeys(DbSession dbSession, Collection<String> componentKeys, @Nullable String branch,
    @Nullable String pullRequest) {
    List<ComponentDto> componentDtos = dbClient.componentDao().selectByKeys(dbSession, componentKeys, branch, pullRequest);
    if (!componentKeys.isEmpty() && componentDtos.isEmpty()) {
      return singletonList(UNKNOWN_COMPONENT);
    }
    return componentDtos;
  }

  private List<ComponentDto> getComponentsFromUuids(DbSession dbSession, Collection<String> componentUuids) {
    List<ComponentDto> componentDtos = dbClient.componentDao().selectByUuids(dbSession, componentUuids);
    if (!componentUuids.isEmpty() && componentDtos.isEmpty()) {
      return singletonList(UNKNOWN_COMPONENT);
    }
    return componentDtos;
  }

  private Collection<RuleDto> ruleKeysToRuleId(DbSession dbSession, @Nullable Collection<String> rules) {
    if (rules != null) {
      return dbClient.ruleDao().selectByKeys(dbSession, transform(rules, RuleKey::parse));
    }
    return Collections.emptyList();
  }

  private void setBranch(IssueQuery.Builder builder, ComponentDto component, @Nullable String branch, @Nullable String pullRequest,
    DbSession session) {
    builder.branchUuid(branch == null && pullRequest == null ? null : component.branchUuid());
    if (UNKNOWN_COMPONENT.equals(component) || (pullRequest == null && branch == null)) {
      builder.mainBranch(true);
    } else {
      BranchDto branchDto = findComponentBranch(session, component);
      builder.mainBranch(branchDto.isMain());
    }
  }
}
