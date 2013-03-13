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

import org.sonar.api.ServerComponent;

import javax.servlet.ServletContext;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class MacroInterpreter implements ServerComponent {

  private ServletContext servletContext;
  private List<Macro> macroList;

  public MacroInterpreter(ServletContext servletContext) {
    this.servletContext = servletContext;
    this.macroList = newArrayList();
  }

  public void start(){
    macroList.add(new RuleMacro(servletContext.getContextPath()));
  }

  public String interpret(String text){
    String textReplaced = text;
    for (Macro macro : macroList) {
      textReplaced = textReplaced.replaceAll(macro.getRegex(), macro.getReplacement());
    }
    return textReplaced;
  }

}
