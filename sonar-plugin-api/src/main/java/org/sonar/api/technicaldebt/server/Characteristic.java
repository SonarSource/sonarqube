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

package org.sonar.api.technicaldebt.server;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.technicaldebt.WorkUnit;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * @since 4.1
 */
public class Characteristic {

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

  public Characteristic setId(Integer id) {
    this.id = id;
    return this;
  }

  public String key() {
    return key;
  }

  public Characteristic setKey(String key) {
    this.key = key;
    return this;
  }

  public String name() {
    return name;
  }

  public Characteristic setName(String name) {
    this.name = name;
    return this;
  }

  public Integer order() {
    return order;
  }

  public Characteristic setOrder(Integer order) {
    this.order = order;
    return this;
  }

  @CheckForNull
  public Integer parentId() {
    return parentId;
  }

  public Characteristic setParentId(@Nullable Integer parentId) {
    this.parentId = parentId;
    return this;
  }

  @CheckForNull
  public Integer rootId() {
    return rootId;
  }

  public Characteristic setRootId(@Nullable Integer rootId) {
    this.rootId = rootId;
    return this;
  }

  public RuleKey ruleKey() {
    return ruleKey;
  }

  public Characteristic setRuleKey(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  public String function() {
    return function;
  }

  public Characteristic setFunction(String function) {
    this.function = function;
    return this;
  }

  public WorkUnit factor() {
    return factor;
  }

  public Characteristic setFactor(WorkUnit factor) {
    this.factor = factor;
    return this;
  }

  public WorkUnit offset() {
    return offset;
  }

  public Characteristic setOffset(WorkUnit offset) {
    this.offset = offset;
    return this;
  }

  public boolean isRoot() {
    return parentId == null;
  }

  public boolean isRequirement() {
    return ruleKey == null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Characteristic that = (Characteristic) o;

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
