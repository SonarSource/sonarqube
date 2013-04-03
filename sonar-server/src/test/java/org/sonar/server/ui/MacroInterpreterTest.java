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

package org.sonar.server.ui;

import org.junit.Before;
import org.junit.Test;
import org.sonar.server.macro.MacroInterpreter;

import javax.servlet.ServletContext;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MacroInterpreterTest {

  private MacroInterpreter interpreter;
  private String path;

  @Before
  public void before() {
    path = "http://sonar";
    ServletContext servletContext = mock(ServletContext.class);
    when(servletContext.getContextPath()).thenReturn(path);
    interpreter = new MacroInterpreter(servletContext);
  }

  @Test
  public void should_do_nothing_if_no_macro_detected() {
    String origin = "nothing to do";
    interpreter.start();
    String result = interpreter.interpret(origin);
    assertThat(result).isEqualTo(origin);
  }

  @Test
  public void should_do_nothing_if_not_started() {
    String ruleKey = "repo:key";
    String origin = "{rule:"+ ruleKey + "}";

    String result = interpreter.interpret(origin);
    assertThat(result).isEqualTo(origin);
  }

  @Test
  public void should_replace_rule_macro() {
    String ruleKey = "repo:key";
    String origin = "See {rule:"+ ruleKey + "} for detail.";
    interpreter.start();
    String result = interpreter.interpret(origin);
    assertThat(result).isEqualTo("See <a class='open-modal rule-modal' modal-width='800' href='"+ path + "/rules/show/"+ ruleKey + "?modal=true&layout=false'>" + ruleKey +"</a> for detail.");
  }

  @Test
  public void should_replace_rule_macro_containing_digit_and_dash() {
    String ruleKey = "my-repo1:my-key1";
    String origin = "See {rule:"+ ruleKey + "} for detail.";
    interpreter.start();
    String result = interpreter.interpret(origin);
    assertThat(result).isEqualTo("See <a class='open-modal rule-modal' modal-width='800' href='"+ path + "/rules/show/"+ ruleKey + "?modal=true&layout=false'>" + ruleKey +"</a> for detail.");
  }
}
