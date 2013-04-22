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

  public String getTitle() {
    return title;
  }

  public Rule setTitle(String title) {
    this.title = title;
    return this;
  }

  public String getKey() {
    return key;
  }

  public Rule setKey(String key) {
    this.key = key;
    return this;
  }

  /**
   * @since 2.7
   */
  public String getConfigKey() {
    return configKey;
  }

  /**
   * @since 2.7
   */

  public Rule setConfigKey(String s) {
    this.configKey = s;
    return this;
  }

  public String getRepository() {
    return repository;
  }

  public Rule setRepository(String s) {
    this.repository = s;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public Rule setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getSeverity() {
    return severity;
  }

  public Rule setSeverity(String severity) {
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
