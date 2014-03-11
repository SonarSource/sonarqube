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
 * @since 2.5
 */
public class Rule extends Model {

  private String title = null;
  private String key = null;
  private String configKey = null;
  private String repository = null;
  private String description = null;
  private String severity = null;
  private List<RuleParam> params;
  private boolean active;

  @CheckForNull
  public String getTitle() {
    return title;
  }

  public Rule setTitle(@Nullable String title) {
    this.title = title;
    return this;
  }

  @CheckForNull
  public String getKey() {
    return key;
  }

  public Rule setKey(@Nullable String key) {
    this.key = key;
    return this;
  }

  /**
   * @since 2.7
   */
  @CheckForNull
  public String getConfigKey() {
    return configKey;
  }

  /**
   * @since 2.7
   */

  public Rule setConfigKey(@Nullable String s) {
    this.configKey = s;
    return this;
  }

  @CheckForNull
  public String getRepository() {
    return repository;
  }

  public Rule setRepository(@Nullable String s) {
    this.repository = s;
    return this;
  }

  @CheckForNull
  public String getDescription() {
    return description;
  }

  public Rule setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  @CheckForNull
  public String getSeverity() {
    return severity;
  }

  public Rule setSeverity(@Nullable String severity) {
    this.severity = severity;
    return this;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public boolean isActive() {
    return active;
  }

  public List<RuleParam> getParams() {
    return params;
  }

  public void setParams(List<RuleParam> params) {
    this.params = params;
  }

}
