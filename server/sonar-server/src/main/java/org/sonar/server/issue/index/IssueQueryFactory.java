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
package org.sonar.server.issue.index;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.Period;
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
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.BooleanUtils;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.issue.SearchRequest;
import org.sonar.server.issue.index.IssueQuery.PeriodStart;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Collections2.transform;
import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.sonar.api.utils.DateUtils.longToDate;
import static org.sonar.api.utils.DateUtils.parseDateOrDateTime;
import static org.sonar.api.utils.DateUtils.parseEndingDateOrDateTime;
import static org.sonar.api.utils.DateUtils.parseStartingDateOrDateTime;
import static org.sonar.core.util.stream.MoreCollectors.toHashSet;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_KEYS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_AFTER;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_IN_LAST;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SINCE_LEAK_PERIOD;

/**
 * This component is used to create an IssueQuery, in order to transform the component and component roots keys into uuid.
 */
@ServerSide
public class IssueQueryFactory {

  public static final String UNKNOWN = "<UNKNOWN>";
  private static final ComponentDto UNKNOWN_COMPONENT = new ComponentDto().setUuid(UNKNOWN).setProjectUuid(UNKNOWN);

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
      IssueQuery.Builder builder = IssueQuery.builder()
        .issueKeys(request.getIssues())
        .severities(request.getSeverities())
        .statuses(request.getStatuses())
        .resolutions(request.getResolutions())
        .resolved(request.getResolved())
        .rules(ruleKeysToRuleId(dbSession, request.getRules()))
        .assigneeUuids(request.getAssigneeUuids())
        .authors(request.getAuthors())
        .languages(request.getLanguages())
        .tags(request.getTags())
        .types(request.getTypes())
        .owaspTop10(request.getOwaspTop10())
        .sansTop25(request.getSansTop25())
        .cwe(request.getCwe())
        .assigned(request.getAssigned())
        .createdAt(parseDateOrDateTime(request.getCreatedAt()))
        .createdBefore(parseEndingDateOrDateTime(request.getCreatedBefore()))
        .facetMode(request.getFacetMode())
        .organizationUuid(convertOrganizationKeyToUuid(dbSession, request.getOrganization()));

      List<ComponentDto> allComponents = new ArrayList<>();
      boolean effectiveOnComponentOnly = mergeDeprecatedComponentParameters(dbSession, request, allComponents);
      addComponentParameters(builder, dbSession, effectiveOnComponentOnly, allComponents, request);

      setCreatedAfterFromRequest(dbSession, builder, request, allComponents);
      String sort = request.getSort();
      if (!Strings.isNullOrEmpty(sort)) {
        builder.sort(sort);
        builder.asc(request.getAsc());
      }
      return builder.build();
    }
  }

  private void setCreatedAfterFromDates(IssueQuery.Builder builder, @Nullable Date createdAfter, @Nullable String createdInLast, boolean createdAfterInclusive) {
    checkArgument(createdAfter == null || createdInLast == null, format("Parameters %s and %s cannot be set simultaneously", PARAM_CREATED_AFTER, PARAM_CREATED_IN_LAST));

    Date actualCreatedAfter = createdAfter;
    if (createdInLast != null) {
      actualCreatedAfter = Date.from(
        OffsetDateTime.now(clock)
          .minus(Period.parse("P" + createdInLast.toUpperCase(Locale.ENGLISH)))
          .toInstant());
    }
    builder.createdAfter(actualCreatedAfter, createdAfterInclusive);
  }

  @CheckForNull
  private String convertOrganizationKeyToUuid(DbSession dbSession, @Nullable String organizationKey) {
    if (organizationKey == null) {
      return null;
    }
    Optional<OrganizationDto> organization = dbClient.organizationDao().selectByKey(dbSession, organizationKey);
    return organization.map(OrganizationDto::getUuid).orElse(UNKNOWN);
  }

  private void setCreatedAfterFromRequest(DbSession dbSession, IssueQuery.Builder builder, SearchRequest request, List<ComponentDto> componentUuids) {
    Date createdAfter = parseStartingDateOrDateTime(request.getCreatedAfter());
    String createdInLast = request.getCreatedInLast();

    if (request.getSinceLeakPeriod() == null || !request.getSinceLeakPeriod()) {
      setCreatedAfterFromDates(builder, createdAfter, createdInLast, true);
    } else {
      checkArgument(createdAfter == null, "Parameters '%s' and '%s' cannot be set simultaneously", PARAM_CREATED_AFTER, PARAM_SINCE_LEAK_PERIOD);
      checkArgument(componentUuids.size() == 1, "One and only one component must be provided when searching since leak period");
      ComponentDto component = componentUuids.iterator().next();
      Date createdAfterFromSnapshot = findCreatedAfterFromComponentUuid(dbSession, component);
      setCreatedAfterFromDates(builder, createdAfterFromSnapshot, createdInLast, false);
    }
  }

  @CheckForNull
  private Date findCreatedAfterFromComponentUuid(DbSession dbSession, ComponentDto component) {
    Optional<SnapshotDto> snapshot = dbClient.snapshotDao().selectLastAnalysisByComponentUuid(dbSession, component.uuid());
    return snapshot.map(s -> longToDate(s.getPeriodDate())).orElse(null);
  }

  private boolean mergeDeprecatedComponentParameters(DbSession session, SearchRequest request, List<ComponentDto> allComponents) {
    Boolean onComponentOnly = request.getOnComponentOnly();
    Collection<String> components = request.getComponents();
    Collection<String> componentUuids = request.getComponentUuids();
    Collection<String> componentKeys = request.getComponentKeys();
    Collection<String> componentRootUuids = request.getComponentRootUuids();
    Collection<String> componentRoots = request.getComponentRoots();
    String branch = request.getBranch();
    String pullRequest = request.getPullRequest();

    boolean effectiveOnComponentOnly = false;

    checkArgument(atMostOneNonNullElement(components, componentUuids, componentKeys, componentRootUuids, componentRoots),
      "At most one of the following parameters can be provided: %s and %s", PARAM_COMPONENT_KEYS, PARAM_COMPONENT_UUIDS);

    if (componentRootUuids != null) {
      allComponents.addAll(getComponentsFromUuids(session, componentRootUuids));
    } else if (componentRoots != null) {
      allComponents.addAll(getComponentsFromKeys(session, componentRoots, branch, pullRequest));
    } else if (components != null) {
      allComponents.addAll(getComponentsFromKeys(session, components, branch, pullRequest));
      effectiveOnComponentOnly = true;
    } else if (componentUuids != null) {
      allComponents.addAll(getComponentsFromUuids(session, componentUuids));
      effectiveOnComponentOnly = BooleanUtils.isTrue(onComponentOnly);
    } else if (componentKeys != null) {
      allComponents.addAll(getComponentsFromKeys(session, componentKeys, branch, pullRequest));
      effectiveOnComponentOnly = BooleanUtils.isTrue(onComponentOnly);
    }

    return effectiveOnComponentOnly;
  }

  private static boolean atMostOneNonNullElement(Object... objects) {
    return Arrays.stream(objects)
      .filter(Objects::nonNull)
      .count() <= 1;
  }

  private void addComponentParameters(IssueQuery.Builder builder, DbSession session, boolean onComponentOnly, List<ComponentDto> components, SearchRequest request) {
    builder.onComponentOnly(onComponentOnly);
    if (onComponentOnly) {
      builder.componentUuids(components.stream().map(ComponentDto::uuid).collect(toList()));
      setBranch(builder, components.get(0), request.getBranch(), request.getPullRequest());
      return;
    }

    List<String> projectKeys = request.getProjectKeys();
    if (projectKeys != null) {
      List<ComponentDto> projects = getComponentsFromKeys(session, projectKeys, request.getBranch(), request.getPullRequest());
      builder.projectUuids(projects.stream().map(IssueQueryFactory::toProjectUuid).collect(toList()));
      setBranch(builder, projects.get(0), request.getBranch(), request.getPullRequest());
    }
    builder.moduleUuids(request.getModuleUuids());
    builder.directories(request.getDirectories());
    builder.fileUuids(request.getFileUuids());

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

    setBranch(builder, components.get(0), request.getBranch(), request.getPullRequest());
    String qualifier = qualifiers.iterator().next();
    switch (qualifier) {
      case Qualifiers.VIEW:
      case Qualifiers.SUBVIEW:
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
      case Qualifiers.FILE:
      case Qualifiers.UNIT_TEST_FILE:
        builder.fileUuids(components.stream().map(ComponentDto::uuid).collect(toList()));
        break;
      default:
        throw new IllegalArgumentException("Unable to set search root context for components " + Joiner.on(',').join(components));
    }
  }

  private void addProjectUuidsForApplication(IssueQuery.Builder builder, DbSession session, SearchRequest request) {
    List<String> projectKeys = request.getProjectKeys();
    if (projectKeys != null) {
      // On application, branch should only be applied on the application, not on projects
      List<ComponentDto> projects = getComponentsFromKeys(session, projectKeys, null, null);
      builder.projectUuids(projects.stream().map(ComponentDto::uuid).collect(toList()));
    }
  }

  private void addViewsOrSubViews(IssueQuery.Builder builder, Collection<ComponentDto> viewOrSubViewUuids) {
    List<String> filteredViewUuids = viewOrSubViewUuids.stream()
      .filter(uuid -> userSession.hasComponentPermission(UserRole.USER, uuid))
      .map(ComponentDto::uuid)
      .collect(Collectors.toList());
    if (filteredViewUuids.isEmpty()) {
      filteredViewUuids.add(UNKNOWN);
    }
    builder.viewUuids(filteredViewUuids);
  }

  private void addApplications(IssueQuery.Builder builder, DbSession dbSession, List<ComponentDto> applications, SearchRequest request) {
    Set<String> authorizedApplicationUuids = applications.stream()
      .filter(app -> userSession.hasComponentPermission(UserRole.USER, app))
      .map(ComponentDto::uuid)
      .collect(toSet());

    builder.viewUuids(authorizedApplicationUuids.isEmpty() ? singleton(UNKNOWN) : authorizedApplicationUuids);
    addCreatedAfterByProjects(builder, dbSession, request, authorizedApplicationUuids);
  }

  private void addCreatedAfterByProjects(IssueQuery.Builder builder, DbSession dbSession, SearchRequest request, Set<String> applicationUuids) {
    if (request.getSinceLeakPeriod() == null || !request.getSinceLeakPeriod()) {
      return;
    }

    Set<String> projectUuids = applicationUuids.stream()
      .flatMap(app -> dbClient.componentDao().selectProjectsFromView(dbSession, app, app).stream())
      .collect(toSet());

    Map<String, PeriodStart> leakByProjects = dbClient.snapshotDao().selectLastAnalysesByRootComponentUuids(dbSession, projectUuids)
      .stream()
      .filter(s -> s.getPeriodDate() != null)
      .collect(uniqueIndex(SnapshotDto::getComponentUuid, s -> new PeriodStart(longToDate(s.getPeriodDate()), false)));
    builder.createdAfterByProjectUuids(leakByProjects);
  }

  private static void addDirectories(IssueQuery.Builder builder, List<ComponentDto> directories) {
    Collection<String> directoryModuleUuids = new HashSet<>();
    Collection<String> directoryPaths = new HashSet<>();
    for (ComponentDto directory : directories) {
      directoryModuleUuids.add(directory.moduleUuid());
      directoryPaths.add(directory.path());
    }
    builder.moduleUuids(directoryModuleUuids);
    builder.directories(directoryPaths);
  }

  private List<ComponentDto> getComponentsFromKeys(DbSession dbSession, Collection<String> componentKeys, @Nullable String branch, @Nullable String pullRequest) {
    List<ComponentDto> componentDtos;
    if (branch != null) {
      componentDtos = dbClient.componentDao().selectByKeysAndBranch(dbSession, componentKeys, branch);
    } else if (pullRequest != null) {
      componentDtos = dbClient.componentDao().selectByKeysAndPullRequest(dbSession, componentKeys, pullRequest);
    } else {
      componentDtos = dbClient.componentDao().selectByKeys(dbSession, componentKeys);
    }
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

  @CheckForNull
  private Collection<RuleDefinitionDto> ruleKeysToRuleId(DbSession dbSession, @Nullable Collection<String> rules) {
    if (rules != null) {
      return dbClient.ruleDao().selectDefinitionByKeys(dbSession, transform(rules, RuleKey::parse));
    }
    return Collections.emptyList();
  }

  private static String toProjectUuid(ComponentDto componentDto) {
    String mainBranchProjectUuid = componentDto.getMainBranchProjectUuid();
    return mainBranchProjectUuid == null ? componentDto.projectUuid() : mainBranchProjectUuid;
  }

  private static void setBranch(IssueQuery.Builder builder, ComponentDto component, @Nullable String branch, @Nullable String pullRequest) {
    builder.branchUuid(branch == null && pullRequest == null ? null : component.projectUuid());
    builder.mainBranch(UNKNOWN_COMPONENT.equals(component)
      || (branch == null && pullRequest == null)
      || (branch != null && !branch.equals(component.getBranch()))
      || (pullRequest != null && !pullRequest.equals(component.getPullRequest())));
  }
}
