/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.BooleanUtils;
import org.joda.time.DateTime;
import org.joda.time.format.ISOPeriodFormat;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.client.issue.SearchWsRequest;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.sonar.api.utils.DateUtils.longToDate;
import static org.sonar.api.utils.DateUtils.parseDateOrDateTime;
import static org.sonar.api.utils.DateUtils.parseEndingDateOrDateTime;
import static org.sonar.api.utils.DateUtils.parseStartingDateOrDateTime;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_KEYS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_ROOTS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_UUIDS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_AFTER;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_IN_LAST;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_SINCE_LEAK_PERIOD;

/**
 * This component is used to create an IssueQuery, in order to transform the component and component roots keys into uuid.
 */
@ServerSide
public class IssueQueryFactory {

  public static final String LOGIN_MYSELF = "__me__";

  private static final String UNKNOWN = "<UNKNOWN>";
  private final DbClient dbClient;
  private final System2 system;
  private final UserSession userSession;

  public IssueQueryFactory(DbClient dbClient, System2 system, UserSession userSession) {
    this.dbClient = dbClient;
    this.system = system;
    this.userSession = userSession;
  }

  public IssueQuery create(SearchWsRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      IssueQuery.Builder builder = IssueQuery.builder()
        .issueKeys(request.getIssues())
        .severities(request.getSeverities())
        .statuses(request.getStatuses())
        .resolutions(request.getResolutions())
        .resolved(request.getResolved())
        .rules(stringsToRules(request.getRules()))
        .assignees(buildAssignees(request.getAssignees()))
        .languages(request.getLanguages())
        .tags(request.getTags())
        .types(request.getTypes())
        .assigned(request.getAssigned())
        .createdAt(parseDateOrDateTime(request.getCreatedAt()))
        .createdBefore(parseEndingDateOrDateTime(request.getCreatedBefore()))
        .facetMode(request.getFacetMode())
        .organizationUuid(convertOrganizationKeyToUuid(dbSession, request.getOrganization()));

      Set<String> allComponentUuids = Sets.newHashSet();
      boolean effectiveOnComponentOnly = mergeDeprecatedComponentParameters(dbSession,
        request.getOnComponentOnly(),
        request.getComponents(),
        request.getComponentUuids(),
        request.getComponentKeys(),
        request.getComponentRootUuids(),
        request.getComponentRoots(),
        allComponentUuids);

      addComponentParameters(builder, dbSession,
        effectiveOnComponentOnly,
        allComponentUuids,
        request.getProjectUuids(),
        request.getProjectKeys(),
        request.getModuleUuids(),
        request.getDirectories(),
        request.getFileUuids(),
        request.getAuthors());

      builder.createdAfter(buildCreatedAfterFromRequest(dbSession, request, allComponentUuids));

      String sort = request.getSort();
      if (!Strings.isNullOrEmpty(sort)) {
        builder.sort(sort);
        builder.asc(request.getAsc());
      }
      return builder.build();

    }
  }

  @CheckForNull
  private Date buildCreatedAfterFromDates(@Nullable Date createdAfter, @Nullable String createdInLast) {
    checkArgument(createdAfter == null || createdInLast == null, format("Parameters %s and %s cannot be set simultaneously", PARAM_CREATED_AFTER, PARAM_CREATED_IN_LAST));

    Date actualCreatedAfter = createdAfter;
    if (createdInLast != null) {
      actualCreatedAfter = new DateTime(system.now()).minus(
        ISOPeriodFormat.standard().parsePeriod("P" + createdInLast.toUpperCase(Locale.ENGLISH))).toDate();
    }
    return actualCreatedAfter;
  }

  @CheckForNull
  private String convertOrganizationKeyToUuid(DbSession dbSession, @Nullable String organizationKey) {
    if (organizationKey == null) {
      return null;
    }
    Optional<OrganizationDto> organization = dbClient.organizationDao().selectByKey(dbSession, organizationKey);
    return organization.map(OrganizationDto::getUuid).orElse(UNKNOWN);
  }

  private Date buildCreatedAfterFromRequest(DbSession dbSession, SearchWsRequest request, Set<String> componentUuids) {
    Date createdAfter = parseStartingDateOrDateTime(request.getCreatedAfter());
    String createdInLast = request.getCreatedInLast();

    if (request.getSinceLeakPeriod() == null || !request.getSinceLeakPeriod()) {
      return buildCreatedAfterFromDates(createdAfter, createdInLast);
    }

    checkRequest(createdAfter == null, "Parameters '%s' and '%s' cannot be set simultaneously", PARAM_CREATED_AFTER, PARAM_SINCE_LEAK_PERIOD);

    checkArgument(componentUuids.size() == 1, "One and only one component must be provided when searching since leak period");
    String uuid = componentUuids.iterator().next();
    Date createdAfterFromSnapshot = findCreatedAfterFromComponentUuid(dbSession, uuid);
    return buildCreatedAfterFromDates(createdAfterFromSnapshot, createdInLast);
  }

  @CheckForNull
  private Date findCreatedAfterFromComponentUuid(DbSession dbSession, String uuid) {
    ComponentDto component = checkFoundWithOptional(dbClient.componentDao().selectByUuid(dbSession, uuid), "Component with id '%s' not found", uuid);
    Optional<SnapshotDto> snapshot = dbClient.snapshotDao().selectLastAnalysisByComponentUuid(dbSession, component.uuid());
    return snapshot.map(s -> longToDate(s.getPeriodDate())).orElse(null);
  }

  private List<String> buildAssignees(@Nullable List<String> assigneesFromParams) {
    List<String> assignees = Lists.newArrayList();
    if (assigneesFromParams != null) {
      assignees.addAll(assigneesFromParams);
    }
    if (assignees.contains(LOGIN_MYSELF)) {
      String login = userSession.getLogin();
      if (login == null) {
        assignees.add(UNKNOWN);
      } else {
        assignees.add(login);
      }
    }
    return assignees;
  }

  private boolean mergeDeprecatedComponentParameters(DbSession session, @Nullable Boolean onComponentOnly,
    @Nullable Collection<String> components,
    @Nullable Collection<String> componentUuids,
    @Nullable Collection<String> componentKeys,
    @Nullable Collection<String> componentRootUuids,
    @Nullable Collection<String> componentRoots,
    Set<String> allComponentUuids) {
    boolean effectiveOnComponentOnly = false;

    checkArgument(atMostOneNonNullElement(components, componentUuids, componentKeys, componentRootUuids, componentRoots),
      "At most one of the following parameters can be provided: %s, %s, %s, %s, %s",
      PARAM_COMPONENT_KEYS, PARAM_COMPONENT_UUIDS, PARAM_COMPONENTS, PARAM_COMPONENT_ROOTS, PARAM_COMPONENT_UUIDS);

    if (componentRootUuids != null) {
      allComponentUuids.addAll(componentRootUuids);
      effectiveOnComponentOnly = false;
    } else if (componentRoots != null) {
      allComponentUuids.addAll(convertComponentKeysToUuids(session, componentRoots));
      effectiveOnComponentOnly = false;
    } else if (components != null) {
      allComponentUuids.addAll(convertComponentKeysToUuids(session, components));
      effectiveOnComponentOnly = true;
    } else if (componentUuids != null) {
      allComponentUuids.addAll(componentUuids);
      effectiveOnComponentOnly = BooleanUtils.isTrue(onComponentOnly);
    } else if (componentKeys != null) {
      allComponentUuids.addAll(convertComponentKeysToUuids(session, componentKeys));
      effectiveOnComponentOnly = BooleanUtils.isTrue(onComponentOnly);
    }
    return effectiveOnComponentOnly;
  }

  private static boolean atMostOneNonNullElement(Object... objects) {
    return Arrays.stream(objects)
      .filter(Objects::nonNull)
      .count() <= 1;
  }

  private void addComponentParameters(IssueQuery.Builder builder, DbSession session,
    boolean onComponentOnly,
    Collection<String> componentUuids,
    @Nullable Collection<String> projectUuids, @Nullable Collection<String> projectKeys,
    @Nullable Collection<String> moduleUuids,
    @Nullable Collection<String> directories,
    @Nullable Collection<String> fileUuids,
    @Nullable Collection<String> authors) {

    builder.onComponentOnly(onComponentOnly);
    if (onComponentOnly) {
      builder.componentUuids(componentUuids);
      return;
    }

    builder.authors(authors);
    checkArgument(projectUuids == null || projectKeys == null, "Parameters projects and projectUuids cannot be set simultaneously");
    if (projectUuids != null) {
      builder.projectUuids(projectUuids);
    } else if (projectKeys != null) {
      builder.projectUuids(convertComponentKeysToUuids(session, projectKeys));
    }
    builder.moduleUuids(moduleUuids);
    builder.directories(directories);
    builder.fileUuids(fileUuids);

    if (!componentUuids.isEmpty()) {
      addComponentsBasedOnQualifier(builder, session, componentUuids);
    }
  }

  private void addComponentsBasedOnQualifier(IssueQuery.Builder builder, DbSession session, Collection<String> componentUuids) {
    if (componentUuids.isEmpty()) {
      builder.componentUuids(componentUuids);
      return;
    }

    List<ComponentDto> components = dbClient.componentDao().selectByUuids(session, componentUuids);
    if (components.isEmpty()) {
      builder.componentUuids(componentUuids);
      return;
    }

    Set<String> qualifiers = components.stream().map(ComponentDto::qualifier).collect(MoreCollectors.toHashSet());
    if (qualifiers.size() > 1) {
      throw new IllegalArgumentException("All components must have the same qualifier, found " + Joiner.on(',').join(qualifiers));
    }

    String qualifier = qualifiers.iterator().next();
    switch (qualifier) {
      case Qualifiers.VIEW:
      case Qualifiers.SUBVIEW:
      case Qualifiers.APP:
        addViewsOrSubViews(builder, componentUuids);
        break;
      case Qualifiers.PROJECT:
        builder.projectUuids(componentUuids);
        break;
      case Qualifiers.MODULE:
        builder.moduleRootUuids(componentUuids);
        break;
      case Qualifiers.DIRECTORY:
        addDirectories(builder, components);
        break;
      case Qualifiers.FILE:
      case Qualifiers.UNIT_TEST_FILE:
        builder.fileUuids(componentUuids);
        break;
      default:
        throw new IllegalArgumentException("Unable to set search root context for components " + Joiner.on(',').join(componentUuids));
    }
  }

  private void addViewsOrSubViews(IssueQuery.Builder builder, Collection<String> viewOrSubViewUuids) {
    List<String> filteredViewUuids = viewOrSubViewUuids.stream()
      .filter(uuid -> userSession.hasComponentUuidPermission(UserRole.USER, uuid))
      .collect(Collectors.toList());

    if (filteredViewUuids.isEmpty()) {
      filteredViewUuids.add(UNKNOWN);
    }
    builder.viewUuids(filteredViewUuids);
  }

  private static void addDirectories(IssueQuery.Builder builder, List<ComponentDto> directories) {
    Collection<String> directoryModuleUuids = Sets.newHashSet();
    Collection<String> directoryPaths = Sets.newHashSet();
    for (ComponentDto directory : directories) {
      directoryModuleUuids.add(directory.moduleUuid());
      directoryPaths.add(directory.path());
    }
    builder.moduleUuids(directoryModuleUuids);
    builder.directories(directoryPaths);
  }

  private Collection<String> convertComponentKeysToUuids(DbSession dbSession, Collection<String> componentKeys) {
    List<String> componentUuids = dbClient.componentDao().selectByKeys(dbSession, componentKeys).stream().map(ComponentDto::uuid).collect(MoreCollectors.toList());
    // If unknown components are given, but no components are found, then all issues will be returned,
    // so we add this hack in order to return no issue in this case.
    if (!componentKeys.isEmpty() && componentUuids.isEmpty()) {
      return singletonList(UNKNOWN);
    }
    return componentUuids;
  }

  @VisibleForTesting
  static Collection<RuleKey> toRules(@Nullable Object o) {
    Collection<RuleKey> result = null;
    if (o != null) {
      if (o instanceof List) {
        // assume that it contains only strings
        result = stringsToRules((List<String>) o);
      } else if (o instanceof String) {
        result = stringsToRules(newArrayList(Splitter.on(',').omitEmptyStrings().split((String) o)));
      }
    }
    return result;
  }

  @CheckForNull
  private static Collection<RuleKey> stringsToRules(@Nullable Collection<String> rules) {
    if (rules != null) {
      return Collections2.transform(rules, RuleKey::parse);
    }
    return null;
  }
}
