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
package org.sonar.api.rule;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.io.Serializable;
import javax.annotation.concurrent.Immutable;

/**
 * Key of a rule. Unique among all the rule repositories.
 *
 * @since 3.6
 */
@Immutable
public class RuleKey implements Serializable {

  public static final String MANUAL_REPOSITORY_KEY = "manual";
  private final String repository;
  private final String rule;

  protected RuleKey(String repositoryKey, String ruleKey) {
    this.repository = repositoryKey;
    this.rule = ruleKey;
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
    int semiColonPos = s.indexOf(":");
    Preconditions.checkArgument(semiColonPos > 0, "Invalid rule key: " + s);
    String key = s.substring(0, semiColonPos);
    String repo = s.substring(semiColonPos + 1);
    return RuleKey.of(key, repo);
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

  public boolean isManual() {
    return MANUAL_REPOSITORY_KEY.equals(repository);
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
    return rule.equals(ruleKey.rule);
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
