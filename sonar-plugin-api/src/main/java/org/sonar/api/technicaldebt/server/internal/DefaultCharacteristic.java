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

package org.sonar.api.technicaldebt.server.internal;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.technicaldebt.server.Characteristic;
import org.sonar.api.utils.WorkUnit;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * @since 4.1
 */
public class DefaultCharacteristic implements Characteristic {

  private Integer id;
  private String key;
  private String name;
  private Integer order;
  private Integer parentId;
  private Integer rootId;
  private RuleKey ruleKey;
  private String function;
  private WorkUnit factor;
  private WorkUnit offset;

  public Integer id() {
    return id;
  }

  public DefaultCharacteristic setId(Integer id) {
    this.id = id;
    return this;
  }

  @CheckForNull
  public String key() {
    return key;
  }

  public DefaultCharacteristic setKey(@Nullable String key) {
    this.key = key;
    return this;
  }

  @CheckForNull
  public String name() {
    return name;
  }

  public DefaultCharacteristic setName(@Nullable String name) {
    this.name = name;
    return this;
  }

  @CheckForNull
  public Integer order() {
    return order;
  }

  public DefaultCharacteristic setOrder(@Nullable Integer order) {
    this.order = order;
    return this;
  }

  @CheckForNull
  public Integer parentId() {
    return parentId;
  }

  public DefaultCharacteristic setParentId(@Nullable Integer parentId) {
    this.parentId = parentId;
    return this;
  }

  @CheckForNull
  public Integer rootId() {
    return rootId;
  }

  public DefaultCharacteristic setRootId(@Nullable Integer rootId) {
    this.rootId = rootId;
    return this;
  }

  @CheckForNull
  public RuleKey ruleKey() {
    return ruleKey;
  }

  public DefaultCharacteristic setRuleKey(@Nullable RuleKey ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  @CheckForNull
  public String function() {
    return function;
  }

  public DefaultCharacteristic setFunction(@Nullable String function) {
    this.function = function;
    return this;
  }

  @CheckForNull
  public WorkUnit factor() {
    return factor;
  }

  public DefaultCharacteristic setFactor(@Nullable WorkUnit factor) {
    this.factor = factor;
    return this;
  }

  @CheckForNull
  public WorkUnit offset() {
    return offset;
  }

  public DefaultCharacteristic setOffset(@Nullable WorkUnit offset) {
    this.offset = offset;
    return this;
  }

  public boolean isRoot() {
    return parentId == null;
  }

  public boolean isRequirement() {
    return ruleKey != null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DefaultCharacteristic that = (DefaultCharacteristic) o;

    if (key != null ? !key.equals(that.key) : that.key != null) {
      return false;
    }
    if (ruleKey != null ? !ruleKey.equals(that.ruleKey) : that.ruleKey != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = key != null ? key.hashCode() : 0;
    result = 31 * result + (ruleKey != null ? ruleKey.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

}
