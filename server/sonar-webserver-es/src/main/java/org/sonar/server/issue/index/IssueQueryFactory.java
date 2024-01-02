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
import org.apache.commons.lang.BooleanUtils;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
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
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.util.stream.MoreCollectors.toHashSet;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_KEYS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_AFTER;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_IN_LAST;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_IN_NEW_CODE_PERIOD;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SINCE_LEAK_PERIOD;

/**
 * This component is used to create an IssueQuery, in order to transform the component and component roots keys into uuid.
 */
@ServerSide
public class IssueQueryFactory {

  private static final Logger LOGGER = Loggers.get(IssueQueryFactory.class);

  public static final String UNKNOWN = "<UNKNOWN>";
  public static final List<String> ISSUE_STATUSES = STATUSES.stream()
    .filter(s -> !s.equals(STATUS_TO_REVIEW))
    .filter(s -> !s.equals(STATUS_REVIEWED))
    .collect(ImmutableList.toImmutableList());
  public static final Set<String> ISSUE_TYPE_NAMES = Arrays.stream(RuleType.values())
    .filter(t -> t != RuleType.SECURITY_HOTSPOT)
    .map(Enum::name)
    .collect(MoreCollectors.toSet(RuleType.values().length - 1));
  private static final ComponentDto UNKNOWN_COMPONENT = new ComponentDto().setUuid(UNKNOWN).setBranchUuid(UNKNOWN);
  private static final Set<String> QUALIFIERS_WITHOUT_LEAK_PERIOD = new HashSet<>(Arrays.asList(Qualifiers.APP, Qualifiers.VIEW, Qualifiers.SUBVIEW));
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

      if (request.getRules() != null && request.getRules().stream().collect(toSet()).size() != ruleDtos.size()) {
        ruleUuids.add("non-existing-uuid");
      }

      IssueQuery.Builder builder = IssueQuery.builder()
        .issueKeys(request.getIssues())
        .severities(request.getSeverities())
        .statuses(request.getStatuses())
        .resolutions(request.getResolutions())
        .resolved(request.getResolved())
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
        .owaspTop10(request.getOwaspTop10())
        .owaspTop10For2021(request.getOwaspTop10For2021())
        .sansTop25(request.getSansTop25())
        .cwe(request.getCwe())
        .sonarsourceSecurity(request.getSonarsourceSecurity())
        .assigned(request.getAssigned())
        .createdAt(parseStartingDateOrDateTime(request.getCreatedAt(), timeZone))
        .createdBefore(parseEndingDateOrDateTime(request.getCreatedBefore(), timeZone))
        .facetMode(request.getFacetMode())
        .timeZone(timeZone);

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

  private void setCreatedAfterFromDates(IssueQuery.Builder builder, @Nullable Date createdAfter, @Nullable String createdInLast, boolean createdAfterInclusive) {
    Date actualCreatedAfter = createdAfter;
    if (createdInLast != null) {
      actualCreatedAfter = Date.from(
        OffsetDateTime.now(clock)
          .minus(Period.parse("P" + createdInLast.toUpperCase(Locale.ENGLISH)))
          .toInstant());
    }
    builder.createdAfter(actualCreatedAfter, createdAfterInclusive);
  }

  private void setCreatedAfterFromRequest(DbSession dbSession, IssueQuery.Builder builder, SearchRequest request, List<ComponentDto> componentUuids, ZoneId timeZone) {
    Date createdAfter = parseStartingDateOrDateTime(request.getCreatedAfter(), timeZone);
    String createdInLast = request.getCreatedInLast();

    if (notInNewCodePeriod(request)) {
      checkArgument(createdAfter == null || createdInLast == null, format("Parameters %s and %s cannot be set simultaneously", PARAM_CREATED_AFTER, PARAM_CREATED_IN_LAST));
      setCreatedAfterFromDates(builder, createdAfter, createdInLast, true);
    } else {
      // If the filter is on leak period
      checkArgument(createdAfter == null, "Parameters '%s' and '%s' or '%s' cannot be set simultaneously", PARAM_CREATED_AFTER, PARAM_IN_NEW_CODE_PERIOD, PARAM_SINCE_LEAK_PERIOD);
      checkArgument(createdInLast == null,
        format("Parameters '%s' and '%s' or '%s' cannot be set simultaneously", PARAM_CREATED_IN_LAST, PARAM_IN_NEW_CODE_PERIOD, PARAM_SINCE_LEAK_PERIOD));

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
    Boolean sinceLeakPeriod = request.getSinceLeakPeriod();
    Boolean inNewCodePeriod = request.getInNewCodePeriod();

    checkArgument(validPeriodParameterValues(sinceLeakPeriod, inNewCodePeriod),
      "If both provided, the following parameters %s and %s must match.", PARAM_SINCE_LEAK_PERIOD, PARAM_IN_NEW_CODE_PERIOD);

    sinceLeakPeriod = Boolean.TRUE.equals(sinceLeakPeriod);
    inNewCodePeriod = Boolean.TRUE.equals(inNewCodePeriod);

    return !sinceLeakPeriod && !inNewCodePeriod;
  }

  private static boolean validPeriodParameterValues(@Nullable Boolean sinceLeakPeriod, @Nullable Boolean inNewCodePeriod) {
    return atMostOneNonNullElement(sinceLeakPeriod, inNewCodePeriod) || !Boolean.logicalXor(sinceLeakPeriod, inNewCodePeriod);
  }

  private Date findCreatedAfterFromComponentUuid(Optional<SnapshotDto> snapshot) {
    return snapshot.map(s -> longToDate(s.getPeriodDate())).orElseGet(() -> new Date(clock.millis()));
  }

  private static boolean isLastAnalysisUsingReferenceBranch(SnapshotDto snapshot) {
    return !isNullOrEmpty(snapshot.getPeriodMode()) && snapshot.getPeriodMode().equals(REFERENCE_BRANCH.name());
  }

  private boolean isLastAnalysisFromSonarQube94Onwards(DbSession dbSession, String componentUuid) {
    return dbClient.liveMeasureDao().selectMeasure(dbSession, componentUuid, ANALYSIS_FROM_SONARQUBE_9_4_KEY).isPresent();
  }

  private Optional<SnapshotDto> getLastAnalysis(DbSession dbSession, ComponentDto component) {
    return dbClient.snapshotDao().selectLastAnalysisByComponentUuid(dbSession, component.uuid());
  }

  private List<SnapshotDto> getLastAnalysis(DbSession dbSession, Set<String> projectUuids) {
    return dbClient.snapshotDao().selectLastAnalysesByRootComponentUuids(dbSession, projectUuids);
  }

  private boolean mergeDeprecatedComponentParameters(DbSession session, SearchRequest request, List<ComponentDto> allComponents) {
    Boolean onComponentOnly = request.getOnComponentOnly();
    Collection<String> components = request.getComponents();
    Collection<String> componentUuids = request.getComponentUuids();
    String branch = request.getBranch();
    String pullRequest = request.getPullRequest();

    boolean effectiveOnComponentOnly = false;

    checkArgument(atMostOneNonNullElement(components, componentUuids),
      "At most one of the following parameters can be provided: %s and %s", PARAM_COMPONENT_KEYS, PARAM_COMPONENT_UUIDS);

    if (components != null) {
      allComponents.addAll(getComponentsFromKeys(session, components, branch, pullRequest));
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
      builder.componentUuids(components.stream().map(ComponentDto::uuid).collect(toList()));
      setBranch(builder, components.get(0), request.getBranch(), request.getPullRequest(), session);
      return;
    }

    List<String> projectKeys = request.getProjects();
    if (projectKeys != null) {
      List<ComponentDto> projects = getComponentsFromKeys(session, projectKeys, request.getBranch(), request.getPullRequest());
      builder.projectUuids(projects.stream().map(IssueQueryFactory::toProjectUuid).collect(toList()));
      setBranch(builder, projects.get(0), request.getBranch(), request.getPullRequest(), session);
    }
    builder.directories(request.getDirectories());
    builder.files(request.getFiles());

    addComponentsBasedOnQualifier(builder, session, components, request);
  }

  private void addComponentsBasedOnQualifier(IssueQuery.Builder builder, DbSession dbSession, List<ComponentDto> components, SearchRequest request) {
    if (components.isEmpty()) {
      return;
    }
    if (components.stream().map(ComponentDto::uuid).anyMatch(uuid -> uuid.equals(UNKNOWN))) {
      builder.componentUuids(singleton(UNKNOWN));
      return;
    }

    Set<String> qualifiers = components.stream().map(ComponentDto::qualifier).collect(toHashSet());
    checkArgument(qualifiers.size() == 1, "All components must have the same qualifier, found %s", String.join(",", qualifiers));

    setBranch(builder, components.get(0), request.getBranch(), request.getPullRequest(), dbSession);
    String qualifier = qualifiers.iterator().next();
    switch (qualifier) {
      case Qualifiers.VIEW, Qualifiers.SUBVIEW:
        addViewsOrSubViews(builder, components);
        break;
      case Qualifiers.APP:
        addApplications(builder, dbSession, components, request);
        addProjectUuidsForApplication(builder, dbSession, request);
        break;
      case Qualifiers.PROJECT:
        builder.projectUuids(components.stream().map(IssueQueryFactory::toProjectUuid).collect(toList()));
        break;
      case Qualifiers.MODULE:
        builder.moduleRootUuids(components.stream().map(ComponentDto::uuid).collect(toList()));
        break;
      case Qualifiers.DIRECTORY:
        addDirectories(builder, components);
        break;
      case Qualifiers.FILE, Qualifiers.UNIT_TEST_FILE:
        builder.componentUuids(components.stream().map(ComponentDto::uuid).collect(toList()));
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
    List<String> projectKeys = request.getProjects();
    if (projectKeys != null) {
      // On application, branch should only be applied on the application, not on projects
      List<ComponentDto> projects = getComponentsFromKeys(session, projectKeys, null, null);
      builder.projectUuids(projects.stream().map(ComponentDto::uuid).collect(toList()));
    }
  }

  private void addViewsOrSubViews(IssueQuery.Builder builder, Collection<ComponentDto> viewOrSubViewUuids) {
    List<String> filteredViewUuids = viewOrSubViewUuids.stream()
      .filter(uuid -> userSession.hasComponentPermission(USER, uuid))
      .map(ComponentDto::uuid)
      .collect(Collectors.toList());
    if (filteredViewUuids.isEmpty()) {
      filteredViewUuids.add(UNKNOWN);
    }
    builder.viewUuids(filteredViewUuids);
  }

  private void addApplications(IssueQuery.Builder builder, DbSession dbSession, List<ComponentDto> applications, SearchRequest request) {
    Set<String> authorizedApplicationUuids = applications.stream()
      .filter(app -> userSession.hasComponentPermission(USER, app) && userSession.hasChildProjectsPermission(USER, app))
      .map(ComponentDto::uuid)
      .collect(toSet());

    builder.viewUuids(authorizedApplicationUuids.isEmpty() ? singleton(UNKNOWN) : authorizedApplicationUuids);
    addCreatedAfterByProjects(builder, dbSession, request, authorizedApplicationUuids);
  }

  private void addCreatedAfterByProjects(IssueQuery.Builder builder, DbSession dbSession, SearchRequest request, Set<String> applicationUuids) {
    if (notInNewCodePeriod(request) || request.getPullRequest() != null) {
      return;
    }

    Set<String> projectUuids = applicationUuids.stream()
      .flatMap(app -> dbClient.componentDao().selectProjectsFromView(dbSession, app, app).stream())
      .collect(toSet());

    List<SnapshotDto> snapshots = getLastAnalysis(dbSession, projectUuids);

    Set<String> newCodeReferenceByProjects = snapshots
      .stream()
      .filter(s -> isLastAnalysisFromReAnalyzedReferenceBranch(dbSession, s))
      .map(SnapshotDto::getComponentUuid)
      .collect(toSet());

    Map<String, PeriodStart> leakByProjects = snapshots
      .stream()
      .filter(s -> s.getPeriodDate() != null && !isLastAnalysisFromReAnalyzedReferenceBranch(dbSession, s))
      .collect(uniqueIndex(SnapshotDto::getComponentUuid, s -> new PeriodStart(longToDate(s.getPeriodDate()), false)));

    builder.createdAfterByProjectUuids(leakByProjects);
    builder.newCodeOnReferenceByProjectUuids(newCodeReferenceByProjects);
  }

  private boolean isLastAnalysisFromReAnalyzedReferenceBranch(DbSession dbSession, SnapshotDto snapshot) {
    return isLastAnalysisUsingReferenceBranch(snapshot) &&
      isLastAnalysisFromSonarQube94Onwards(dbSession, snapshot.getComponentUuid());
  }

  private static void addDirectories(IssueQuery.Builder builder, List<ComponentDto> directories) {
    Set<String> paths = directories.stream().map(ComponentDto::path).collect(Collectors.toSet());
    builder.directories(paths);
  }

  private List<ComponentDto> getComponentsFromKeys(DbSession dbSession, Collection<String> componentKeys, @Nullable String branch, @Nullable String pullRequest) {
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

  private static String toProjectUuid(ComponentDto componentDto) {
    String mainBranchProjectUuid = componentDto.getMainBranchProjectUuid();
    return mainBranchProjectUuid == null ? componentDto.branchUuid() : mainBranchProjectUuid;
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
