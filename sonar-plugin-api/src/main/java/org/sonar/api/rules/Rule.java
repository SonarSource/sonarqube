/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.rules;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.rule.RuleKey;
import org.sonar.check.Cardinality;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

public class Rule {

  /**
   * @since 3.6
   */
  public static final String STATUS_BETA = "BETA";
  /**
   * @since 3.6
   */
  public static final String STATUS_DEPRECATED = "DEPRECATED";
  /**
   * @since 3.6
   */
  public static final String STATUS_READY = "READY";

  /**
   * For internal use only.
   *
   * @since 3.6
   */
  public static final String STATUS_REMOVED = "REMOVED";

  /**
   * List of available status
   *
   * @since 3.6
   */
  private static final Set<String> STATUS_LIST = unmodifiableSet(new LinkedHashSet<>(asList(STATUS_READY, STATUS_BETA, STATUS_DEPRECATED, STATUS_REMOVED)));

  private Integer id;

  /**
   * The default priority given to a rule if not explicitly set
   */
  public static final RulePriority DEFAULT_PRIORITY = RulePriority.MAJOR;

  private String name;
  private String key;
  private String configKey;
  private RulePriority priority = DEFAULT_PRIORITY;
  private String description;
  private String pluginName;
  private boolean isTemplate = false;
  private String status = STATUS_READY;
  private String language;
  private Rule template = null;
  private List<RuleParam> params = new ArrayList<>();
  private Date createdAt;
  private Date updatedAt;
  private String tags;
  private String systemTags;

  /**
   * @deprecated since 2.3. Use the factory method {@link #create()}
   */
  @Deprecated
  public Rule() {
  }

  /**
   * Creates rule with minimum set of info
   *
   * @param pluginName the plugin name indicates which plugin the rule belongs to
   * @param key        the key should be unique within a plugin, but it is even more careful for the time being that it is unique across the
   *                   application
   * @deprecated since 2.3. Use the factory method {@link #create()}
   */
  @Deprecated
  public Rule(String pluginName, String key) {
    this.pluginName = pluginName;
    this.key = key;
    this.configKey = key;
  }

  public Integer getId() {
    return id;
  }

  /**
   * @deprecated since 2.3. visibility should be decreased to protected or package
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

  /**
   * Sets the rule description
   */
  public Rule setDescription(String description) {
    this.description = StringUtils.strip(description);
    return this;
  }

  public Boolean isEnabled() {
    return !STATUS_REMOVED.equals(status);
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
    RuleParam parameter = new RuleParam()
      .setRule(this);
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

  /**
   * @since 2.5
   */
  public RulePriority getSeverity() {
    return priority;
  }

  /**
   * @param severity severity to set, if null, uses the default priority.
   * @since 2.5
   */
  public Rule setSeverity(RulePriority severity) {
    if (severity == null) {
      this.priority = DEFAULT_PRIORITY;
    } else {
      this.priority = severity;
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

  /**
   * @since 4.4
   */
  public boolean isTemplate() {
    return isTemplate;
  }

  /**
   * @since 4.4
   */
  public Rule setIsTemplate(boolean isTemplate) {
    this.isTemplate = isTemplate;
    return this;
  }

  /**
   * @deprecated since 4.4, use {@link #isTemplate()}
   */
  @Deprecated
  public Cardinality getCardinality() {
    return isTemplate ? Cardinality.MULTIPLE : Cardinality.SINGLE;
  }

  /**
   * @deprecated since 4.4, use {@link #setIsTemplate(boolean)}
   */
  @Deprecated
  public Rule setCardinality(Cardinality c) {
    this.isTemplate = Cardinality.MULTIPLE.equals(c);
    return this;
  }

  /**
   * @deprecated since 4.4, use {@link #getTemplate()}
   */
  @Deprecated
  public Rule getParent() {
    return getTemplate();
  }

  /**
   * @deprecated since 4.4, use {@link #setTemplate(Rule)}}
   */
  @Deprecated
  public Rule setParent(Rule parent) {
    return setTemplate(parent);
  }

  /**
   * @since 4.4
   */
  public Rule getTemplate() {
    return template;
  }

  /**
   * @since 4.4
   */
  public Rule setTemplate(Rule template) {
    this.template = template;
    return this;
  }

  /**
   * @since 3.6
   */
  public String getStatus() {
    return status;
  }

  /**
   * @since 3.6
   */
  public Rule setStatus(String status) {
    if (!STATUS_LIST.contains(status)) {
      throw new IllegalStateException("The status of a rule can only contain : " + String.join(", ", STATUS_LIST));
    }
    this.status = status;
    return this;
  }

  /**
   * @since 3.6
   */
  public Date getCreatedAt() {
    return createdAt;
  }

  /**
   * @since 3.6
   */
  public Rule setCreatedAt(Date d) {
    this.createdAt = d;
    return this;
  }

  /**
   * @since 3.6
   */
  public Date getUpdatedAt() {
    return updatedAt;
  }

  /**
   * @since 3.6
   */
  public Rule setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  /**
   * @since 3.6
   */
  public String getLanguage() {
    return language;
  }

  /**
   * For internal use only.
   *
   * @since 3.6
   */
  public Rule setLanguage(String language) {
    this.language = language;
    return this;
  }

  /**
   * For definition of rule only
   */
  public String[] getTags() {
    return tags == null ? new String[0] : StringUtils.split(tags, ',');
  }

  /**
   * For definition of rule only
   */
  public Rule setTags(String[] tags) {
    this.tags = tags == null ? null : StringUtils.join(tags, ',');
    return this;
  }

  /**
   * For internal use
   */
  public String[] getSystemTags() {
    return systemTags == null ? new String[0] : StringUtils.split(systemTags, ',');
  }

  public Rule setSystemTags(String[] tags) {
    this.systemTags = tags == null ? null : StringUtils.join(tags, ',');
    return this;
  }

  /**
   * For internal use only.
   *
   * @since 4.3
   * @deprecated since 4.4, use {@link #getCharacteristicKey()}
   */
  @CheckForNull
  @Deprecated
  public Integer getCharacteristicId() {
    return null;
  }

  /**
   * For internal use only.
   *
   * @since 4.3
   * @deprecated since 4.4, use {@link #setCharacteristicKey(String)}
   */
  @Deprecated
  public Rule setCharacteristicId(@Nullable Integer characteristicId) {
    return this;
  }

  /**
   * For internal use only.
   *
   * @since 4.3
   * @deprecated since 4.4, use {@link #getDefaultCharacteristicKey()}
   */
  @CheckForNull
  @Deprecated
  public Integer getDefaultCharacteristicId() {
    return null;
  }

  /**
   * For internal use only.
   *
   * @since 4.3
   * @deprecated since 4.4, use {@link #setDefaultCharacteristicKey(String)}
   */
  @Deprecated
  public Rule setDefaultCharacteristicId(@Nullable Integer defaultCharacteristicId) {
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
      .append(pluginName, other.getRepositoryKey())
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
    // Note that ReflectionToStringBuilder will not work here - see SONAR-3077
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
      .append("id", id)
      .append("name", name)
      .append("key", key)
      .append("configKey", configKey)
      .append("plugin", pluginName)
      .append("severity", priority)
      .append("isTemplate", isTemplate())
      .append("status", status)
      .append("language", language)
      .append("template", template)
      .toString();
  }

  @CheckForNull
  private static String removeNewLineCharacters(@Nullable String text) {
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

  /**
   * Create with all required fields
   *
   * @since 2.10
   */
  public static Rule create(String repositoryKey, String key) {
    return new Rule().setUniqueKey(repositoryKey, key);
  }

  /**
   * @since 3.6
   */
  public RuleKey ruleKey() {
    return RuleKey.of(getRepositoryKey(), getKey());
  }

  /**
   * @since 4.4
   * @deprecated in 5.5. SQALE Quality Model is replaced by SonarQube Quality Model.
   */
  @CheckForNull
  @Deprecated
  public String getDefaultCharacteristicKey() {
    return null;
  }

  /**
   * @since 4.4
   * @deprecated in 5.5. SQALE Quality Model is replaced by SonarQube Quality Model.
   */
  @Deprecated
  public Rule setDefaultCharacteristicKey(@Nullable String defaultCharacteristicKey) {
    return this;
  }

  /**
   * @since 4.4
   * @deprecated in 5.5. SQALE Quality Model is replaced by SonarQube Quality Model.
   */
  @CheckForNull
  @Deprecated
  public String getDefaultSubCharacteristicKey() {
    return null;
  }

  /**
   * @since 4.4
   * @deprecated in 5.5. SQALE Quality Model is replaced by SonarQube Quality Model.
   */
  @Deprecated
  public Rule setDefaultSubCharacteristicKey(@Nullable String defaultSubCharacteristicKey) {
    return this;
  }

  /**
   * @since 4.4
   * @deprecated in 5.5. SQALE Quality Model is replaced by SonarQube Quality Model.
   */
  @CheckForNull
  @Deprecated
  public String getCharacteristicKey() {
    return null;
  }

  /**
   * @since 4.4
   * @deprecated in 5.5. SQALE Quality Model is replaced by SonarQube Quality Model.
   */
  @Deprecated
  public Rule setCharacteristicKey(@Nullable String characteristicKey) {
    return this;
  }

  /**
   * @since 4.4
   * @deprecated in 5.5. SQALE Quality Model is replaced by SonarQube Quality Model.
   */
  @CheckForNull
  @Deprecated
  public String getSubCharacteristicKey() {
    return null;
  }

  /**
   * @since 4.4
   * @deprecated in 5.5. SQALE Quality Model is replaced by SonarQube Quality Model.
   */
  @Deprecated
  public Rule setSubCharacteristicKey(@Nullable String subCharacteristicKey) {
    return this;
  }
}
