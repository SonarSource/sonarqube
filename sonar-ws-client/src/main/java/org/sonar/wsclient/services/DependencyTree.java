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

import java.util.List;

/**
 * Experimental
 */
public class DependencyTree extends Model {
  private String depId;
  private String resourceId;
  private String resourceKey;
  private String resourceName;
  private String usage;
  private String resourceScope;
  private String resourceQualifier;
  private String resourceVersion;
  private int weight;
  private List<DependencyTree> to;

  public String getDepId() {
    return depId;
  }

  public DependencyTree setDepId(String depId) {
    this.depId = depId;
    return this;
  }

  public String getResourceId() {
    return resourceId;
  }

  public DependencyTree setResourceId(String resourceId) {
    this.resourceId = resourceId;
    return this;
  }

  public String getResourceKey() {
    return resourceKey;
  }

  public DependencyTree setResourceKey(String resourceKey) {
    this.resourceKey = resourceKey;
    return this;
  }

  public String getResourceName() {
    return resourceName;
  }

  public DependencyTree setResourceName(String resourceName) {
    this.resourceName = resourceName;
    return this;
  }

  public String getUsage() {
    return usage;
  }

  public DependencyTree setUsage(String usage) {
    this.usage = usage;
    return this;
  }

  public String getResourceScope() {
    return resourceScope;
  }

  public DependencyTree setResourceScope(String resourceScope) {
    this.resourceScope = resourceScope;
    return this;
  }

  public String getResourceQualifier() {
    return resourceQualifier;
  }

  public DependencyTree setResourceQualifier(String resourceQualifier) {
    this.resourceQualifier = resourceQualifier;
    return this;
  }

  public String getResourceVersion() {
    return resourceVersion;
  }

  public DependencyTree setResourceVersion(String resourceVersion) {
    this.resourceVersion = resourceVersion;
    return this;
  }

  public int getWeight() {
    return weight;
  }

  public DependencyTree setWeight(int weight) {
    this.weight = weight;
    return this;
  }

  public List<DependencyTree> getTo() {
    return to;
  }

  public DependencyTree setTo(List<DependencyTree> to) {
    this.to = to;
    return this;
  }
}
