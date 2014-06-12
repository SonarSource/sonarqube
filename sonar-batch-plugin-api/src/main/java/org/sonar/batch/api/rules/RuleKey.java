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
package org.sonar.batch.api.rules;

import java.io.Serializable;

/**
 * Key of a rule. Unique among all the rule repositories.
 *
 * @since 3.6
 */
public class RuleKey implements Serializable {
  private final String repository, rule;

  protected RuleKey(String repositoryKey, String ruleKey) {
    this.repository = repositoryKey;
    this.rule = ruleKey;
  }

  /**
   * Create a key. Parameters are NOT null.
   */
  public static RuleKey of(String repository, String rule) {
    return new RuleKey(repository, rule);
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
