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
package org.sonar.wsclient.services;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

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
  private Integer weight;
  private List<DependencyTree> to;

  @CheckForNull
  public String getDepId() {
    return depId;
  }

  public DependencyTree setDepId(@Nullable String depId) {
    this.depId = depId;
    return this;
  }
  @CheckForNull
  public String getResourceId() {
    return resourceId;
  }

  public DependencyTree setResourceId(@Nullable String resourceId) {
    this.resourceId = resourceId;
    return this;
  }
  @CheckForNull
  public String getResourceKey() {
    return resourceKey;
  }

  public DependencyTree setResourceKey(@Nullable String resourceKey) {
    this.resourceKey = resourceKey;
    return this;
  }
  @CheckForNull
  public String getResourceName() {
    return resourceName;
  }

  public DependencyTree setResourceName(@Nullable String resourceName) {
    this.resourceName = resourceName;
    return this;
  }
  @CheckForNull
  public String getUsage() {
    return usage;
  }

  public DependencyTree setUsage(@Nullable String usage) {
    this.usage = usage;
    return this;
  }
  @CheckForNull
  public String getResourceScope() {
    return resourceScope;
  }

  public DependencyTree setResourceScope(@Nullable String resourceScope) {
    this.resourceScope = resourceScope;
    return this;
  }
  @CheckForNull
  public String getResourceQualifier() {
    return resourceQualifier;
  }

  public DependencyTree setResourceQualifier(@Nullable String resourceQualifier) {
    this.resourceQualifier = resourceQualifier;
    return this;
  }
  @CheckForNull
  public String getResourceVersion() {
    return resourceVersion;
  }

  public DependencyTree setResourceVersion(@Nullable String resourceVersion) {
    this.resourceVersion = resourceVersion;
    return this;
  }

  @CheckForNull
  public Integer getWeight() {
    return weight;
  }

  public DependencyTree setWeight(@Nullable Integer weight) {
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
