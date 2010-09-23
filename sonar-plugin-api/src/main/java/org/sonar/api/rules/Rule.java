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
package org.sonar.api.rules;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.database.DatabaseProperties;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rules")
public final class Rule {

  public static enum Cardinality {
    SINGLE, MULTIPLE
  }

  @Id
  @Column(name = "id")
  @GeneratedValue
  private Integer id;

  /**
   * The default priority given to a rule if not explicitely set
   */
  public static final RulePriority DEFAULT_PRIORITY = RulePriority.MAJOR;

  @Column(name = "name", updatable = true, nullable = false)
  private String name;

  @Column(name = "plugin_rule_key", updatable = false, nullable = true)
  private String key;

  @Column(name = "enabled", updatable = true, nullable = true)
  private Boolean enabled = Boolean.TRUE;

  @Column(name = "plugin_config_key", updatable = true, nullable = true)
  private String configKey;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "rules_category_id", updatable = true, nullable = true)
  private RulesCategory rulesCategory;

  @Column(name = "priority", updatable = true, nullable = true)
  @Enumerated(EnumType.ORDINAL)
  private RulePriority priority = DEFAULT_PRIORITY;

  @Column(name = "description", updatable = true, nullable = true, length = DatabaseProperties.MAX_TEXT_SIZE)
  private String description;

  @Column(name = "plugin_name", updatable = true, nullable = false)
  private String pluginName;

  @Enumerated(EnumType.STRING)
  @Column(name = "cardinality", updatable = true, nullable = false)
  private Cardinality cardinality = Cardinality.SINGLE;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "parent_id", updatable = true, nullable = true)
  private Rule parent = null;

  @org.hibernate.annotations.Cascade(
      {org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN}
)
  @OneToMany(mappedBy = "rule")
  private List<RuleParam> params = new ArrayList<RuleParam>();

  /**
   * @deprecated since 2.3. Use the factory method create()
   */
  @Deprecated
  public Rule() {
    // TODO reduce visibility to package
  }

  /**
   * Creates rule with minimum set of info
   *
   * @param pluginName the plugin name indicates which plugin the rule belongs to
   * @param key        the key should be unique within a plugin, but it is even more careful for the time being that it is unique
   *                   across the application
   * @deprecated since 2.3. Use the factory method create()
   */
  @Deprecated
  public Rule(String pluginName, String key) {
    this.pluginName = pluginName;
    this.key = key;
    this.configKey = key;
  }

  /**
   * Creates a fully qualified rule
   *
   * @param pluginKey     the plugin the rule belongs to
   * @param key           the key should be unique within a plugin, but it is even more careful for the time being that it is unique
   *                      across the application
   * @param name          the name displayed in the UI
   * @param rulesCategory the ISO category the rule belongs to
   * @param priority      this is the priority associated to the rule
   * @deprecated since 2.3. Use the factory method create()
   */
  @Deprecated
  public Rule(String pluginKey, String key, String name, RulesCategory rulesCategory, RulePriority priority) {
    setName(name);
    this.key = key;
    this.configKey = key;
    this.rulesCategory = rulesCategory;
    this.priority = priority;
    this.pluginName = pluginKey;
  }

  /**
   * @deprecated Use the factory method create()
   */
  @Deprecated
  public Rule(String name, String key, RulesCategory rulesCategory, String pluginName, String description) {
    this();
    setName(name);
    this.key = key;
    this.configKey = key;
    this.rulesCategory = rulesCategory;
    this.pluginName = pluginName;
    this.description = description;
  }

  /**
   * @deprecated since 2.3. Use the factory method create()
   */
  @Deprecated
  public Rule(String name, String key, String configKey, RulesCategory rulesCategory, String pluginName, String description) {
    this();
    setName(name);
    this.key = key;
    this.configKey = configKey;
    this.rulesCategory = rulesCategory;
    this.pluginName = pluginName;
    this.description = description;
  }

  public Integer getId() {
    return id;
  }

  /**
   * @deprecated visibility should be decreased to protected or package
   */
  @Deprecated
  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  /**
   * Sets the rule name
   */
  public Rule setName(String name) {
    this.name = removeNewLineCharacters(name);
    return this;
  }

  public String getKey() {
    return key;
  }

  /**
   * Sets the rule key
   */
  public Rule setKey(String key) {
    this.key = key;
    return this;
  }

  /**
   * @return the rule category
   */
  public RulesCategory getRulesCategory() {
    return rulesCategory;
  }

  /**
   * Sets the rule category
   */
  public Rule setRulesCategory(RulesCategory rulesCategory) {
    this.rulesCategory = rulesCategory;
    return this;
  }

  public String getPluginName() {
    return pluginName;
  }

  /**
   * Sets the plugin name the rule belongs to
   */
  public Rule setPluginName(String pluginName) {
    this.pluginName = pluginName;
    return this;
  }

  public String getConfigKey() {
    return configKey;
  }

  /**
   * Sets the configuration key
   */
  public Rule setConfigKey(String configKey) {
    this.configKey = configKey;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public Boolean isEnabled() {
    return enabled;
  }

  /**
   * Do not call. Used only by sonar.
   */
  public Rule setEnabled(Boolean b) {
    this.enabled = b;
    return this;
  }

  /**
   * Sets the rule description
   */
  public Rule setDescription(String description) {
    this.description = StringUtils.strip(description);
    return this;
  }

  public List<RuleParam> getParams() {
    return params;
  }

  public RuleParam getParam(String key) {
    for (RuleParam param : params) {
      if (StringUtils.equals(key, param.getKey())) {
        return param;
      }
    }
    return null;
  }

  /**
   * Sets the rule parameters
   */
  public Rule setParams(List<RuleParam> params) {
    this.params.clear();
    for (RuleParam param : params) {
      param.setRule(this);
      this.params.add(param);
    }
    return this;
  }

  public RuleParam createParameter() {
    RuleParam parameter = new RuleParam();
    parameter.setRule(this);
    params.add(parameter);
    return parameter;
  }

  public RuleParam createParameter(String key) {
    RuleParam parameter = new RuleParam()
        .setKey(key)
        .setRule(this);
    params.add(parameter);
    return parameter;
  }

  public Integer getCategoryId() {
    if (rulesCategory != null) {
      return rulesCategory.getId();
    }
    return null;
  }

  public RulePriority getPriority() {
    return priority;
  }

  /**
   * Sets the rule priority. If null, uses the default priority
   */
  public Rule setPriority(RulePriority priority) {
    if (priority == null) {
      this.priority = DEFAULT_PRIORITY;
    } else {
      this.priority = priority;
    }

    return this;
  }

  public String getRepositoryKey() {
    return pluginName;
  }

  public Rule setRepositoryKey(String s) {
    this.pluginName = s;
    return this;
  }

  public Rule setUniqueKey(String repositoryKey, String key) {
    return setRepositoryKey(repositoryKey).setKey(key).setConfigKey(key);
  }

  public Cardinality getCardinality() {
    return cardinality;
  }

  public Rule setCardinality(Cardinality c) {
    this.cardinality = c;
    return this;
  }

  public Rule getParent() {
    return parent;
  }

  public Rule setParent(Rule parent) {
    this.parent = parent;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Rule)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    Rule other = (Rule) obj;
    return new EqualsBuilder()
        .append(pluginName, other.getPluginName())
        .append(key, other.getKey())
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(pluginName)
        .append(key)
        .toHashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("id", getId())
        .append("name", name)
        .append("key", key)
        .append("configKey", configKey)
        .append("categ", rulesCategory)
        .append("plugin", pluginName)
        .toString();
  }

  private String removeNewLineCharacters(String text) {
    String removedCRLF = StringUtils.remove(text, "\n");
    removedCRLF = StringUtils.remove(removedCRLF, "\r");
    removedCRLF = StringUtils.remove(removedCRLF, "\n\r");
    removedCRLF = StringUtils.remove(removedCRLF, "\r\n");
    return removedCRLF;
  }

  public static Rule create() {
    return new Rule();
  }

  /**
   * Create with all required fields
   */
  public static Rule create(String repositoryKey, String key, String name) {
    return new Rule().setUniqueKey(repositoryKey, key).setName(name);
  }

}
