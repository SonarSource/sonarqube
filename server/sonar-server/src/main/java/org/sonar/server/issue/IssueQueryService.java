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
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.Request;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.filter.IssueFilterParameters;
import org.sonar.server.search.ws.SearchRequestHandler;
import org.sonar.server.util.RubyUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

/**
 * This component is used to create an IssueQuery, in order to transform the component and component roots keys into uuid.
 */
public class IssueQueryService implements ServerComponent {

  private final DbClient dbClient;

  public IssueQueryService(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public IssueQuery createFromMap(Map<String, Object> params) {
    DbSession session = dbClient.openSession(false);
    try {
      IssueQuery.Builder builder = IssueQuery.builder()
        .issueKeys(RubyUtils.toStrings(params.get(IssueFilterParameters.ISSUES)))
        .severities(RubyUtils.toStrings(params.get(IssueFilterParameters.SEVERITIES)))
        .statuses(RubyUtils.toStrings(params.get(IssueFilterParameters.STATUSES)))
        .resolutions(RubyUtils.toStrings(params.get(IssueFilterParameters.RESOLUTIONS)))
        .resolved(RubyUtils.toBoolean(params.get(IssueFilterParameters.RESOLVED)))
        .components(componentUuids(session, RubyUtils.toStrings(params.get(IssueFilterParameters.COMPONENTS))))
        .componentRoots(componentUuids(session, RubyUtils.toStrings(params.get(IssueFilterParameters.COMPONENT_ROOTS))))
        .rules(toRules(params.get(IssueFilterParameters.RULES)))
        .actionPlans(RubyUtils.toStrings(params.get(IssueFilterParameters.ACTION_PLANS)))
        .reporters(RubyUtils.toStrings(params.get(IssueFilterParameters.REPORTERS)))
        .assignees(RubyUtils.toStrings(params.get(IssueFilterParameters.ASSIGNEES)))
        .languages(RubyUtils.toStrings(params.get(IssueFilterParameters.LANGUAGES)))
        .assigned(RubyUtils.toBoolean(params.get(IssueFilterParameters.ASSIGNED)))
        .planned(RubyUtils.toBoolean(params.get(IssueFilterParameters.PLANNED)))
        .hideRules(RubyUtils.toBoolean(params.get(IssueFilterParameters.HIDE_RULES)))
        .createdAt(RubyUtils.toDate(params.get(IssueFilterParameters.CREATED_AT)))
        .createdAfter(RubyUtils.toDate(params.get(IssueFilterParameters.CREATED_AFTER)))
        .createdBefore(RubyUtils.toDate(params.get(IssueFilterParameters.CREATED_BEFORE)));
      String sort = (String) params.get(IssueFilterParameters.SORT);
      if (!Strings.isNullOrEmpty(sort)) {
        builder.sort(sort);
        builder.asc(RubyUtils.toBoolean(params.get(IssueFilterParameters.ASC)));
      }
      return builder.build();

    } finally {
      session.close();
    }
  }

  public IssueQuery createFromRequest(Request request) {
    DbSession session = dbClient.openSession(false);
    try {
      IssueQuery.Builder builder = IssueQuery.builder()
        .issueKeys(request.paramAsStrings(IssueFilterParameters.ISSUES))
        .severities(request.paramAsStrings(IssueFilterParameters.SEVERITIES))
        .statuses(request.paramAsStrings(IssueFilterParameters.STATUSES))
        .resolutions(request.paramAsStrings(IssueFilterParameters.RESOLUTIONS))
        .resolved(request.paramAsBoolean(IssueFilterParameters.RESOLVED))
        .components(componentUuids(session, request.paramAsStrings(IssueFilterParameters.COMPONENTS)))
        .componentRoots(componentUuids(session, request.paramAsStrings(IssueFilterParameters.COMPONENT_ROOTS)))
        .rules(stringsToRules(request.paramAsStrings(IssueFilterParameters.RULES)))
        .actionPlans(request.paramAsStrings(IssueFilterParameters.ACTION_PLANS))
        .reporters(request.paramAsStrings(IssueFilterParameters.REPORTERS))
        .assignees(request.paramAsStrings(IssueFilterParameters.ASSIGNEES))
        .languages(request.paramAsStrings(IssueFilterParameters.LANGUAGES))
        .assigned(request.paramAsBoolean(IssueFilterParameters.ASSIGNED))
        .planned(request.paramAsBoolean(IssueFilterParameters.PLANNED))
        .createdAt(request.paramAsDateTime(IssueFilterParameters.CREATED_AT))
        .createdAfter(request.paramAsDateTime(IssueFilterParameters.CREATED_AFTER))
        .createdBefore(request.paramAsDateTime(IssueFilterParameters.CREATED_BEFORE));
      String sort = request.param(SearchRequestHandler.PARAM_SORT);
      if (!Strings.isNullOrEmpty(sort)) {
        builder.sort(sort);
        builder.asc(request.paramAsBoolean(SearchRequestHandler.PARAM_ASCENDING));
      }
      return builder.build();

    } finally {
      session.close();
    }
  }

  private Collection<String> componentUuids(DbSession session, @Nullable Collection<String> componentKeys) {
    Collection<String> componentUuids = newArrayList();
    if (componentKeys != null) {
      for (ComponentDto component : dbClient.componentDao().getByKeys(session, componentKeys)) {
        componentUuids.add(component.uuid());
      }
      // If unknown components are given, if no components are set then all issues will be return,
      // then we add this hack in order to return no issue in this case.
      if (!componentKeys.isEmpty() && componentUuids.isEmpty()) {
        componentUuids.add("<UNKNOWN>");
      }
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
      return newArrayList(Iterables.transform(rules, new Function<String, RuleKey>() {
        @Override
        public RuleKey apply(@Nullable String s) {
          return s != null ? RuleKey.parse(s) : null;
        }
      }));
    }
    return null;
  }
}
