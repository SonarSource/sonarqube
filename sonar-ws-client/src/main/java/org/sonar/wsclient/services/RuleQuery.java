/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.wsclient.services;

/**
 * @since 2.5
 */
public class RuleQuery extends Query<Rule> {
  public static final String BASE_URL = "/api/rules";

  private String language;
  private String[] repositories;
  private String searchText;
  private String profile;
  private String[] severities;
  private Boolean active;

  public RuleQuery(String language) {
    this.language = language;
  }

  public RuleQuery setLanguage(String language) {
    this.language = language;
    return this;
  }

  public String getLanguage() {
    return language;
  }

  public RuleQuery setRepositories(String... s) {
    this.repositories = s;
    return this;
  }

  public String[] getRepositories() {
    return repositories;
  }

  public RuleQuery setSearchText(String searchText) {
    this.searchText = searchText;
    return this;
  }

  public String getSearchText() {
    return searchText;
  }

  public RuleQuery setProfile(String profile) {
    this.profile = profile;
    return this;
  }

  public String getProfile() {
    return profile;
  }

  public RuleQuery setSeverities(String... severities) {
    this.severities = severities;
    return this;
  }

  public String[] getSeverities() {
    return severities;
  }

  public RuleQuery setActive(Boolean active) {
    this.active = active;
    return this;
  }

  public Boolean getStatus() {
    return active;
  }

  @Override
  public String getUrl() {
    StringBuilder url = new StringBuilder(BASE_URL);
    url.append('?');
    appendUrlParameter(url, "language", language);
    appendUrlParameter(url, "plugins", repositories);
    appendUrlParameter(url, "searchtext", searchText);
    appendUrlParameter(url, "profile", profile);
    appendUrlParameter(url, "priorities", severities);
    if (active != null) {
      appendUrlParameter(url, "status", active ? "ACTIVE" : "INACTIVE");
    }
    return url.toString();
  }

  @Override
  public Class<Rule> getModelClass() {
    return Rule.class;
  }

}
