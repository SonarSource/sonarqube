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

import java.util.*;

/**
 * @since 2.7
 */
public class Profile extends Model {

  private String language;
  private String name;
  private boolean defaultProfile;
  private String parentName;
  private List<Rule> rules = new ArrayList<Rule>();

  public String getLanguage() {
    return language;
  }

  public Profile setLanguage(String s) {
    this.language = s;
    return this;
  }

  public String getName() {
    return name;
  }

  public Profile setName(String name) {
    this.name = name;
    return this;
  }

  public boolean isDefaultProfile() {
    return defaultProfile;
  }

  public Profile setDefaultProfile(boolean b) {
    this.defaultProfile = b;
    return this;
  }

  public String getParentName() {
    return parentName;
  }

  public Profile setParentName(String s) {
    this.parentName = s;
    return this;
  }

  public List<Rule> getRules() {
    return rules;
  }

  public Rule getRule(String repositoryKey, String ruleKey) {
    for (Rule rule : rules) {
      if (repositoryKey.equals(rule.getRepository()) && ruleKey.equals(rule.getKey())) {
        return rule;
      }
    }
    return null;
  }

  public Profile addRule(Rule rule) {
    rules.add(rule);
    return this;
  }

  public static final class Rule {
    private String key;
    private String repository;
    private String severity;
    private String inheritance;
    private Map<String,String> parameters;

    public String getKey() {
      return key;
    }

    public Rule setKey(String key) {
      this.key = key;
      return this;
    }

    public String getRepository() {
      return repository;
    }

    public Rule setRepository(String repository) {
      this.repository = repository;
      return this;
    }

    public String getSeverity() {
      return severity;
    }

    public Rule setSeverity(String severity) {
      this.severity = severity;
      return this;
    }

    public String getInheritance() {
      return inheritance;
    }

    public Rule setInheritance(String inheritance) {
      this.inheritance = inheritance;
      return this;
    }

    public Map<String, String> getParameters() {
      if (parameters==null) {
        return Collections.emptyMap();
      }
      return parameters;
    }

    public String getParameter(String key) {
      return getParameters().get(key);
    }

    public Rule addParameter(String key, String value) {
      if (parameters==null) {
        parameters = new HashMap<String,String>();
      }
      parameters.put(key, value);
      return this;
    }
  }
}
