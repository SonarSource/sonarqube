/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.server.text;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletContext;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MacroInterpreterTest {

  String path = "http://sonar";
  MacroInterpreter interpreter;

  @Before
  public void setUp() {
    ServletContext servletContext = mock(ServletContext.class);
    when(servletContext.getContextPath()).thenReturn(path);
    interpreter = new MacroInterpreter(servletContext);
  }

  @Test
  public void should_do_nothing_if_no_macro_detected() {
    String origin = "nothing to do";
    String result = interpreter.interpret(origin);
    assertThat(result).isEqualTo(origin);
  }

  @Test
  public void should_replace_rule_macro() {
    String ruleKey = "repo:key";
    String origin = "See {rule:" + ruleKey + "} for detail.";
    String result = interpreter.interpret(origin);
    assertThat(result).isEqualTo("See <a class='open-modal rule-modal' modal-width='800' href='" + path + "/rules/show/" + ruleKey + "?modal=true&layout=false'>" + ruleKey + "</a> for detail.");
  }

  @Test
  public void should_replace_rule_macro_containing_digit_and_dash() {
    String ruleKey = "my-repo1:my-key1";
    String origin = "See {rule:" + ruleKey + "} for detail.";
    String result = interpreter.interpret(origin);
    assertThat(result).isEqualTo("See <a class='open-modal rule-modal' modal-width='800' href='" + path + "/rules/show/" + ruleKey + "?modal=true&layout=false'>" + ruleKey + "</a> for detail.");
  }
}
