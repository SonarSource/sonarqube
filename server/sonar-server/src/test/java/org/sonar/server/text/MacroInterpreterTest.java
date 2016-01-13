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
package org.sonar.server.text;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.platform.Server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MacroInterpreterTest {

  String path = "http://sonar";
  MacroInterpreter interpreter;

  @Before
  public void setUp() {
    Server server = mock(Server.class);
    when(server.getContextPath()).thenReturn(path);
    interpreter = new MacroInterpreter(server);
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
    assertThat(result).isEqualTo("See <a href='" + path + "/coding_rules#rule_key=" + ruleKey + "'>key</a> for detail.");
  }

  @Test
  public void should_replace_rule_macro_containing_digit_and_dash() {
    String ruleKey = "my-repo1:my-key1";
    String origin = "See {rule:" + ruleKey + "} for detail.";
    String result = interpreter.interpret(origin);
    assertThat(result).isEqualTo("See <a href='" + path + "/coding_rules#rule_key=" + ruleKey + "'>my-key1</a> for detail.");
  }
}
