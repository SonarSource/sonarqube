/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class RuleTest {

  @Test
  public void descriptionShouldBeCleaned() {
    Rule rule = new Rule();
    rule.setDescription("    my description         ");
    Assert.assertEquals("my description", rule.getDescription());

    rule.setDescription(null);
    assertNull(rule.getDescription());
  }

  @Test
  public void shouldRemoveNewLineCharactersInNameWithSetter() {
    Rule rule = new Rule();
    for (String example : getExamplesContainingNewLineCharacter()) {
      rule.setName(example);
      assertThat(rule.getName(), is("test"));
    }
  }

  @Test
  public void shouldRemoveNewLineCharactersInNameWithfirstConstructor() {
    Rule rule;
    for (String example : getExamplesContainingNewLineCharacter()) {
      rule = new Rule(null, null, example, (RulesCategory) null, null);
      assertThat(rule.getName(), is("test"));
    }
  }

  @Test
  public void shouldRemoveNewLineCharactersInNameWithSecondConstructor() {
    Rule rule;
    for (String example : getExamplesContainingNewLineCharacter()) {
      rule = new Rule(null, null, example, (RulesCategory)null, null);
      assertThat(rule.getName(), is("test"));
    }
  }

  @Test
  public void defaultPriorityIsMajor() {
    Rule rule = new Rule();
    assertThat(rule.getPriority(), Is.is(RulePriority.MAJOR));

    rule = new Rule("name", "key");
    assertThat(rule.getPriority(), Is.is(RulePriority.MAJOR));

    rule = new Rule("pkey", "key", "name", Iso9126RulesCategories.EFFICIENCY, null, null);
    assertThat(rule.getPriority(), Is.is(RulePriority.MAJOR));

    rule.setPriority(RulePriority.BLOCKER);
    assertThat(rule.getPriority(), Is.is(RulePriority.BLOCKER));

    rule.setPriority(null);
    assertThat(rule.getPriority(), Is.is(RulePriority.MAJOR));
  }


  private List<String> getExamplesContainingNewLineCharacter() {
    return Arrays.asList("te\nst", "te\ns\nt", "te\rst", "te\n\rst", "te\r\nst");
  }


}
