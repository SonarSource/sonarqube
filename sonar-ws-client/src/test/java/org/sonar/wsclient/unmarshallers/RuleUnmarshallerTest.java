/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.wsclient.unmarshallers;

import org.junit.Test;
import org.sonar.wsclient.services.Rule;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class RuleUnmarshallerTest extends UnmarshallerTestCase {

  @Test
  public void toModels() {
    Rule rule = new RuleUnmarshaller().toModel("[]");
    assertThat(rule, nullValue());

    List<Rule> rules = new RuleUnmarshaller().toModels("[]");
    assertThat(rules.size(), is(0));

    rules = new RuleUnmarshaller().toModels(loadFile("/rules/rules.json"));
    assertThat(rules.size(), is(6));

    rule = rules.get(0);
    assertThat(rule.getTitle(), is("Indentation"));
    assertThat(rule.getKey(), is("checkstyle:com.puppycrawl.tools.checkstyle.checks.indentation.IndentationCheck"));
    assertThat(rule.getRepository(), is("checkstyle"));
    assertThat(rule.getDescription(), is("Checks correct indentation of Java Code."));
    assertThat(rule.getSeverity(), is("MINOR"));
    assertThat(rule.isActive(), is(false));
    assertThat(rule.getParams().size(), is(3));
    assertThat(rule.getParams().get(0).getName(), is("basicOffset"));
    assertThat(rule.getParams().get(0).getDescription(), is("how many spaces to use for new indentation level. Default is 4."));

    rule = rules.get(1);
    assertThat(rule.isActive(), is(true));
  }

}
