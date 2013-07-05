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
package org.sonar.wsclient.services;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class Dependency extends Model {

  private String id;
  private Long fromId;
  private Long toId;
  private String usage;
  private Integer weight;
  private String fromKey;
  private String fromName;
  private String fromQualifier;
  private String toKey;
  private String toName;
  private String toQualifier;

  @CheckForNull
  public String getId() {
    return id;
  }

  public Dependency setId(String id) {
    this.id = id;
    return this;
  }

  public long getFromId() {
    return fromId;
  }

  public Dependency setFromId(@Nullable Long fromId) {
    this.fromId = fromId;
    return this;
  }

  public long getToId() {
    return toId;
  }

  public Dependency setToId(@Nullable Long toId) {
    this.toId = toId;
    return this;
  }

  @CheckForNull
  public String getFromKey() {
    return fromKey;
  }

  public Dependency setFromKey(@Nullable String fromKey) {
    this.fromKey = fromKey;
    return this;
  }

  @CheckForNull
  public String getToKey() {
    return toKey;
  }

  public Dependency setToKey(@Nullable String toKey) {
    this.toKey = toKey;
    return this;
  }

  @CheckForNull
  public String getUsage() {
    return usage;
  }

  public Dependency setUsage(@Nullable String usage) {
    this.usage = usage;
    return this;
  }

  @CheckForNull
  public Integer getWeight() {
    return weight;
  }

  public Dependency setWeight(@Nullable Integer weight) {
    this.weight = weight;
    return this;
  }

  @CheckForNull
  public String getFromName() {
    return fromName;
  }

  public Dependency setFromName(@Nullable String fromName) {
    this.fromName = fromName;
    return this;
  }

  @CheckForNull
  public String getFromQualifier() {
    return fromQualifier;
  }

  public Dependency setFromQualifier(@Nullable String fromQualifier) {
    this.fromQualifier = fromQualifier;
    return this;
  }

  @CheckForNull
  public String getToName() {
    return toName;
  }

  public Dependency setToName(@Nullable String toName) {
    this.toName = toName;
    return this;
  }

  @CheckForNull
  public String getToQualifier() {
    return toQualifier;
  }

  public Dependency setToQualifier(@Nullable String toQualifier) {
    this.toQualifier = toQualifier;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Dependency that = (Dependency) o;
    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public String toString() {
    return new StringBuilder()
      .append(fromKey)
      .append(" -> ")
      .append(toKey)
      .toString();
  }
}
