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

package org.sonar.api.technicaldebt;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class Characteristic {

  private Integer id;
  private String key;
  private String name;
  private Integer order;
  private Characteristic parent;
  private Characteristic root;
  private List<Characteristic> children;
  private List<Requirement> requirements;

  private Date createdAt;
  private Date updatedAt;

  public Characteristic() {
    this.children = newArrayList();
    this.requirements = newArrayList();
  }

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
    this.key = StringUtils.trimToNull(key);
    return this;
  }

  public String name() {
    return name;
  }

  public Characteristic setName(String name) {
    return setName(name, false);
  }

  public Characteristic setName(String s, boolean asKey) {
    this.name = StringUtils.trimToNull(s);
    if (asKey) {
      this.key = StringUtils.upperCase(this.name);
      this.key = StringUtils.replaceChars(this.key, ' ', '_');
    }
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
  public Characteristic parent() {
    return parent;
  }

  public Characteristic setParent(@Nullable Characteristic parent) {
    if (parent != null) {
      this.parent = parent;
      parent.addChild(this);
    }
    return this;
  }

  @CheckForNull
  public Characteristic getRoot() {
    return root;
  }

  public Characteristic setRoot(@Nullable Characteristic root) {
    this.root = root;
    return this;
  }

  public List<Characteristic> children() {
    return children;
  }

  private Characteristic addChild(Characteristic child){
    this.children.add(child);
    return this;
  }

  public List<Requirement> requirements() {
    return requirements;
  }

  protected Characteristic addRequirement(Requirement requirement){
    this.requirements.add(requirement);
    return this;
  }

  public Characteristic removeRequirement(Requirement requirement){
    this.requirements.remove(requirement);
    return this;
  }

  public boolean isRoot(){
    return parent == null;
  }

  public Date createdAt() {
    return createdAt;
  }

  public Characteristic setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date updatedAt() {
    return updatedAt;
  }

  public Characteristic setUpdatedAt(Date updatedAt) {
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
    Characteristic that = (Characteristic) o;
    return key.equals(that.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }
}
