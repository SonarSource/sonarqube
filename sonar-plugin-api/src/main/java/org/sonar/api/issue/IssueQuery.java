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
package org.sonar.api.issue;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.web.UserRole;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Date;

/**
 * @since 3.6
 */
public class IssueQuery {

  public static final int DEFAULT_PAGE_INDEX = 1;
  public static final int DEFAULT_PAGE_SIZE = 100;
  public static final int MAX_RESULTS = 5000;
  public static final int MAX_PAGE_SIZE = 500;
  public static final int MAX_ISSUE_KEYS = 500;

  public static enum Sort {
    CREATION_DATE, UPDATE_DATE, CLOSE_DATE, ASSIGNEE, SEVERITY, STATUS
  }

  private final Collection<String> issueKeys;
  private final Collection<String> severities;
  private final Collection<String> statuses;
  private final Collection<String> resolutions;
  private final Collection<String> components;
  private final Collection<String> componentRoots;
  private final Collection<RuleKey> rules;
  private final Collection<String> actionPlans;
  private final Collection<String> reporters;
  private final Collection<String> assignees;
  private final Boolean assigned;
  private final Boolean planned;
  private final Boolean resolved;
  private final Date createdAfter;
  private final Date createdBefore;
  private final Sort sort;
  private final Boolean asc;
  private final String requiredRole;

  // max results per page
  private final int pageSize;

  // index of selected page. Start with 1.
  private final int pageIndex;

  private IssueQuery(Builder builder) {
    this.issueKeys = builder.issueKeys;
    this.severities = builder.severities;
    this.statuses = builder.statuses;
    this.resolutions = builder.resolutions;
    this.components = builder.components;
    this.componentRoots = builder.componentRoots;
    this.rules = builder.rules;
    this.actionPlans = builder.actionPlans;
    this.reporters = builder.reporters;
    this.assignees = builder.assignees;
    this.assigned = builder.assigned;
    this.planned = builder.planned;
    this.resolved = builder.resolved;
    this.createdAfter = builder.createdAfter;
    this.createdBefore = builder.createdBefore;
    this.sort = builder.sort;
    this.asc = builder.asc;
    this.pageSize = builder.pageSize;
    this.pageIndex = builder.pageIndex;
    this.requiredRole = builder.requiredRole;
  }

  public Collection<String> issueKeys() {
    return issueKeys;
  }

  public Collection<String> severities() {
    return severities;
  }

  public Collection<String> statuses() {
    return statuses;
  }

  public Collection<String> resolutions() {
    return resolutions;
  }

  public Collection<String> components() {
    return components;
  }

  public Collection<String> componentRoots() {
    return componentRoots;
  }

  public Collection<RuleKey> rules() {
    return rules;
  }

  public Collection<String> actionPlans() {
    return actionPlans;
  }

  public Collection<String> reporters() {
    return reporters;
  }

  public Collection<String> assignees() {
    return assignees;
  }

  @CheckForNull
  public Boolean assigned() {
    return assigned;
  }

  @CheckForNull
  public Boolean planned() {
    return planned;
  }

  @CheckForNull
  public Boolean resolved() {
    return resolved;
  }

  @CheckForNull
  public Date createdAfter() {
    return createdAfter;
  }

  @CheckForNull
  public Date createdBefore() {
    return createdBefore;
  }

  @CheckForNull
  public Sort sort() {
    return sort;
  }

  @CheckForNull
  public Boolean asc() {
    return asc;
  }

  public int pageSize() {
    return pageSize;
  }

  public int pageIndex() {
    return pageIndex;
  }

  public int maxResults() {
    return MAX_RESULTS;
  }

  public String requiredRole() {
    return requiredRole;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Collection<String> issueKeys;
    private Collection<String> severities;
    private Collection<String> statuses;
    private Collection<String> resolutions;
    private Collection<String> components;
    private Collection<String> componentRoots;
    private Collection<RuleKey> rules;
    private Collection<String> actionPlans;
    private Collection<String> reporters;
    private Collection<String> assignees;
    private Boolean assigned = null;
    private Boolean planned = null;
    private Boolean resolved = null;
    private Date createdAfter;
    private Date createdBefore;
    private Sort sort;
    private Boolean asc = false;
    private Integer pageSize;
    private Integer pageIndex;
    private String requiredRole = UserRole.CODEVIEWER;

    private Builder() {
    }

    public Builder issueKeys(Collection<String> l) {
      this.issueKeys = l;
      return this;
    }

    public Builder severities(Collection<String> l) {
      this.severities = l;
      return this;
    }

    public Builder statuses(Collection<String> l) {
      this.statuses = l;
      return this;
    }

    public Builder resolutions(Collection<String> l) {
      this.resolutions = l;
      return this;
    }

    public Builder components(Collection<String> l) {
      this.components = l;
      return this;
    }

    public Builder componentRoots(Collection<String> l) {
      this.componentRoots = l;
      return this;
    }

    public Builder rules(Collection<RuleKey> rules) {
      this.rules = rules;
      return this;
    }

    public Builder actionPlans(Collection<String> l) {
      this.actionPlans = l;
      return this;
    }

    public Builder reporters(Collection<String> l) {
      this.reporters = l;
      return this;
    }

    public Builder assignees(Collection<String> l) {
      this.assignees = l;
      return this;
    }

    /**
     * If true, it will return all issues assigned to someone
     * If false, it will return all issues not assigned to someone
     */
    public Builder assigned(@Nullable Boolean b) {
      this.assigned = b;
      return this;
    }

    /**
     * If true, it will return all issues linked to an action plan
     * If false, it will return all issues not linked to an action plan
     */
    public Builder planned(Boolean planned) {
      this.planned = planned;
      return this;
    }

    /**
     * If true, it will return all resolved issues
     * If false, it will return all none resolved issues
     */
    public Builder resolved(@Nullable Boolean resolved) {
      this.resolved = resolved;
      return this;
    }

    public Builder createdAfter(@Nullable Date d) {
      this.createdAfter = d;
      return this;
    }

    public Builder createdBefore(@Nullable Date d) {
      this.createdBefore = d;
      return this;
    }

    public Builder sort(@Nullable Sort sort) {
      this.sort = sort;
      return this;
    }

    public Builder asc(@Nullable Boolean asc) {
      this.asc = asc;
      return this;
    }

    public Builder pageSize(@Nullable Integer i) {
      this.pageSize = i;
      return this;
    }

    public Builder pageIndex(@Nullable Integer i) {
      this.pageIndex = i;
      return this;
    }

    public Builder requiredRole(@Nullable String s) {
      this.requiredRole = s;
      return this;
    }

    public IssueQuery build() {
      initPageIndex();
      initPageSize();
      if (issueKeys != null) {
        Preconditions.checkArgument(issueKeys.size() < MAX_ISSUE_KEYS, "Number of issue keys must be less than " + MAX_ISSUE_KEYS + " (got " + issueKeys.size() + ")");
      }
      return new IssueQuery(this);
    }

    private void initPageSize() {
      if (components != null && components.size() == 1 && pageSize == null) {
        pageSize = 999999;
      } else {
        if (pageSize == null) {
          pageSize = DEFAULT_PAGE_SIZE;
        } else if (pageSize<=0 || pageSize > MAX_PAGE_SIZE) {
          pageSize = MAX_PAGE_SIZE;
        }
      }
    }

    private void initPageIndex() {
      if (pageIndex == null) {
        pageIndex = DEFAULT_PAGE_INDEX;
      }
      Preconditions.checkArgument(pageIndex > 0, "Page index must be greater than 0 (got " + pageIndex + ")");
    }
  }
}
