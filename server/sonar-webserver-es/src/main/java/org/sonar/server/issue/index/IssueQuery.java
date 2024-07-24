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

import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.sonar.db.rule.RuleDto;

/**
 * @since 3.6
 */
public class IssueQuery {

  public static final String SORT_BY_CREATION_DATE = "CREATION_DATE";
  public static final String SORT_BY_UPDATE_DATE = "UPDATE_DATE";
  public static final String SORT_BY_CLOSE_DATE = "CLOSE_DATE";
  public static final String SORT_BY_SEVERITY = "SEVERITY";
  public static final String SORT_BY_STATUS = "STATUS";

  /**
   * Sort by project, file path then line id
   */
  public static final String SORT_BY_FILE_LINE = "FILE_LINE";
  /**
   * Sort hotspots by vulnerabilityProbability, sqSecurityCategory, project, file path then line id
   */
  public static final String SORT_HOTSPOTS = "HOTSPOTS";

  public static final Set<String> SORTS = Set.of(SORT_BY_CREATION_DATE, SORT_BY_UPDATE_DATE, SORT_BY_CLOSE_DATE, SORT_BY_SEVERITY,
    SORT_BY_STATUS, SORT_BY_FILE_LINE, SORT_HOTSPOTS);

  private final Collection<String> issueKeys;
  private final Collection<String> severities;
  private final Collection<String> impactSeverities;
  private final Collection<String> impactSoftwareQualities;
  private final Collection<String> statuses;
  private final Collection<String> issueStatuses;
  private final Collection<String> resolutions;
  private final Collection<String> components;
  private final Collection<String> projects;
  private final Collection<String> directories;
  private final Collection<String> files;
  private final Collection<String> views;
  private final Collection<RuleDto> rules;
  private final Collection<String> ruleUuids;
  private final Collection<String> assignees;
  private final Collection<String> authors;
  private final Collection<String> scopes;
  private final Collection<String> languages;
  private final Collection<String> tags;
  private final Collection<String> types;
  private final Collection<String> owaspTop10;
  private final Collection<String> pciDss32;
  private final Collection<String> pciDss40;
  private final Collection<String> owaspAsvs40;
  private final Integer owaspAsvsLevel;
  private final Collection<String> owaspTop10For2021;
  private final Collection<String> stigAsdV5R3;
  private final Collection<String> casa;
  private final Collection<String> sansTop25;
  private final Collection<String> cwe;
  private final Collection<String> sonarsourceSecurity;
  private final Map<String, PeriodStart> createdAfterByProjectUuids;
  private final Boolean onComponentOnly;
  private final Boolean assigned;
  private final Boolean resolved;
  private final Boolean prioritizedRule;
  private final Date createdAt;
  private final PeriodStart createdAfter;
  private final Date createdBefore;
  private final String sort;
  private final Boolean asc;
  private final String facetMode;
  private final String branchUuid;
  private final Boolean mainBranch;
  private final ZoneId timeZone;
  private final Boolean newCodeOnReference;
  private final Collection<String> newCodeOnReferenceByProjectUuids;
  private final Collection<String> codeVariants;
  private final Collection<String> cleanCodeAttributesCategories;

  private IssueQuery(Builder builder) {
    this.issueKeys = nullableDefaultCollection(builder.issueKeys);
    this.severities = defaultCollection(builder.severities);
    this.impactSeverities = defaultCollection(builder.impactSeverities);
    this.impactSoftwareQualities = defaultCollection(builder.impactSoftwareQualities);
    this.statuses = defaultCollection(builder.statuses);
    this.resolutions = defaultCollection(builder.resolutions);
    this.issueStatuses = defaultCollection(builder.issueStatuses);
    this.components = defaultCollection(builder.components);
    this.projects = defaultCollection(builder.projects);
    this.directories = defaultCollection(builder.directories);
    this.files = defaultCollection(builder.files);
    this.views = defaultCollection(builder.views);
    this.rules = defaultCollection(builder.rules);
    this.ruleUuids = defaultCollection(builder.ruleUuids);
    this.assignees = defaultCollection(builder.assigneeUuids);
    this.authors = defaultCollection(builder.authors);
    this.scopes = defaultCollection(builder.scopes);
    this.languages = defaultCollection(builder.languages);
    this.tags = defaultCollection(builder.tags);
    this.types = defaultCollection(builder.types);
    this.pciDss32 = defaultCollection(builder.pciDss32);
    this.pciDss40 = defaultCollection(builder.pciDss40);
    this.owaspAsvs40 = defaultCollection(builder.owaspAsvs40);
    this.owaspAsvsLevel = builder.owaspAsvsLevel;
    this.owaspTop10 = defaultCollection(builder.owaspTop10);
    this.owaspTop10For2021 = defaultCollection(builder.owaspTop10For2021);
    this.stigAsdV5R3 = defaultCollection(builder.stigAsdV5R3);
    this.casa = defaultCollection(builder.casa);
    this.sansTop25 = defaultCollection(builder.sansTop25);
    this.cwe = defaultCollection(builder.cwe);
    this.sonarsourceSecurity = defaultCollection(builder.sonarsourceSecurity);
    this.createdAfterByProjectUuids = defaultMap(builder.createdAfterByProjectUuids);
    this.onComponentOnly = builder.onComponentOnly;
    this.assigned = builder.assigned;
    this.resolved = builder.resolved;
    this.prioritizedRule = builder.prioritizedRule;
    this.createdAt = builder.createdAt;
    this.createdAfter = builder.createdAfter;
    this.createdBefore = builder.createdBefore;
    this.sort = builder.sort;
    this.asc = builder.asc;
    this.facetMode = builder.facetMode;
    this.branchUuid = builder.branchUuid;
    this.mainBranch = builder.mainBranch;
    this.timeZone = builder.timeZone;
    this.newCodeOnReference = builder.newCodeOnReference;
    this.newCodeOnReferenceByProjectUuids = defaultCollection(builder.newCodeOnReferenceByProjectUuids);
    this.codeVariants = defaultCollection(builder.codeVariants);
    this.cleanCodeAttributesCategories = defaultCollection(builder.cleanCodeAttributesCategories);
  }

  public Collection<String> issueKeys() {
    return issueKeys;
  }

  public Collection<String> severities() {
    return severities;
  }

  public Collection<String> impactSeverities() {
    return impactSeverities;
  }

  public Collection<String> impactSoftwareQualities() {
    return impactSoftwareQualities;
  }

  public Collection<String> statuses() {
    return statuses;
  }

  public Collection<String> issueStatuses() {
    return issueStatuses;
  }

  public Collection<String> resolutions() {
    return resolutions;
  }

  public Collection<String> componentUuids() {
    return components;
  }

  public Collection<String> projectUuids() {
    return projects;
  }

  public Collection<String> directories() {
    return directories;
  }

  public Collection<String> files() {
    return files;
  }

  /**
   * Restrict issues belonging to projects that were analyzed under a view.
   * The view UUIDs should be portfolios, sub portfolios or application branches.
   */
  public Collection<String> viewUuids() {
    return views;
  }

  public Collection<RuleDto> rules() {
    return rules;
  }

  public Collection<String> ruleUuids() {
    return ruleUuids;
  }

  public Collection<String> assignees() {
    return assignees;
  }

  public Collection<String> authors() {
    return authors;
  }

  public Collection<String> scopes() {
    return scopes;
  }

  public Collection<String> languages() {
    return languages;
  }

  public Collection<String> tags() {
    return tags;
  }

  public Collection<String> types() {
    return types;
  }

  public Collection<String> pciDss32() {
    return pciDss32;
  }

  public Collection<String> pciDss40() {
    return pciDss40;
  }

  public Collection<String> owaspAsvs40() {
    return owaspAsvs40;
  }

  public Optional<Integer> getOwaspAsvsLevel() {
    return Optional.ofNullable(owaspAsvsLevel);
  }

  public Collection<String> owaspTop10() {
    return owaspTop10;
  }

  public Collection<String> owaspTop10For2021() {
    return owaspTop10For2021;
  }

  public Collection<String> stigAsdV5R3() {
    return stigAsdV5R3;
  }

  public Collection<String> casa() {
    return casa;
  }

  public Collection<String> sansTop25() {
    return sansTop25;
  }

  public Collection<String> cwe() {
    return cwe;
  }

  public Collection<String> sonarsourceSecurity() {
    return sonarsourceSecurity;
  }

  public Map<String, PeriodStart> createdAfterByProjectUuids() {
    return createdAfterByProjectUuids;
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
  public Boolean resolved() {
    return resolved;
  }

  @CheckForNull
  public Boolean prioritizedRule() {
    return prioritizedRule;
  }

  @CheckForNull
  public PeriodStart createdAfter() {
    return createdAfter;
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
  public String branchUuid() {
    return branchUuid;
  }

  public Boolean isMainBranch() {
    return mainBranch;
  }

  public String facetMode() {
    return facetMode;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }

  public static Builder builder() {
    return new Builder();
  }

  @CheckForNull
  public ZoneId timeZone() {
    return timeZone;
  }

  @CheckForNull
  public Boolean newCodeOnReference() {
    return newCodeOnReference;
  }

  public Collection<String> newCodeOnReferenceByProjectUuids() {
    return newCodeOnReferenceByProjectUuids;
  }

  public Collection<String> codeVariants() {
    return codeVariants;
  }

  public Collection<String> cleanCodeAttributesCategories() {
    return cleanCodeAttributesCategories;
  }

  public static class Builder {
    private Collection<String> issueKeys;
    private Collection<String> severities;
    private Collection<String> impactSeverities;
    private Collection<String> impactSoftwareQualities;
    private Collection<String> statuses;
    private Collection<String> resolutions;
    private Collection<String> issueStatuses;
    private Collection<String> components;
    private Collection<String> projects;
    private Collection<String> directories;
    private Collection<String> files;
    private Collection<String> views;
    private Collection<RuleDto> rules;
    private Collection<String> ruleUuids;
    private Collection<String> assigneeUuids;
    private Collection<String> authors;
    private Collection<String> scopes;
    private Collection<String> languages;
    private Collection<String> tags;
    private Collection<String> types;
    private Collection<String> pciDss32;
    private Collection<String> pciDss40;
    private Collection<String> owaspAsvs40;
    private Integer owaspAsvsLevel;
    private Collection<String> owaspTop10;
    private Collection<String> owaspTop10For2021;
    private Collection<String> stigAsdV5R3;
    private Collection<String> casa;
    private Collection<String> sansTop25;
    private Collection<String> cwe;
    private Collection<String> sonarsourceSecurity;
    private Map<String, PeriodStart> createdAfterByProjectUuids;
    private Boolean onComponentOnly = false;
    private Boolean assigned = null;
    private Boolean resolved = null;
    private Boolean prioritizedRule = null;
    private Date createdAt;
    private PeriodStart createdAfter;
    private Date createdBefore;
    private String sort;
    private Boolean asc = false;
    private String facetMode;
    private String branchUuid;
    private Boolean mainBranch = true;
    private ZoneId timeZone;
    private Boolean newCodeOnReference = null;
    private Collection<String> newCodeOnReferenceByProjectUuids;
    private Collection<String> codeVariants;
    private Collection<String> cleanCodeAttributesCategories;

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

    public Builder issueStatuses(@Nullable Collection<String> l) {
      this.issueStatuses = l;
      return this;
    }

    public Builder componentUuids(@Nullable Collection<String> l) {
      this.components = l;
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

    public Builder impactSeverities(@Nullable Collection<String> l) {
      this.impactSeverities = l;
      return this;
    }

    public Builder impactSoftwareQualities(@Nullable Collection<String> l) {
      this.impactSoftwareQualities = l;
      return this;
    }

    public Builder files(@Nullable Collection<String> l) {
      this.files = l;
      return this;
    }

    /**
     * Restrict issues belonging to projects that were analyzed under a view.
     * The view UUIDs should be portfolios, sub portfolios or application branches.
     */
    public Builder viewUuids(@Nullable Collection<String> l) {
      this.views = l;
      return this;
    }

    public Builder rules(@Nullable Collection<RuleDto> rules) {
      this.rules = rules;
      return this;
    }

    public Builder ruleUuids(@Nullable Collection<String> ruleUuids) {
      this.ruleUuids = ruleUuids;
      return this;
    }

    public Builder assigneeUuids(@Nullable Collection<String> l) {
      this.assigneeUuids = l;
      return this;
    }

    public Builder authors(@Nullable Collection<String> l) {
      this.authors = l;
      return this;
    }

    public Builder scopes(@Nullable Collection<String> s) {
      this.scopes = s;
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

    public Builder types(@Nullable Collection<String> t) {
      this.types = t;
      return this;
    }

    public Builder pciDss32(@Nullable Collection<String> o) {
      this.pciDss32 = o;
      return this;
    }

    public Builder pciDss40(@Nullable Collection<String> o) {
      this.pciDss40 = o;
      return this;
    }

    public Builder owaspAsvs40(@Nullable Collection<String> o) {
      this.owaspAsvs40 = o;
      return this;
    }

    public Builder owaspAsvsLevel(@Nullable Integer level) {
      this.owaspAsvsLevel = level;
      return this;
    }

    public Builder owaspTop10(@Nullable Collection<String> o) {
      this.owaspTop10 = o;
      return this;
    }

    public Builder owaspTop10For2021(@Nullable Collection<String> o) {
      this.owaspTop10For2021 = o;
      return this;
    }

    public Builder stigAsdR5V3(@Nullable Collection<String> o) {
      this.stigAsdV5R3 = o;
      return this;
    }

    public Builder casa(@Nullable Collection<String> o) {
      this.casa = o;
      return this;
    }

    public Builder sansTop25(@Nullable Collection<String> s) {
      this.sansTop25 = s;
      return this;
    }

    public Builder cwe(@Nullable Collection<String> cwe) {
      this.cwe = cwe;
      return this;
    }

    public Builder sonarsourceSecurity(@Nullable Collection<String> sonarsourceSecurity) {
      this.sonarsourceSecurity = sonarsourceSecurity;
      return this;
    }

    public Builder createdAfterByProjectUuids(@Nullable Map<String, PeriodStart> createdAfterByProjectUuids) {
      this.createdAfterByProjectUuids = createdAfterByProjectUuids;
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
     * If true, it will return all resolved issues
     * If false, it will return all none resolved issues
     */
    public Builder resolved(@Nullable Boolean resolved) {
      this.resolved = resolved;
      return this;
    }

    public Builder prioritizedRule(@Nullable Boolean prioritizedRule) {
      this.prioritizedRule = prioritizedRule;
      return this;
    }


    public Builder createdAt(@Nullable Date d) {
      this.createdAt = d == null ? null : new Date(d.getTime());
      return this;
    }

    public Builder createdAfter(@Nullable Date d) {
      this.createdAfter(d, true);
      return this;
    }

    public Builder createdAfter(@Nullable Date d, boolean inclusive) {
      this.createdAfter = d == null ? null : new PeriodStart(new Date(d.getTime()), inclusive);
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
      return new IssueQuery(this);
    }

    public Builder facetMode(String facetMode) {
      this.facetMode = facetMode;
      return this;
    }

    public Builder branchUuid(@Nullable String s) {
      this.branchUuid = s;
      return this;
    }

    public Builder mainBranch(@Nullable Boolean mainBranch) {
      this.mainBranch = mainBranch;
      return this;
    }

    public Builder timeZone(ZoneId timeZone) {
      this.timeZone = timeZone;
      return this;
    }

    public Builder newCodeOnReference(@Nullable Boolean newCodeOnReference) {
      this.newCodeOnReference = newCodeOnReference;
      return this;
    }

    public Builder newCodeOnReferenceByProjectUuids(@Nullable Collection<String> newCodeOnReferenceByProjectUuids) {
      this.newCodeOnReferenceByProjectUuids = newCodeOnReferenceByProjectUuids;
      return this;
    }

    public Builder codeVariants(@Nullable Collection<String> codeVariants) {
      this.codeVariants = codeVariants;
      return this;
    }

    public Builder cleanCodeAttributesCategories(@Nullable Collection<String> cleanCodeAttributesCategories) {
      this.cleanCodeAttributesCategories = cleanCodeAttributesCategories;
      return this;
    }
  }

  private static <T> Collection<T> defaultCollection(@Nullable Collection<T> c) {
    return c == null ? Collections.emptyList() : Collections.unmodifiableCollection(c);
  }

  private static <T> Collection<T> nullableDefaultCollection(@Nullable Collection<T> c) {
    return c == null ? null : Collections.unmodifiableCollection(c);
  }

  private static <K, V> Map<K, V> defaultMap(@Nullable Map<K, V> map) {
    return map == null ? Collections.emptyMap() : Collections.unmodifiableMap(map);
  }

  public static class PeriodStart {
    private final Date date;
    private final boolean inclusive;

    public PeriodStart(Date date, boolean inclusive) {
      this.date = date;
      this.inclusive = inclusive;

    }

    public Date date() {
      return date;
    }

    public boolean inclusive() {
      return inclusive;
    }

  }

}
