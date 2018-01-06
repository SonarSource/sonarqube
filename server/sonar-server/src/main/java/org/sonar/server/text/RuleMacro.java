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
package org.sonar.server.text;

class RuleMacro implements Macro {

  private static final String COLON = "%3A";
  private final String contextPath;

  RuleMacro(String contextPath) {
    this.contextPath = contextPath;
  }

  /**
   * First parameter is the repository, second one is the rule key. Exemple : {rule:squid:ArchitecturalConstraint}
   */
  @Override
  public String getRegex() {
    return "\\{rule:([a-zA-Z0-9._-]++):([a-zA-Z0-9._-]++)\\}";
  }

  @Override
  public String getReplacement() {
    return "<a href='" + contextPath + "/coding_rules#rule_key=$1" + COLON + "$2'>$2</a>";
  }
}
