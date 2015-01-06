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
package org.sonar.wsclient.issue;

import org.sonar.wsclient.internal.EncodingUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 3.6
 */
public class IssueQuery {

  private final Map<String, Object> params = new HashMap<String, Object>();

  private IssueQuery() {
  }

  public static IssueQuery create() {
    return new IssueQuery();
  }

  /**
   * URL query string, for internal use
   */
  public Map<String, Object> urlParams() {
    return params;
  }

  public IssueQuery issues(String... keys) {
    return addParam("issues", keys);
  }

  public IssueQuery severities(String... severities) {
    return addParam("severities", severities);
  }

  public IssueQuery statuses(String... statuses) {
    return addParam("statuses", statuses);
  }

  public IssueQuery resolutions(String... resolutions) {
    return addParam("resolutions", resolutions);
  }

  public IssueQuery components(String... components) {
    return addParam("components", components);
  }

  public IssueQuery onComponentOnly(boolean onComponentOnly) {
    params.put("onComponentOnly", onComponentOnly);
    return this;
  }

  public IssueQuery rules(String... s) {
    return addParam("rules", s);
  }

  public IssueQuery actionPlans(String... s) {
    return addParam("actionPlans", s);
  }

  public IssueQuery reporters(String... s) {
    return addParam("reporters", s);
  }

  public IssueQuery assignees(String... s) {
    return addParam("assignees", s);
  }
  public IssueQuery languages(String... s) {
    return addParam("languages", s);
  }

  public IssueQuery assigned(Boolean assigned) {
    params.put("assigned", assigned);
    return this;
  }

  public IssueQuery planned(Boolean planned) {
    params.put("planned", planned);
    return this;
  }

  public IssueQuery resolved(Boolean resolved) {
    params.put("resolved", resolved);
    return this;
  }

  /**
   * @since 4.2
   */
  public IssueQuery hideRules(Boolean hideRules) {
    params.put("hideRules", hideRules);
    return this;
  }

  /**
   * Require second precision.
   * @since 3.7
   */
  public IssueQuery createdAt(Date d) {
    params.put("createdAt", EncodingUtils.toQueryParam(d, true));
    return this;
  }

  public IssueQuery createdAfter(Date d) {
    params.put("createdAfter", EncodingUtils.toQueryParam(d, true));
    return this;
  }

  public IssueQuery createdBefore(Date d) {
    params.put("createdBefore", EncodingUtils.toQueryParam(d, true));
    return this;
  }

  public IssueQuery sort(String sort) {
    params.put("sort", sort);
    return this;
  }

  public IssueQuery asc(boolean asc) {
    params.put("asc", asc);
    return this;
  }

  public IssueQuery pageSize(int pageSize) {
    params.put("pageSize", pageSize);
    return this;
  }

  public IssueQuery pageIndex(int pageIndex) {
    params.put("pageIndex", pageIndex);
    return this;
  }

  private IssueQuery addParam(String key, String[] values) {
    if (values != null) {
      params.put(key, EncodingUtils.toQueryParam(values));
    }
    return this;
  }

}
