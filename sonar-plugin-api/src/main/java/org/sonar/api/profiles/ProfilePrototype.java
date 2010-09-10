/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.profiles;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.BatchComponent;
import org.sonar.api.rules.RulePriority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 2.3
 */
public final class ProfilePrototype implements BatchComponent {

  private String name;
  private String language;
  private List<RulePrototype> rules = new ArrayList<RulePrototype>();

  private ProfilePrototype() {
  }

  public static ProfilePrototype create() {
    return new ProfilePrototype();
  }

  public static ProfilePrototype create(String name, String language) {
    return new ProfilePrototype().setName(name).setLanguage(language);
  }

  public final String getName() {
    return name;
  }

  public final ProfilePrototype setName(String s) {
    this.name = s;
    return this;
  }

  public final String getLanguage() {
    return language;
  }

  public final ProfilePrototype setLanguage(String s) {
    this.language = s;
    return this;
  }

  public List<RulePrototype> getRules() {
    return rules;
  }

  public List<RulePrototype> getRulesByRepositoryKey(String repositoryKey) {
    List<RulePrototype> result = new ArrayList<RulePrototype>();
    for (RulePrototype rule : rules) {
      if (StringUtils.equals(repositoryKey, rule.getRepositoryKey())) {
        result.add(rule);
      }
    }
    return result;
  }

  public RulePrototype getRule(String repositoryKey, String key) {
    for (RulePrototype rule : rules) {
      if (StringUtils.equals(repositoryKey, rule.getRepositoryKey()) &&
          StringUtils.equals(key, rule.getKey())) {
        return rule;
      }
    }
    return null;
  }

  public RulePrototype getRuleByConfigKey(String repositoryKey, String configKey) {
    for (RulePrototype rule : rules) {
      if (StringUtils.equals(repositoryKey, rule.getRepositoryKey()) &&
          StringUtils.equals(configKey, rule.getConfigKey())) {
        return rule;
      }
    }
    return null;
  }

  /**
   * @param repositoryKey
   * @param key
   * @param nullablePriority if null, then the default rule priority is used.
   * @return the created rule
   */
  public RulePrototype activateRule(String repositoryKey, String key, RulePriority nullablePriority) {
    RulePrototype rule = RulePrototype.create(repositoryKey, key);
    rule.setPriority(nullablePriority);
    rules.add(rule);
    return rule;
  }

  public RulePrototype activateRule(RulePrototype rule) {
    rules.add(rule);
    return rule;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("name", name)
        .append("language", language)
        .toString();
  }

  public static final class RulePrototype {
    private String repositoryKey;

    private String key;

    private String configKey;

    private RulePriority priority = null;

    private Map<String, String> parameters = new HashMap<String, String>();

    private RulePrototype() {
    }

    public static RulePrototype create() {
      return new RulePrototype();
    }

    public static RulePrototype create(String repositoryKey, String key) {
      return new RulePrototype().setRepositoryKey(repositoryKey).setKey(key);
    }

    public static RulePrototype createByConfigKey(String repositoryKey, String configKey) {
      return new RulePrototype().setRepositoryKey(repositoryKey).setConfigKey(configKey);
    }

    public String getRepositoryKey() {
      return repositoryKey;
    }

    public RulePrototype setRepositoryKey(String s) {
      this.repositoryKey = s;
      return this;
    }

    public String getKey() {
      return key;
    }

    public RulePrototype setKey(String s) {
      this.key = s;
      return this;
    }

    public String getConfigKey() {
      return configKey;
    }

    public RulePrototype setConfigKey(String s) {
      this.configKey = s;
      return this;
    }

    public RulePriority getPriority() {
      return priority;
    }

    public RulePrototype setPriority(RulePriority p) {
      this.priority = p;
      return this;
    }

    public RulePrototype setParameter(String key, String value) {
      parameters.put(key, value);
      return this;
    }

    public Map<String, String> getParameters() {
      return parameters;
    }

    public String getParameter(String key) {
      return parameters.get(key);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder().append("[repository=").append(repositoryKey);
      if (StringUtils.isNotBlank(key)) {
        sb.append(",key=").append(key);
      }
      if (StringUtils.isNotBlank(configKey)) {
        sb.append(",configKey=").append(configKey);
      }
      return sb.append("]").toString();
    }
  }
}
