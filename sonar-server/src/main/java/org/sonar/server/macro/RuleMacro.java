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

package org.sonar.server.macro;

public class RuleMacro implements Macro{

  private final String contextPath;

  public RuleMacro(String contextPath){
    this.contextPath = contextPath;
  }

  /**
   * First parameter is the repository, second one is the rule key
   */
  public String getRegex() {
    return "\\{rule:([a-zA-Z0-9._]++):([a-zA-Z0-9._]++)\\}";
  }

  public String getReplacement(){
    return "<a class='open-modal rule-modal' href='" + contextPath + "/rules/show/$1:$2?modal=true&layout=false'>$1:$2</a>";
  }
}
