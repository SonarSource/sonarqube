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

package org.sonar.server.rule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.util.RubyUtils;

import javax.annotation.CheckForNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @since 4.2
 */
public class ProfileRuleQuery {

  private int profileId;
  private String nameOrKey;
  private List<String> repositoryKeys;
  private List<String> severities;
  private List<String> statuses;

  private ProfileRuleQuery() {
    repositoryKeys = Lists.newArrayList();
    severities = Lists.newArrayList();
    statuses = Lists.newArrayList();
  }

  public static ProfileRuleQuery parse(Map<String, Object> params) {
    BadRequestException validationException = BadRequestException.of("Incorrect rule search parameters");

    validatePresenceOf(params, validationException, "profileId");

    ProfileRuleQuery result = new ProfileRuleQuery();

    try {
      result.profileId = RubyUtils.toInteger(params.get("profileId"));
    } catch (Exception badProfileId) {
      validationException.addError("profileId could not be parsed");
    }


    if (params.containsKey("nameOrKey")) {
      result.setNameOrKey((String) params.get("nameOrKey"));
    }
    if (params.get("repositoryKeys") != null) {
      for (Object param: (Object[]) params.get("repositoryKeys")) {
        if (! "".equals(param)) {
          result.addRepositoryKeys((String) param);
        }
      }
    }
    if (params.get("severities") != null) {
      for (Object param: (Object[]) params.get("severities")) {
        if (! "".equals(param)) {
          result.addSeverities((String) param);
        }
      }
    }
    if (params.get("statuses") != null) {
      for (Object param: (Object[]) params.get("statuses")) {
        if (! "".equals(param)) {
          result.addStatuses((String) param);
        }
      }
    }

    validationException.checkMessages();

    return result;
  }

  private static void validatePresenceOf(Map<String, Object> params, BadRequestException validationException, String... paramNames) {
    for (String param: paramNames) {
      if (params.get(param) == null) {
        validationException.addError("Missing parameter "+param);
      }
    }
  }

  public static ProfileRuleQuery create(int profileId) {
    ProfileRuleQuery newQuery = new ProfileRuleQuery();
    newQuery.profileId = profileId;
    return newQuery;
  }

  public ProfileRuleQuery setNameOrKey(String nameOrKey) {
    this.nameOrKey = nameOrKey;
    return this;
  }

  public ProfileRuleQuery addRepositoryKeys(String... repositoryKeys) {
    this.repositoryKeys.addAll(Arrays.asList(repositoryKeys));
    return this;
  }

  public ProfileRuleQuery addSeverities(String... severities) {
    this.severities.addAll(Arrays.asList(severities));
    return this;
  }

  public ProfileRuleQuery addStatuses(String... statuses) {
    this.statuses.addAll(Arrays.asList(statuses));
    return this;
  }

  public int profileId() {
    return profileId;
  }

  public @CheckForNull String nameOrKey() {
    return nameOrKey;
  }

  public Collection<String> repositoryKeys() {
    return ImmutableList.copyOf(repositoryKeys);
  }

  public Collection<String> severities() {
    return ImmutableList.copyOf(severities);
  }

  public Collection<String> statuses() {
    return ImmutableList.copyOf(statuses);
  }

  public boolean hasParentRuleCriteria() {
    return !(
      StringUtils.isEmpty(nameOrKey)
      && repositoryKeys.isEmpty()
      && statuses.isEmpty()
    );
  }
}
