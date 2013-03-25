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
package org.sonar.api.rules;

import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class RuleTest {

  @Test
  public void description_should_be_cleaned() {
    Rule rule = new Rule();
    rule.setDescription("    my description         ");
    Assert.assertEquals("my description", rule.getDescription());

    rule.setDescription(null);
    assertNull(rule.getDescription());
  }

  @Test
  public void should_remove_new_line_characters_in_name_with_setter() {
    Rule rule = new Rule();
    for (String example : getExamplesContainingNewLineCharacter()) {
      rule.setName(example);
      assertThat(rule.getName(), is("test"));
    }
  }

  @Test
  public void should_remove_new_line_characters_in_name_with_first_constructor() {
    Rule rule;
    for (String example : getExamplesContainingNewLineCharacter()) {
      rule = new Rule(null, null).setName(example);
      assertThat(rule.getName(), is("test"));
    }
  }

  @Test
  public void should_remove_new_line_characters_in_name_with_second_constructor() {
    Rule rule;
    for (String example : getExamplesContainingNewLineCharacter()) {
      rule = new Rule(null, null).setName(example);
      assertThat(rule.getName(), is("test"));
    }
  }

  @Test
  public void default_priority_is_major() {
    Rule rule = new Rule();
    assertThat(rule.getSeverity(), Is.is(RulePriority.MAJOR));

    rule = new Rule("name", "key");
    assertThat(rule.getSeverity(), Is.is(RulePriority.MAJOR));

    rule.setSeverity(RulePriority.BLOCKER);
    assertThat(rule.getSeverity(), Is.is(RulePriority.BLOCKER));

    rule.setSeverity(null);
    assertThat(rule.getSeverity(), Is.is(RulePriority.MAJOR));
  }

  private List<String> getExamplesContainingNewLineCharacter() {
    return Arrays.asList("te\nst", "te\ns\nt", "te\rst", "te\n\rst", "te\r\nst");
  }

}
