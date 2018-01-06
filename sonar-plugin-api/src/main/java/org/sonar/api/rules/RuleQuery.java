/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.rules;

/**
 * @since 2.3
 */
public final class RuleQuery {

  private String repositoryKey = null;
  private String key = null;
  private String configKey = null;

  /**
   * Use the factory method <code>create()</code>
   */
  RuleQuery() {
  }

  public static RuleQuery create() {
    return new RuleQuery();
  }

  public RuleQuery withRepositoryKey(String s) {
    this.repositoryKey = s;
    return this;
  }

  public RuleQuery withKey(String s) {
    this.key = s;
    return this;
  }

  public RuleQuery withConfigKey(String s) {
    this.configKey = s;
    return this;
  }

  public String getRepositoryKey() {
    return repositoryKey;
  }

  public String getKey() {
    return key;
  }

  public String getConfigKey() {
    return configKey;
  }
}
