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
package org.sonar.db.qualityprofile;

import javax.annotation.CheckForNull;

public class IndexedActiveRuleDto {
  private long id;
  private int ruleId;
  private int severity;
  private String inheritance;
  private String repository;
  private String key;
  private String ruleProfileUuid;

  public long getId() {
    return id;
  }

  public int getRuleId() {
    return ruleId;
  }

  public int getSeverity() {
    return severity;
  }

  @CheckForNull
  public String getInheritance() {
    return inheritance;
  }

  public String getRepository() {
    return repository;
  }

  public String getKey() {
    return key;
  }

  public String getRuleProfileUuid() {
    return ruleProfileUuid;
  }
}
