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
package org.sonar.db.rule;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sonar.api.rule.RuleKey;

public class RuleExtensionForIndexingDto {

  private static final Splitter TAGS_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private int ruleId;
  private String pluginName;
  private String pluginRuleKey;
  private String organizationUuid;
  private String tags;

  public String getPluginName() {
    return pluginName;
  }

  public RuleExtensionForIndexingDto setPluginName(String pluginName) {
    this.pluginName = pluginName;
    return this;
  }

  public String getPluginRuleKey() {
    return pluginRuleKey;
  }

  public RuleExtensionForIndexingDto setPluginRuleKey(String pluginRuleKey) {
    this.pluginRuleKey = pluginRuleKey;
    return this;
  }

  public int getRuleId() {
    return ruleId;
  }

  public void setRuleId(int ruleId) {
    this.ruleId = ruleId;
  }

  public String getOrganizationUuid() {
    return organizationUuid;
  }

  public RuleExtensionForIndexingDto setOrganizationUuid(String organizationUuid) {
    this.organizationUuid = organizationUuid;
    return this;
  }

  public String getTags() {
    return tags;
  }

  public RuleExtensionForIndexingDto setTags(String tags) {
    this.tags = tags;
    return this;
  }

  public RuleKey getRuleKey() {
    return RuleKey.of(pluginName, pluginRuleKey);
  }

  public Set<String> getTagsAsSet() {
    return ImmutableSet.copyOf(TAGS_SPLITTER.split(tags == null ? "" : tags));
  }

}
