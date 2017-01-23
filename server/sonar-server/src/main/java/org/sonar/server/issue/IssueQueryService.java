/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.joda.time.DateTime;
import org.joda.time.format.ISOPeriodFormat;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.server.component.ComponentService;
import org.sonar.server.rule.RuleKeyFunctions;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.RubyUtils;
import org.sonarqube.ws.client.issue.IssuesWsParameters;
import org.sonarqube.ws.client.issue.SearchWsRequest;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.sonar.api.utils.DateUtils.longToDate;
import static org.sonar.api.utils.DateUtils.parseDateOrDateTime;
import static org.sonar.api.utils.DateUtils.parseEndingDateOrDateTime;
import static org.sonar.api.utils.DateUtils.parseStartingDateOrDateTime;
import static org.sonar.db.component.ComponentDtoFunctions.toProjectUuid;
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
@ComputeEngineSide
public class IssueQueryService {

  public static final String LOGIN_MYSELF = "__me__";

  private static final String UNKNOWN = "<UNKNOWN>";
  private final DbClient dbClient;
  private final ComponentService componentService;
  private final System2 system;
  private final UserSession userSession;

  public IssueQueryService(DbClient dbClient, ComponentService componentService, System2 system, UserSession userSession) {
    this.dbClient = dbClient;
    this.componentService = componentService;
    this.system = system;
    this.userSession = userSession;
  }

  public IssueQuery createFromMap(Map<String, Object> params) {
    DbSession session = dbClient.openSession(false);
    try {

      IssueQuery.Builder builder = IssueQuery.builder(userSession)
        .issueKeys(RubyUtils.toStrings(params.get(IssuesWsParameters.PARAM_ISSUES)))
        .severities(RubyUtils.toStrings(params.get(IssuesWsParameters.PARAM_SEVERITIES)))
        .statuses(RubyUtils.toStrings(params.get(IssuesWsParameters.PARAM_STATUSES)))
        .resolutions(RubyUtils.toStrings(params.get(IssuesWsParameters.PARAM_RESOLUTIONS)))
        .resolved(RubyUtils.toBoolean(params.get(IssuesWsParameters.PARAM_RESOLVED)))
        .rules(toRules(params.get(IssuesWsParameters.PARAM_RULES)))
        .assignees(buildAssignees(RubyUtils.toStrings(params.get(IssuesWsParameters.PARAM_ASSIGNEES))))
        .languages(RubyUtils.toStrings(params.get(IssuesWsParameters.PARAM_LANGUAGES)))
        .tags(RubyUtils.toStrings(params.get(IssuesWsParameters.PARAM_TAGS)))
        .types(RubyUtils.toStrings(params.get(IssuesWsParameters.PARAM_TYPES)))
        .assigned(RubyUtils.toBoolean(params.get(IssuesWsParameters.PARAM_ASSIGNED)))
        .hideRules(RubyUtils.toBoolean(params.get(IssuesWsParameters.PARAM_HIDE_RULES)))
        .createdAt(RubyUtils.toDate(params.get(IssuesWsParameters.PARAM_CREATED_AT)))
        .createdAfter(buildCreatedAfterFromDates(RubyUtils.toDate(params.get(PARAM_CREATED_AFTER)), (String) params.get(PARAM_CREATED_IN_LAST)))
        .createdBefore(RubyUtils.toDate(parseEndingDateOrDateTime((String) params.get(IssuesWsParameters.PARAM_CREATED_BEFORE))));

      Set<String> allComponentUuids = Sets.newHashSet();
      boolean effectiveOnComponentOnly = mergeDeprecatedComponentParameters(session,
        RubyUtils.toBoolean(params.get(IssuesWsParameters.PARAM_ON_COMPONENT_ONLY)),
        RubyUtils.toStrings(params.get(IssuesWsParameters.PARAM_COMPONENTS)),
        RubyUtils.toStrings(params.get(IssuesWsParameters.PARAM_COMPONENT_UUIDS)),
        RubyUtils.toStrings(params.get(IssuesWsParameters.PARAM_COMPONENT_KEYS)),
        RubyUtils.toStrings(params.get(IssuesWsParameters.PARAM_COMPONENT_ROOT_UUIDS)),
        RubyUtils.toStrings(params.get(IssuesWsParameters.PARAM_COMPONENT_ROOTS)),
        allComponentUuids);

      addComponentParameters(builder, session,
        effectiveOnComponentOnly,
        allComponentUuids,
        RubyUtils.toStrings(params.get(IssuesWsParameters.PARAM_PROJECT_UUIDS)),
        RubyUtils.toStrings(
          ObjectUtils.defaultIfNull(
            params.get(IssuesWsParameters.PARAM_PROJECT_KEYS),
            params.get(IssuesWsParameters.PARAM_PROJECTS))),
        RubyUtils.toStrings(params.get(IssuesWsParameters.PARAM_MODULE_UUIDS)),
        RubyUtils.toStrings(params.get(IssuesWsParameters.PARAM_DIRECTORIES)),
        RubyUtils.toStrings(params.get(IssuesWsParameters.PARAM_FILE_UUIDS)),
        RubyUtils.toStrings(params.get(IssuesWsParameters.PARAM_AUTHORS)));

      String sort = (String) params.get(IssuesWsParameters.PARAM_SORT);
      if (!Strings.isNullOrEmpty(sort)) {
        builder.sort(sort);
        builder.asc(RubyUtils.toBoolean(params.get(IssuesWsParameters.PARAM_ASC)));
      }
      String facetMode = (String) params.get(IssuesWsParameters.FACET_MODE);
      if (!Strings.isNullOrEmpty(facetMode)) {
        builder.facetMode(facetMode);
      } else {
        builder.facetMode(IssuesWsParameters.FACET_MODE_COUNT);
      }
      return builder.build();

    } finally {
      session.close();
    }
  }

  @CheckForNull
  private Date buildCreatedAfterFromDates(@Nullable Date createdAfter, @Nullable String createdInLast) {
    checkArgument(createdAfter == null || createdInLast == null, format("%s and %s cannot be set simultaneously", PARAM_CREATED_AFTER, PARAM_CREATED_IN_LAST));

    Date actualCreatedAfter = createdAfter;
    if (createdInLast != null) {
      actualCreatedAfter = new DateTime(system.now()).minus(
        ISOPeriodFormat.standard().parsePeriod("P" + createdInLast.toUpperCase(Locale.ENGLISH))).toDate();
    }
    return actualCreatedAfter;
  }

  public IssueQuery createFromRequest(SearchWsRequest request) {
    DbSession session = dbClient.openSession(false);
    try {
      IssueQuery.Builder builder = IssueQuery.builder(userSession)
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
        .facetMode(request.getFacetMode());

      Set<String> allComponentUuids = Sets.newHashSet();
      boolean effectiveOnComponentOnly = mergeDeprecatedComponentParameters(session,
        request.getOnComponentOnly(),
        request.getComponents(),
        request.getComponentUuids(),
        request.getComponentKeys(),
        request.getComponentRootUuids(),
        request.getComponentRoots(),
        allComponentUuids);

      addComponentParameters(builder, session,
        effectiveOnComponentOnly,
        allComponentUuids,
        request.getProjectUuids(),
        request.getProjectKeys(),
        request.getModuleUuids(),
        request.getDirectories(),
        request.getFileUuids(),
        request.getAuthors());

      builder.createdAfter(buildCreatedAfterFromRequest(session, request, allComponentUuids));

      String sort = request.getSort();
      if (!Strings.isNullOrEmpty(sort)) {
        builder.sort(sort);
        builder.asc(request.getAsc());
      }
      return builder.build();

    } finally {
      session.close();
    }
  }

  private Date buildCreatedAfterFromRequest(DbSession dbSession, SearchWsRequest request, Set<String> componentUuids) {
    Date createdAfter = parseStartingDateOrDateTime(request.getCreatedAfter());
    String createdInLast = request.getCreatedInLast();

    if (request.getSinceLeakPeriod() == null || !request.getSinceLeakPeriod()) {
      return buildCreatedAfterFromDates(createdAfter, createdInLast);
    }

    checkRequest(createdAfter == null, "'%s' and '%s' cannot be set simultaneously", PARAM_CREATED_AFTER, PARAM_SINCE_LEAK_PERIOD);

    checkArgument(componentUuids.size() == 1, "One and only one component must be provided when searching since leak period");
    String uuid = componentUuids.iterator().next();
    // TODO use ComponentFinder instead
    Date createdAfterFromSnapshot = findCreatedAfterFromComponentUuid(dbSession, uuid);
    return buildCreatedAfterFromDates(createdAfterFromSnapshot, createdInLast);
  }

  @CheckForNull
  private Date findCreatedAfterFromComponentUuid(DbSession dbSession, String uuid) {
    ComponentDto component = checkFoundWithOptional(dbClient.componentDao().selectByUuid(dbSession, uuid), "Component with id '%s' not found", uuid);
    Optional<SnapshotDto> snapshot = dbClient.snapshotDao().selectLastAnalysisByComponentUuid(dbSession, component.uuid());
    if (snapshot.isPresent()) {
      return longToDate(snapshot.get().getPeriodDate(1));
    }
    return null;
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

  private boolean mergeDeprecatedComponentParameters(DbSession session, Boolean onComponentOnly,
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
      allComponentUuids.addAll(componentUuids(session, componentRoots));
      effectiveOnComponentOnly = false;
    } else if (components != null) {
      allComponentUuids.addAll(componentUuids(session, components));
      effectiveOnComponentOnly = true;
    } else if (componentUuids != null) {
      allComponentUuids.addAll(componentUuids);
      effectiveOnComponentOnly = BooleanUtils.isTrue(onComponentOnly);
    } else if (componentKeys != null) {
      allComponentUuids.addAll(componentUuids(session, componentKeys));
      effectiveOnComponentOnly = BooleanUtils.isTrue(onComponentOnly);
    }
    return effectiveOnComponentOnly;
  }

  private static boolean atMostOneNonNullElement(Object... objects) {
    return !from(Arrays.asList(objects))
      .filter(notNull())
      .anyMatch(new HasTwoOrMoreElements());
  }

  private void addComponentParameters(IssueQuery.Builder builder, DbSession session,
    boolean onComponentOnly,
    Collection<String> componentUuids,
    @Nullable Collection<String> projectUuids, @Nullable Collection<String> projects,
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
    checkArgument(projectUuids == null || projects == null, "projects and projectUuids cannot be set simultaneously");
    if (projectUuids != null) {
      builder.projectUuids(projectUuids);
    } else {
      builder.projectUuids(componentUuids(session, projects));
    }
    builder.moduleUuids(moduleUuids);
    builder.directories(directories);
    builder.fileUuids(fileUuids);

    if (!componentUuids.isEmpty()) {
      addComponentsBasedOnQualifier(builder, session, componentUuids, authors);
    }
  }

  protected void addComponentsBasedOnQualifier(IssueQuery.Builder builder, DbSession session, Collection<String> componentUuids, Collection<String> authors) {
    Set<String> qualifiers = componentService.getDistinctQualifiers(session, componentUuids);
    if (qualifiers.isEmpty()) {
      // Qualifier not found, defaulting to componentUuids (e.g <UNKNOWN>)
      builder.componentUuids(componentUuids);
      return;
    }
    if (qualifiers.size() > 1) {
      throw new IllegalArgumentException("All components must have the same qualifier, found " + Joiner.on(',').join(qualifiers));
    }

    String uniqueQualifier = qualifiers.iterator().next();
    switch (uniqueQualifier) {
      case Qualifiers.VIEW:
      case Qualifiers.SUBVIEW:
        addViewsOrSubViews(builder, componentUuids, uniqueQualifier);
        break;
      case "DEV":
        // XXX No constant for developer !!!
        Collection<String> actualAuthors = authorsFromParamsOrFromDeveloper(session, componentUuids, authors);
        builder.authors(actualAuthors);
        break;
      case "DEV_PRJ":
        addDeveloperTechnicalProjects(builder, session, componentUuids, authors);
        break;
      case Qualifiers.PROJECT:
        builder.projectUuids(componentUuids);
        break;
      case Qualifiers.MODULE:
        builder.moduleRootUuids(componentUuids);
        break;
      case Qualifiers.DIRECTORY:
        addDirectories(builder, session, componentUuids);
        break;
      case Qualifiers.FILE:
      case Qualifiers.UNIT_TEST_FILE:
        builder.fileUuids(componentUuids);
        break;
      default:
        throw new IllegalArgumentException("Unable to set search root context for components " + Joiner.on(',').join(componentUuids));
    }
  }

  private void addViewsOrSubViews(IssueQuery.Builder builder, Collection<String> componentUuids, String uniqueQualifier) {
    List<String> filteredViewUuids = newArrayList();
    for (String viewUuid : componentUuids) {
      if ((Qualifiers.VIEW.equals(uniqueQualifier) && userSession.hasComponentUuidPermission(UserRole.USER, viewUuid))
        || (Qualifiers.SUBVIEW.equals(uniqueQualifier) && userSession.hasComponentUuidPermission(UserRole.USER, viewUuid))) {
        filteredViewUuids.add(viewUuid);
      }
    }
    if (filteredViewUuids.isEmpty()) {
      filteredViewUuids.add(UNKNOWN);
    }
    builder.viewUuids(filteredViewUuids);
  }

  private void addDeveloperTechnicalProjects(IssueQuery.Builder builder, DbSession session, Collection<String> componentUuids, Collection<String> authors) {
    Collection<ComponentDto> technicalProjects = dbClient.componentDao().selectByUuids(session, componentUuids);
    Collection<String> developerUuids = Collections2.transform(technicalProjects, toProjectUuid());
    Collection<String> authorsFromProjects = authorsFromParamsOrFromDeveloper(session, developerUuids, authors);
    builder.authors(authorsFromProjects);
    Collection<String> projectUuids = Collections2.transform(technicalProjects, ComponentDto::getCopyResourceUuid);
    builder.projectUuids(projectUuids);
  }

  private Collection<String> authorsFromParamsOrFromDeveloper(DbSession session, Collection<String> componentUuids,
    @Nullable Collection<String> authors) {
    return authors == null ? dbClient.authorDao().selectScmAccountsByDeveloperUuids(session, componentUuids) : authors;
  }

  private void addDirectories(IssueQuery.Builder builder, DbSession session, Collection<String> componentUuids) {
    Collection<String> directoryModuleUuids = Sets.newHashSet();
    Collection<String> directoryPaths = Sets.newHashSet();
    for (ComponentDto directory : componentService.getByUuids(session, componentUuids)) {
      directoryModuleUuids.add(directory.moduleUuid());
      directoryPaths.add(directory.path());
    }
    builder.moduleUuids(directoryModuleUuids);
    builder.directories(directoryPaths);
  }

  private Collection<String> componentUuids(DbSession session, @Nullable Collection<String> componentKeys) {
    Collection<String> componentUuids = Lists.newArrayList();
    componentUuids.addAll(componentService.componentUuids(session, componentKeys, true));
    // If unknown components are given, but no components are found, then all issues will be returned,
    // so we add this hack in order to return no issue in this case.
    if (componentKeys != null && !componentKeys.isEmpty() && componentUuids.isEmpty()) {
      componentUuids.add(UNKNOWN);
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
      return newArrayList(Iterables.transform(rules, RuleKeyFunctions.stringToRuleKey()));
    }
    return null;
  }

  private static class HasTwoOrMoreElements implements Predicate<Object> {
    private AtomicInteger counter;

    private HasTwoOrMoreElements() {
      this.counter = new AtomicInteger();
    }

    @Override
    public boolean apply(@Nonnull Object input) {
      Objects.requireNonNull(input);
      return counter.incrementAndGet() >= 2;
    }
  }
}
