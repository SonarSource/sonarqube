/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.rule;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.io.Serializable;

/**
 * Key of a rule. Unique among all the rule repositories.
 * @since 3.6
 */
public class RuleKey implements Serializable {
  private final String repository, rule;

  private RuleKey(String repository, String rule) {
    this.repository = repository;
    this.rule = rule;
  }

  /**
   * Create a key. Parameters are NOT null.
   */
  public static RuleKey of(String repository, String rule) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(repository), "Repository must be set");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(rule), "Rule must be set");
    return new RuleKey(repository, rule);
  }

  /**
   * Create a key from a string representation (see {@link #toString()}. An {@link IllegalArgumentException} is raised
   * if the format is not valid.
   */
  public static RuleKey parse(String s) {
    String[] split = s.split(":");
    Preconditions.checkArgument(split.length == 2, "Bad format of rule key: " + s);
    return RuleKey.of(split[0], split[1]);
  }

  /**
   * Never null
   */
  public String repository() {
    return repository;
  }

  /**
   * Never null
   */
  public String rule() {
    return rule;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RuleKey ruleKey = (RuleKey) o;
    if (!repository.equals(ruleKey.repository)) {
      return false;
    }
    if (!rule.equals(ruleKey.rule)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = repository.hashCode();
    result = 31 * result + rule.hashCode();
    return result;
  }

  /**
   * Format is "repository:rule", for example "squid:AvoidCycle"
   */
  @Override
  public String toString() {
    return String.format("%s:%s", repository, rule);
  }
}
