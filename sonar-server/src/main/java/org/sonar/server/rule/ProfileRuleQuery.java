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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.QProfileRule;
import org.sonar.server.util.RubyUtils;

import javax.annotation.CheckForNull;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 4.2
 */
public class ProfileRuleQuery {

  private static final String INHERITANCE_ANY = "any";
  private static final String INHERITANCE_NOT = "NOT";
  private static final Set<String> AUTHORIZED_INHERITANCE_PARAMS = ImmutableSet.of(QProfileRule.INHERITED, QProfileRule.OVERRIDES, INHERITANCE_ANY, INHERITANCE_NOT);

  private static final String PARAM_PROFILE_ID = "profileId";
  private static final String PARAM_LANGUAGE = "language";
  private static final String PARAM_NAME_OR_KEY = "nameOrKey";
  private static final String PARAM_REPOSITORY_KEYS = "repositoryKeys";
  private static final String PARAM_SEVERITIES = "severities";
  private static final String PARAM_STATUSES = "statuses";
  private static final String PARAM_INHERITANCE = "inheritance";

  private int profileId;
  private String language;
  private String nameOrKey;
  private List<String> repositoryKeys;
  private List<String> severities;
  private List<String> statuses;
  private String inheritance;
  private boolean anyInheritance;
  private boolean noInheritance;

  private ProfileRuleQuery() {
    repositoryKeys = Lists.newArrayList();
    severities = Lists.newArrayList();
    statuses = Lists.newArrayList();
  }

  public static ProfileRuleQuery parse(Map<String, Object> params) {
    List<BadRequestException.Message> errors = newArrayList();

    validatePresenceOf(params, errors, PARAM_PROFILE_ID);

    ProfileRuleQuery result = new ProfileRuleQuery();

    if (params.containsKey(PARAM_LANGUAGE)) {
      result.setLanguage((String) params.get(PARAM_LANGUAGE));
    }
    if (params.containsKey(PARAM_NAME_OR_KEY)) {
      result.setNameOrKey((String) params.get(PARAM_NAME_OR_KEY));
    }
    if (params.get(PARAM_REPOSITORY_KEYS) != null) {
      result.addRepositoryKeys(optionalVarargs(params.get(PARAM_REPOSITORY_KEYS)));
    }
    if (params.get(PARAM_SEVERITIES) != null) {
      result.addSeverities(optionalVarargs(params.get(PARAM_SEVERITIES)));
    }
    if (params.get(PARAM_STATUSES) != null) {
      result.addStatuses(optionalVarargs(params.get(PARAM_STATUSES)));
    }

    if (params.containsKey(PARAM_INHERITANCE)) {
      String inheritance = (String) params.get(PARAM_INHERITANCE);
      validateInheritance(inheritance, errors);
      if (inheritance.equals(INHERITANCE_ANY)) {
        result.setAnyInheritance(true);
      } else if (inheritance.equals(INHERITANCE_NOT)) {
        result.setNoInheritance(true);
      } else {
        result.setInheritance(inheritance);
      }
    } else {
      result.setAnyInheritance(true);
    }

    if (!errors.isEmpty()) {
      throw BadRequestException.of("Incorrect rule search parameters", errors);
    } else {
      result.profileId = RubyUtils.toInteger(params.get(PARAM_PROFILE_ID));
    }
    return result;
  }

  private static void validatePresenceOf(Map<String, Object> params, List<BadRequestException.Message> errors, String... paramNames) {
    for (String param : paramNames) {
      if (params.get(param) == null) {
        errors.add(BadRequestException.Message.of("Missing parameter " + param));
      }
    }
  }

  private static void validateInheritance(String value, List<BadRequestException.Message> errors) {
    if (!AUTHORIZED_INHERITANCE_PARAMS.contains(value)) {
      errors.add(BadRequestException.Message.of("Wrong inheritance param, should be one of " + AUTHORIZED_INHERITANCE_PARAMS));
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

  public ProfileRuleQuery setLanguage(String language) {
    this.language = language;
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

  public ProfileRuleQuery setInheritance(String inheritance) {
    this.inheritance = inheritance;
    return this;
  }

  public ProfileRuleQuery setAnyInheritance(boolean anyInheritance) {
    this.anyInheritance = anyInheritance;
    return this;
  }

  public ProfileRuleQuery setNoInheritance(boolean noInheritance) {
    this.noInheritance = noInheritance;
    return this;
  }

  public int profileId() {
    return profileId;
  }

  @CheckForNull
  public String language() {
    return language;
  }

  @CheckForNull
  public String nameOrKey() {
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

  @CheckForNull
  public String inheritance() {
    return inheritance;
  }

  public boolean anyInheritance() {
    return anyInheritance;
  }

  public boolean noInheritance() {
    return noInheritance;
  }

  private static String[] optionalVarargs(Object jRubyArray) {
    List<String> items = RubyUtils.toStrings(jRubyArray);
    String[] empty = new String[0];
    if (items == null) {
      return empty;
    } else {
      items.remove("");
      return items.toArray(empty);
    }
  }
}
