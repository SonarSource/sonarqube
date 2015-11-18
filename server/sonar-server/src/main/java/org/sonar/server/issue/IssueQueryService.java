/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.joda.time.DateTime;
import org.joda.time.format.ISOPeriodFormat;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.rule.RuleKeyFunctions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentService;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.RubyUtils;
import org.sonarqube.ws.client.issue.IssueFilterParameters;
import org.sonarqube.ws.client.issue.SearchWsRequest;

import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.db.component.ComponentDtoFunctions.toCopyResourceId;
import static org.sonar.db.component.ComponentDtoFunctions.toProjectUuid;
import static org.sonar.db.component.ComponentDtoFunctions.toUuid;

/**
 * This component is used to create an IssueQuery, in order to transform the component and component roots keys into uuid.
 */
@ServerSide
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
        .issueKeys(RubyUtils.toStrings(params.get(IssueFilterParameters.ISSUES)))
        .severities(RubyUtils.toStrings(params.get(IssueFilterParameters.SEVERITIES)))
        .statuses(RubyUtils.toStrings(params.get(IssueFilterParameters.STATUSES)))
        .resolutions(RubyUtils.toStrings(params.get(IssueFilterParameters.RESOLUTIONS)))
        .resolved(RubyUtils.toBoolean(params.get(IssueFilterParameters.RESOLVED)))
        .rules(toRules(params.get(IssueFilterParameters.RULES)))
        .actionPlans(RubyUtils.toStrings(params.get(IssueFilterParameters.ACTION_PLANS)))
        .reporters(RubyUtils.toStrings(params.get(IssueFilterParameters.REPORTERS)))
        .assignees(buildAssignees(RubyUtils.toStrings(params.get(IssueFilterParameters.ASSIGNEES))))
        .languages(RubyUtils.toStrings(params.get(IssueFilterParameters.LANGUAGES)))
        .tags(RubyUtils.toStrings(params.get(IssueFilterParameters.TAGS)))
        .assigned(RubyUtils.toBoolean(params.get(IssueFilterParameters.ASSIGNED)))
        .planned(RubyUtils.toBoolean(params.get(IssueFilterParameters.PLANNED)))
        .hideRules(RubyUtils.toBoolean(params.get(IssueFilterParameters.HIDE_RULES)))
        .createdAt(RubyUtils.toDate(params.get(IssueFilterParameters.CREATED_AT)))
        .createdAfter(buildCreatedAfter(RubyUtils.toDate(params.get(IssueFilterParameters.CREATED_AFTER)), (String) params.get(IssueFilterParameters.CREATED_IN_LAST)))
        .createdBefore(RubyUtils.toDate(params.get(IssueFilterParameters.CREATED_BEFORE)));

      Set<String> allComponentUuids = Sets.newHashSet();
      boolean effectiveOnComponentOnly = mergeDeprecatedComponentParameters(session,
        RubyUtils.toBoolean(params.get(IssueFilterParameters.ON_COMPONENT_ONLY)),
        RubyUtils.toStrings(params.get(IssueFilterParameters.COMPONENTS)),
        RubyUtils.toStrings(params.get(IssueFilterParameters.COMPONENT_UUIDS)),
        RubyUtils.toStrings(params.get(IssueFilterParameters.COMPONENT_KEYS)),
        RubyUtils.toStrings(params.get(IssueFilterParameters.COMPONENT_ROOT_UUIDS)),
        RubyUtils.toStrings(params.get(IssueFilterParameters.COMPONENT_ROOTS)),
        allComponentUuids);

      addComponentParameters(builder, session,
        effectiveOnComponentOnly,
        allComponentUuids,
        RubyUtils.toStrings(params.get(IssueFilterParameters.PROJECT_UUIDS)),
        RubyUtils.toStrings(
          ObjectUtils.defaultIfNull(
            params.get(IssueFilterParameters.PROJECT_KEYS),
            params.get(IssueFilterParameters.PROJECTS))),
        RubyUtils.toStrings(params.get(IssueFilterParameters.MODULE_UUIDS)),
        RubyUtils.toStrings(params.get(IssueFilterParameters.DIRECTORIES)),
        RubyUtils.toStrings(params.get(IssueFilterParameters.FILE_UUIDS)),
        RubyUtils.toStrings(params.get(IssueFilterParameters.AUTHORS)));

      String sort = (String) params.get(IssueFilterParameters.SORT);
      if (!Strings.isNullOrEmpty(sort)) {
        builder.sort(sort);
        builder.asc(RubyUtils.toBoolean(params.get(IssueFilterParameters.ASC)));
      }
      String facetMode = (String) params.get(IssueFilterParameters.FACET_MODE);
      if (!Strings.isNullOrEmpty(facetMode)) {
        builder.facetMode(facetMode);
      } else {
        builder.facetMode(IssueFilterParameters.FACET_MODE_COUNT);
      }
      return builder.build();

    } finally {
      session.close();
    }
  }

  private Date buildCreatedAfter(@Nullable Date createdAfter, @Nullable String createdSince) {
    Preconditions.checkArgument(createdAfter == null || createdSince == null, "createdAfter and createdSince cannot be set simultaneously");

    Date actualCreatedAfter = createdAfter;
    if (createdSince != null) {
      actualCreatedAfter = new DateTime(system.now()).minus(ISOPeriodFormat.standard().parsePeriod("P" + createdSince.toUpperCase())).toDate();
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
        .actionPlans(request.getActionPlans())
        .reporters(request.getReporters())
        .assignees(buildAssignees(request.getAssignees()))
        .languages(request.getLanguages())
        .tags(request.getTags())
        .assigned(request.getAssigned())
        .planned(request.getPlanned())
        .createdAt(parseAsDateTime(request.getCreatedAt()))
        .createdAfter(buildCreatedAfter(parseAsDateTime(request.getCreatedAfter()), request.getCreatedInLast()))
        .createdBefore(parseAsDateTime(request.getCreatedBefore()))
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

    failIfBothParametersSet(componentRootUuids, componentRoots, "componentRoots and componentRootUuids cannot be set simultaneously");
    failIfBothParametersSet(componentUuids, components, "components and componentUuids cannot be set simultaneously");
    failIfBothParametersSet(componentKeys, componentUuids, "componentKeys and componentUuids cannot be set simultaneously");

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

  private void failIfBothParametersSet(@Nullable Collection<String> uuids, @Nullable Collection<String> keys, String message) {
    if (uuids != null && keys != null) {
      throw new IllegalArgumentException(message);
    }
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
    failIfBothParametersSet(projectUuids, projects, "projects and projectUuids cannot be set simultaneously");
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
      if ((Qualifiers.VIEW.equals(uniqueQualifier) && userSession.hasProjectPermissionByUuid(UserRole.USER, viewUuid))
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
    Collection<Long> projectIds = Collections2.transform(technicalProjects, toCopyResourceId());
    List<ComponentDto> originalProjects = dbClient.componentDao().selectByIds(session, projectIds);
    Collection<String> projectUuids = Collections2.transform(originalProjects, toUuid());
    builder.projectUuids(projectUuids);
  }

  private Collection<String> authorsFromParamsOrFromDeveloper(DbSession session, Collection<String> componentUuids, Collection<String> authors) {
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

  @CheckForNull
  private static Date parseAsDateTime(@Nullable String stringDate) {
    if (stringDate != null) {
      try {
        return DateUtils.parseDateTime(stringDate);
      } catch (SonarException notDateTime) {
        try {
          return DateUtils.parseDate(stringDate);
        } catch (SonarException notDateEither) {
          throw new SonarException(String.format("'%s' cannot be parsed as either a date or date+time", stringDate));
        }
      }
    }
    return null;
  }
}
