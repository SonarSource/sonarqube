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
package org.sonar.api.rule;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Key of a rule. Unique among all the rule repositories.
 *
 * @since 3.6
 */
@Immutable
public class RuleKey implements Serializable, Comparable<RuleKey> {

  /**
   * @deprecated since 5.5, manual rule feature has been dropped
   */
  @Deprecated
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
    Preconditions.checkArgument(!isEmpty(repository), "Repository must be set");
    Preconditions.checkArgument(!isEmpty(rule), "Rule must be set");
    return new RuleKey(repository, rule);
  }

  /**
   * Create a key from a string representation (see {@link #toString()}. An {@link IllegalArgumentException} is raised
   * if the format is not valid.
   */
  public static RuleKey parse(String s) {
    int semiColonPos = s.indexOf(':');
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

  /**
   * @deprecated since 5.5, manual rule feature has been dropped
   */
  @Deprecated
  public boolean isManual() {
    return false;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RuleKey ruleKey = (RuleKey) o;
    return repository.equals(ruleKey.repository) && rule.equals(ruleKey.rule);
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

  @Override
  public int compareTo(RuleKey o) {
    int compareRepositories = this.repository.compareTo(o.repository);
    if (compareRepositories == 0) {
      return this.rule.compareTo(o.rule);
    }
    return compareRepositories;
  }
}
