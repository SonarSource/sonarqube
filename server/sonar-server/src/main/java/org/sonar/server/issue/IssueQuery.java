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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.sonar.api.rule.RuleKey;
import org.sonar.server.search.QueryContext;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

/**
 * @since 3.6
 */
public class IssueQuery {

  public static final String SORT_BY_CREATION_DATE = "CREATION_DATE";
  public static final String SORT_BY_UPDATE_DATE = "UPDATE_DATE";
  public static final String SORT_BY_CLOSE_DATE = "CLOSE_DATE";
  public static final String SORT_BY_ASSIGNEE = "ASSIGNEE";
  public static final String SORT_BY_SEVERITY = "SEVERITY";
  public static final String SORT_BY_STATUS = "STATUS";

  /**
   * Sort by project, file path then line id
   */
  public static final String SORT_BY_FILE_LINE = "FILE_LINE";

  public static final Set<String> SORTS = ImmutableSet.of(SORT_BY_CREATION_DATE, SORT_BY_UPDATE_DATE, SORT_BY_CLOSE_DATE, SORT_BY_ASSIGNEE, SORT_BY_SEVERITY,
    SORT_BY_STATUS, SORT_BY_FILE_LINE);

  private final Collection<String> issueKeys;
  private final Collection<String> severities;
  private final Collection<String> statuses;
  private final Collection<String> resolutions;
  private final Collection<String> components;
  private final Collection<String> modules;
  private final Collection<String> moduleRoots;
  private final Collection<String> projects;
  private final Collection<String> directories;
  private final Collection<String> files;
  private final Collection<String> views;
  private final Collection<RuleKey> rules;
  private final Collection<String> actionPlans;
  private final Collection<String> reporters;
  private final Collection<String> assignees;
  private final Collection<String> authors;
  private final Collection<String> languages;
  private final Collection<String> tags;
  private final Boolean onComponentOnly;
  private final Boolean assigned;
  private final Boolean planned;
  private final Boolean resolved;
  private final Boolean hideRules;
  private final Date createdAt;
  private final Date createdAfter;
  private final Date createdBefore;
  private final String sort;
  private final Boolean asc;
  private final Boolean ignorePaging;

  private final String userLogin;
  private final Set<String> userGroups;
  private final boolean checkAuthorization;

  private IssueQuery(Builder builder) {
    this.issueKeys = defaultCollection(builder.issueKeys);
    this.severities = defaultCollection(builder.severities);
    this.statuses = defaultCollection(builder.statuses);
    this.resolutions = defaultCollection(builder.resolutions);
    this.components = defaultCollection(builder.components);
    this.modules = defaultCollection(builder.modules);
    this.moduleRoots = defaultCollection(builder.moduleRoots);
    this.projects = defaultCollection(builder.projects);
    this.directories = defaultCollection(builder.directories);
    this.files = defaultCollection(builder.files);
    this.views = defaultCollection(builder.views);
    this.rules = defaultCollection(builder.rules);
    this.actionPlans = defaultCollection(builder.actionPlans);
    this.reporters = defaultCollection(builder.reporters);
    this.assignees = defaultCollection(builder.assignees);
    this.authors = defaultCollection(builder.authors);
    this.languages = defaultCollection(builder.languages);
    this.tags = defaultCollection(builder.tags);
    this.onComponentOnly = builder.onComponentOnly;
    this.assigned = builder.assigned;
    this.planned = builder.planned;
    this.resolved = builder.resolved;
    this.hideRules = builder.hideRules;
    this.createdAt = builder.createdAt;
    this.createdAfter = builder.createdAfter;
    this.createdBefore = builder.createdBefore;
    this.sort = builder.sort;
    this.asc = builder.asc;
    this.ignorePaging = builder.ignorePaging;
    this.userLogin = builder.userLogin;
    this.userGroups = builder.userGroups;
    this.checkAuthorization = builder.checkAuthorization;
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

  public Collection<String> componentUuids() {
    return components;
  }

  public Collection<String> moduleUuids() {
    return modules;
  }

  public Collection<String> moduleRootUuids() {
    return moduleRoots;
  }

  public Collection<String> projectUuids() {
    return projects;
  }

  public Collection<String> directories() {
    return directories;
  }

  public Collection<String> fileUuids() {
    return files;
  }

  public Collection<String> viewUuids() {
    return views;
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

  public Collection<String> authors() {
    return authors;
  }

  public Collection<String> languages() {
    return languages;
  }

  public Collection<String> tags() {
    return tags;
  }

  @CheckForNull
  public Boolean onComponentOnly() {
    return onComponentOnly;
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

  /**
   * @since 4.2
   */
  @CheckForNull
  public Boolean hideRules() {
    return hideRules;
  }

  @CheckForNull
  public Date createdAfter() {
    return createdAfter == null ? null : new Date(createdAfter.getTime());
  }

  @CheckForNull
  public Date createdAt() {
    return createdAt == null ? null : new Date(createdAt.getTime());
  }

  @CheckForNull
  public Date createdBefore() {
    return createdBefore == null ? null : new Date(createdBefore.getTime());
  }

  @CheckForNull
  public String sort() {
    return sort;
  }

  @CheckForNull
  public Boolean asc() {
    return asc;
  }

  @CheckForNull
  public Boolean ignorePaging() {
    return ignorePaging;
  }

  @CheckForNull
  public String userLogin() {
    return userLogin;
  }

  public Set<String> userGroups() {
    return newHashSet(defaultCollection(userGroups));
  }

  public boolean checkAuthorization() {
    return checkAuthorization;
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
    private Collection<String> modules;
    private Collection<String> moduleRoots;
    private Collection<String> projects;
    private Collection<String> directories;
    private Collection<String> files;
    private Collection<String> views;
    private Collection<RuleKey> rules;
    private Collection<String> actionPlans;
    private Collection<String> reporters;
    private Collection<String> assignees;
    private Collection<String> authors;
    private Collection<String> languages;
    private Collection<String> tags;
    private Boolean onComponentOnly = false;
    private Boolean assigned = null;
    private Boolean planned = null;
    private Boolean resolved = null;
    private Boolean hideRules = false;
    private Date createdAt;
    private Date createdAfter;
    private Date createdBefore;
    private String sort;
    private Boolean asc = false;
    private Boolean ignorePaging = false;
    private String userLogin = UserSession.get().login();
    private Set<String> userGroups = UserSession.get().userGroups();
    private boolean checkAuthorization = true;

    private Builder() {
    }

    public Builder issueKeys(@Nullable Collection<String> l) {
      this.issueKeys = l;
      return this;
    }

    public Builder severities(@Nullable Collection<String> l) {
      this.severities = l;
      return this;
    }

    public Builder statuses(@Nullable Collection<String> l) {
      this.statuses = l;
      return this;
    }

    public Builder resolutions(@Nullable Collection<String> l) {
      this.resolutions = l;
      return this;
    }

    public Builder componentUuids(@Nullable Collection<String> l) {
      this.components = l;
      return this;
    }

    public Builder moduleUuids(@Nullable Collection<String> l) {
      this.modules = l;
      return this;
    }

    public Builder moduleRootUuids(@Nullable Collection<String> l) {
      this.moduleRoots = l;
      return this;
    }

    public Builder projectUuids(@Nullable Collection<String> l) {
      this.projects = l;
      return this;
    }

    public Builder directories(@Nullable Collection<String> l) {
      this.directories = l;
      return this;
    }

    public Builder fileUuids(@Nullable Collection<String> l) {
      this.files = l;
      return this;
    }

    public Builder viewUuids(@Nullable Collection<String> l) {
      this.views = l;
      return this;
    }

    public Builder rules(@Nullable Collection<RuleKey> rules) {
      this.rules = rules;
      return this;
    }

    public Builder actionPlans(@Nullable Collection<String> l) {
      this.actionPlans = l;
      return this;
    }

    public Builder reporters(@Nullable Collection<String> l) {
      this.reporters = l;
      return this;
    }

    public Builder assignees(@Nullable Collection<String> l) {
      this.assignees = l;
      return this;
    }

    public Builder authors(@Nullable Collection<String> l) {
      this.authors = l;
      return this;
    }

    public Builder languages(@Nullable Collection<String> l) {
      this.languages = l;
      return this;
    }

    public Builder tags(@Nullable Collection<String> t) {
      this.tags = t;
      return this;
    }

    /**
     * If true, it will return only issues on the passed component(s)
     * If false, it will return all issues on the passed component(s) and their descendants
     */
    public Builder onComponentOnly(@Nullable Boolean b) {
      this.onComponentOnly = b;
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
    public Builder planned(@Nullable Boolean planned) {
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

    /**
     * If true, rules will not be loaded
     * If false, rules will be loaded
     *
     * @since 4.2
     *
     */
    public Builder hideRules(@Nullable Boolean b) {
      this.hideRules = b;
      return this;
    }

    public Builder createdAt(@Nullable Date d) {
      this.createdAt = d == null ? null : new Date(d.getTime());
      return this;
    }

    public Builder createdAfter(@Nullable Date d) {
      this.createdAfter = d == null ? null : new Date(d.getTime());
      return this;
    }

    public Builder createdBefore(@Nullable Date d) {
      this.createdBefore = d == null ? null : new Date(d.getTime());
      return this;
    }

    public Builder sort(@Nullable String s) {
      if (s != null && !SORTS.contains(s)) {
        throw new IllegalArgumentException("Bad sort field: " + s);
      }
      this.sort = s;
      return this;
    }

    public Builder asc(@Nullable Boolean asc) {
      this.asc = asc;
      return this;
    }

    public IssueQuery build() {
      if (issueKeys != null) {
        Preconditions.checkArgument(issueKeys.size() <= QueryContext.MAX_LIMIT, "Number of issue keys must be less than " + QueryContext.MAX_LIMIT + " (got " + issueKeys.size()
          + ")");
      }
      return new IssueQuery(this);
    }

    public Builder ignorePaging(@Nullable Boolean ignorePaging) {
      this.ignorePaging = ignorePaging;
      return this;
    }

    public Builder userLogin(@Nullable String userLogin) {
      this.userLogin = userLogin;
      return this;
    }

    public Builder userGroups(@Nullable Set<String> userGroups) {
      this.userGroups = userGroups;
      return this;
    }

    public Builder checkAuthorization(boolean checkAuthorization) {
      this.checkAuthorization = checkAuthorization;
      return this;
    }

  }

  private static <T> Collection<T> defaultCollection(@Nullable Collection<T> c) {
    return c == null ? Collections.<T>emptyList() : Collections.unmodifiableCollection(c);
  }
}
