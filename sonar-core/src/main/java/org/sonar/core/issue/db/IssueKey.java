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
package org.sonar.core.issue.db;

import com.google.common.base.Preconditions;
import org.sonar.api.rule.RuleKey;

import java.io.Serializable;

public class IssueKey implements Serializable {

  private final RuleKey ruleKey;
  private final String projectKey, componentKey;

  protected IssueKey(RuleKey ruleKey, String projectKey, String componentKey) {
    Preconditions.checkNotNull(ruleKey, "RuleKey is missing");
    Preconditions.checkNotNull(projectKey, "Project is missing");
    this.ruleKey = ruleKey;
    this.projectKey = projectKey;
    this.componentKey = componentKey;
  }


  /**
   * Create a key. Parameters are NOT null.
   */
  public static IssueKey of(String ruleKey, String ruleRepo, String rootComponentKey, String componentKey) {
    return new IssueKey(RuleKey.of(ruleRepo, ruleKey), rootComponentKey, componentKey);
  }

  /**
   * Create a key from a string representation (see {@link #toString()}. An {@link IllegalArgumentException} is raised
   * if the format is not valid.
   */
  public static IssueKey parse(String s) {
    Preconditions.checkArgument(s.split(":").length >= 4, "Bad format of issueKey key: " + s);
//    int semiColonPos = s.indexOf(":");
//    String key = s.substring(0, semiColonPos);
//    String ruleKey = s.substring(semiColonPos + 1);
//    return IssueKey.of(key, RuleKey.parse(ruleKey));
    return null;
  }

  /**
   * Never null
   */
  public RuleKey ruleKey() {
    return ruleKey;
  }

  /**
   * Never null
   */
  public String projectKey() {
    return projectKey;
  }

  /**
   * Never null
   */
  public String componentKey() {
    return componentKey;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IssueKey key = (IssueKey) o;
    if (!ruleKey.equals(key.ruleKey)) {
      return false;
    }
    if (!projectKey.equals(key.projectKey)) {
      return false;
    }
    if (!componentKey.equals(key.componentKey)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = ruleKey.hashCode();
    result = 31 * result + projectKey.hashCode();
    result = 31 * result + componentKey.hashCode();
    return result;
  }


  /**
   * Format is "qprofile:rule:project:component", for example "12345:squid:AvoidCycle"
   */
  @Override
  public String toString() {
    return String.format("%s:%s:%s", ruleKey.toString(), projectKey(), componentKey());
  }

}
