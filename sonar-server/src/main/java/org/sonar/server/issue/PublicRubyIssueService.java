/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.IssueQueryResult;
import org.sonar.api.issue.RubyIssueService;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.web.UserRole;
import org.sonar.server.util.RubyUtils;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Facade of issue components for JRuby on Rails webapp
 *
 * @since 3.6
 */
public class PublicRubyIssueService implements RubyIssueService {

  private final IssueFinder finder;

  public PublicRubyIssueService(IssueFinder f) {
    this.finder = f;
  }

  /**
   * Requires the role {@link org.sonar.api.web.UserRole#USER}
   */
  @Override
  public IssueQueryResult find(String issueKey) {
    return finder.find(
      IssueQuery.builder()
        .issueKeys(Arrays.asList(issueKey))
        .requiredRole(UserRole.USER)
        .build());
  }

  /**
   * Requires the role {@link org.sonar.api.web.UserRole#USER}
   */
  @Override
  public IssueQueryResult find(Map<String, Object> params) {
    return finder.find(toQuery(params));
  }

  static IssueQuery toQuery(Map<String, Object> props) {
    IssueQuery.Builder builder = IssueQuery.builder()
      .requiredRole(UserRole.USER)
      .issueKeys(RubyUtils.toStrings(props.get(IssueFilterParameters.ISSUES)))
      .severities(RubyUtils.toStrings(props.get(IssueFilterParameters.SEVERITIES)))
      .statuses(RubyUtils.toStrings(props.get(IssueFilterParameters.STATUSES)))
      .resolutions(RubyUtils.toStrings(props.get(IssueFilterParameters.RESOLUTIONS)))
      .resolved(RubyUtils.toBoolean(props.get(IssueFilterParameters.RESOLVED)))
      .components(RubyUtils.toStrings(props.get(IssueFilterParameters.COMPONENTS)))
      .componentRoots(RubyUtils.toStrings(props.get(IssueFilterParameters.COMPONENT_ROOTS)))
      .rules(toRules(props.get(IssueFilterParameters.RULES)))
      .actionPlans(RubyUtils.toStrings(props.get(IssueFilterParameters.ACTION_PLANS)))
      .reporters(RubyUtils.toStrings(props.get(IssueFilterParameters.REPORTERS)))
      .assignees(RubyUtils.toStrings(props.get(IssueFilterParameters.ASSIGNEES)))
      .assigned(RubyUtils.toBoolean(props.get(IssueFilterParameters.ASSIGNED)))
      .planned(RubyUtils.toBoolean(props.get(IssueFilterParameters.PLANNED)))
      .createdAfter(RubyUtils.toDate(props.get(IssueFilterParameters.CREATED_AFTER)))
      .createdBefore(RubyUtils.toDate(props.get(IssueFilterParameters.CREATED_BEFORE)))
      .pageSize(RubyUtils.toInteger(props.get(IssueFilterParameters.PAGE_SIZE)))
      .pageIndex(RubyUtils.toInteger(props.get(IssueFilterParameters.PAGE_INDEX)));
    String sort = (String) props.get(IssueFilterParameters.SORT);
    if (!Strings.isNullOrEmpty(sort)) {
      builder.sort(sort);
      builder.asc(RubyUtils.toBoolean(props.get(IssueFilterParameters.ASC)));
    }
    return builder.build();
  }

  @SuppressWarnings("unchecked")
  static Collection<RuleKey> toRules(Object o) {
    Collection<RuleKey> result = null;
    if (o != null) {
      if (o instanceof List) {
        // assume that it contains only strings
        result = stringsToRules((List<String>) o);
      } else if (o instanceof String) {
        result = stringsToRules(Lists.newArrayList(Splitter.on(',').omitEmptyStrings().split((String) o)));
      }
    }
    return result;
  }

  private static Collection<RuleKey> stringsToRules(Collection<String> o) {
    return Collections2.transform(o, new Function<String, RuleKey>() {
      @Override
      public RuleKey apply(@Nullable String s) {
        return s != null ? RuleKey.parse(s) : null;
      }
    });
  }


  public void start() {
    // used to force pico to instantiate the singleton at startup
  }
}
