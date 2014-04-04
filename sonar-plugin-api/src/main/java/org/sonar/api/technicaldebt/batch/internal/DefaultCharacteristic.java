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

package org.sonar.api.technicaldebt.batch.internal;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.technicaldebt.batch.Characteristic;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @deprecated since 4.3
 */
@Deprecated
public class DefaultCharacteristic implements Characteristic {

  private Integer id;
  private String key;
  private String name;
  private Integer order;
  private DefaultCharacteristic parent;
  private DefaultCharacteristic root;
  private List<DefaultCharacteristic> children;
  private List<DefaultRequirement> requirements;

  private Date createdAt;
  private Date updatedAt;

  public DefaultCharacteristic() {
    this.children = newArrayList();
    this.requirements = newArrayList();
  }

  public Integer id() {
    return id;
  }

  public DefaultCharacteristic setId(Integer id) {
    this.id = id;
    return this;
  }

  public String key() {
    return key;
  }

  public DefaultCharacteristic setKey(String key) {
    this.key = StringUtils.trimToNull(key);
    return this;
  }

  public String name() {
    return name;
  }

  public DefaultCharacteristic setName(String name) {
    this.name = name;
    return this;
  }

  public DefaultCharacteristic setName(String s, boolean asKey) {
    this.name = StringUtils.trimToNull(s);
    if (asKey) {
      this.key = StringUtils.upperCase(this.name);
      this.key = StringUtils.replaceChars(this.key, ' ', '_');
    }
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
  public DefaultCharacteristic parent() {
    return parent;
  }

  public DefaultCharacteristic setParent(@Nullable DefaultCharacteristic parent) {
    if (parent != null) {
      this.parent = parent;
      parent.addChild(this);
    }
    return this;
  }

  @CheckForNull
  public DefaultCharacteristic root() {
    return root;
  }

  public DefaultCharacteristic setRoot(@Nullable DefaultCharacteristic root) {
    this.root = root;
    return this;
  }

  public List<DefaultCharacteristic> children() {
    return children;
  }

  private DefaultCharacteristic addChild(DefaultCharacteristic child) {
    this.children.add(child);
    return this;
  }

  public List<DefaultRequirement> requirements() {
    return requirements;
  }

  public DefaultCharacteristic addRequirement(DefaultRequirement requirement) {
    this.requirements.add(requirement);
    return this;
  }

  public boolean isRoot() {
    return parent == null;
  }

  public Date createdAt() {
    return createdAt;
  }

  public DefaultCharacteristic setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  @CheckForNull
  public Date updatedAt() {
    return updatedAt;
  }

  public DefaultCharacteristic setUpdatedAt(@Nullable Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
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
    return key.equals(that.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }
}
