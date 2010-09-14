/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.api.checks.profiles;

import org.sonar.api.BatchExtension;
import org.sonar.api.ServerExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @since 2.1 (experimental)
 * @deprecated since 2.3
 */
@Deprecated
public class CheckProfile implements BatchExtension, ServerExtension {

  private String name;
  private String language;
  private List<Check> checks = new ArrayList<Check>();

  public CheckProfile(String name, String language) {
    if (name == null) {
      throw new IllegalArgumentException("Name can not be null");
    }
    if (language == null) {
      throw new IllegalArgumentException("Language can not be null");
    }
    this.name = name;
    this.language = language;
  }

  public String getName() {
    return name;
  }

  public String getLanguage() {
    return language;
  }

  public List<Check> getChecks() {
    return checks;
  }

  public List<Check> getChecks(String repositoryKey) {
    List<Check> result = new ArrayList<Check>();
    for (Check check : getChecks()) {
      if (check.getRepositoryKey().equals(repositoryKey)) {
        result.add(check);
      }
    }
    return result;
  }

  public List<Check> getChecks(String repositoryKey, String templateKey) {
    List<Check> result = new ArrayList<Check>();
    List<Check> repoChecks = getChecks(repositoryKey);
    for (Check repoCheck : repoChecks) {
      if (repoCheck.getTemplateKey().equals(templateKey)) {
        result.add(repoCheck);
      }
    }
    return result;
  }

  /**
   * We assume there is only one check for this template
   */
  public Check getCheck(String repositoryKey, String templateKey) {
    List<Check> repoChecks = getChecks(repositoryKey);
    for (Check repoCheck : repoChecks) {
      if (repoCheck.getTemplateKey().equals(templateKey)) {
        return repoCheck;
      }
    }
    return null;
  }

  public void addCheck(Check check) {
    checks.add(check);
  }

  public void setChecks(Collection<Check> list) {
    checks.clear();
    checks.addAll(list);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CheckProfile profile = (CheckProfile) o;
    if (!language.equals(profile.language)) {
      return false;
    }
    if (!name.equals(profile.name)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + language.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return name;
  }
}
