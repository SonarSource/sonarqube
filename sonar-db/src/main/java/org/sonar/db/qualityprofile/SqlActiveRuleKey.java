/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonar.db.qualityprofile;

public class SqlActiveRuleKey implements Comparable<SqlActiveRuleKey> {
  private final String qProfile;
  private final String rule;
  private final String repository;

  public SqlActiveRuleKey(ActiveRuleKey key) {
    this.qProfile = key.qProfile();
    this.rule = key.ruleKey().rule();
    this.repository = key.ruleKey().repository();
  }

  @Override
  public int compareTo(SqlActiveRuleKey o) {
    int result = qProfile.compareTo(o.qProfile);
    if (result != 0) {
      return result;
    }
    result = rule.compareTo(o.rule);
    if (result != 0) {
      return result;
    }

    return repository.compareTo(o.repository);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SqlActiveRuleKey activeRuleKey = (SqlActiveRuleKey) o;
    return qProfile.equals(activeRuleKey.qProfile)
      && rule.equals(activeRuleKey.rule)
      && repository.equals(activeRuleKey.repository);
  }

  @Override
  public int hashCode() {
    int result = qProfile.hashCode();
    result = 31 * result + rule.hashCode();
    result = 31 * result + repository.hashCode();
    return result;
  }

  public String getqProfile() {
    return qProfile;
  }

  public String getRule() {
    return rule;
  }

  public String getRepository() {
    return repository;
  }
}
