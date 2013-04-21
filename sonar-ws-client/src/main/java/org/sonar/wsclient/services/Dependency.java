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

public class Dependency extends Model {

  private String id;
  private long fromId;
  private long toId;
  private String usage;
  private int weight;
  private String fromKey;
  private String fromName;
  private String fromQualifier;
  private String toKey;
  private String toName;
  private String toQualifier;

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

  public Dependency setFromId(long fromId) {
    this.fromId = fromId;
    return this;
  }

  public long getToId() {
    return toId;
  }

  public Dependency setToId(long toId) {
    this.toId = toId;
    return this;
  }

  public String getFromKey() {
    return fromKey;
  }

  public Dependency setFromKey(String fromKey) {
    this.fromKey = fromKey;
    return this;
  }

  public String getToKey() {
    return toKey;
  }

  public Dependency setToKey(String toKey) {
    this.toKey = toKey;
    return this;
  }

  public String getUsage() {
    return usage;
  }

  public Dependency setUsage(String usage) {
    this.usage = usage;
    return this;
  }

  public Integer getWeight() {
    return weight;
  }

  public Dependency setWeight(Integer weight) {
    this.weight = weight;
    return this;
  }

  public String getFromName() {
    return fromName;
  }

  public Dependency setFromName(String fromName) {
    this.fromName = fromName;
    return this;
  }

  public String getFromQualifier() {
    return fromQualifier;
  }

  public Dependency setFromQualifier(String fromQualifier) {
    this.fromQualifier = fromQualifier;
    return this;
  }

  public String getToName() {
    return toName;
  }

  public Dependency setToName(String toName) {
    this.toName = toName;
    return this;
  }

  public String getToQualifier() {
    return toQualifier;
  }

  public Dependency setToQualifier(String toQualifier) {
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
