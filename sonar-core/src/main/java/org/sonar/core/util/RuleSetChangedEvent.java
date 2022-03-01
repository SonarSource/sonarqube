/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.core.util;

import java.io.Serializable;

public class RuleSetChangedEvent implements Serializable {

  private static final String EVENT = "RuleSetChanged";

  private final String[] projects;
  private final String language;
  private final RuleChange[] activatedRules;
  private final String[] deactivatedRules;

  public RuleSetChangedEvent(String[] projects, RuleChange[] activatedRules, String[] deactivatedRules, String language) {
    this.projects = projects;
    this.activatedRules = activatedRules;
    this.deactivatedRules = deactivatedRules;
    if (activatedRules.length == 0 && deactivatedRules.length == 0) {
      throw new IllegalArgumentException("Can't create RuleSetChangedEvent without any rules that have changed");
    }
    this.language = language;
  }

  public String getEvent() {
    return EVENT;
  }

  public String[] getProjects() {
    return projects;
  }

  public String getLanguage() {
    return language;
  }

  public RuleChange[] getActivatedRules() {
    return activatedRules;
  }

  public String[] getDeactivatedRules() {
    return deactivatedRules;
  }
}
