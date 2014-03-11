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
package org.sonar.wsclient.services;

/**
 * @since 2.7
 */
public final class ProfileQuery extends Query<Profile> {
  public static final String BASE_URL = "/api/profiles";

  private String language;
  private String name;//optional
  private String[] ruleRepositories;//optional
  private String[] ruleSeverities;//optional

  private ProfileQuery(String language) {
    this.language = language;
  }

  public String getLanguage() {
    return language;
  }

  public String getName() {
    return name;
  }

  public String[] getRuleRepositories() {
    return ruleRepositories;
  }

  public String[] getRuleSeverities() {
    return ruleSeverities;
  }

  public ProfileQuery setName(String name) {
    this.name = name;
    return this;
  }

  public ProfileQuery setRuleRepositories(String[] ruleRepositories) {
    this.ruleRepositories = ruleRepositories;
    return this;
  }

  public ProfileQuery setRuleSeverities(String[] ruleSeverities) {
    this.ruleSeverities = ruleSeverities;
    return this;
  }

  @Override
  public Class<Profile> getModelClass() {
    return Profile.class;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder(BASE_URL);
    url.append('?');
    appendUrlParameter(url, "language", language);
    appendUrlParameter(url, "name", name);
    appendUrlParameter(url, "rule_repositories", ruleRepositories);
    appendUrlParameter(url, "rule_severities", ruleSeverities);
    return url.toString();
  }

  public static ProfileQuery createWithLanguage(String language) {
    return new ProfileQuery(language);
  }

  public static ProfileQuery create(String language, String name) {
    return new ProfileQuery(language).setName(name);
  }
}
