/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.rules;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RuleTest {

  @Test
  public void description_should_be_cleaned() {
    Rule rule = Rule.create().setDescription("    my description         ");
    Assert.assertEquals("my description", rule.getDescription());

    rule.setDescription(null);
    assertThat(rule.getDescription()).isNull();
  }

  @Test
  public void should_remove_new_line_characters_in_name_with_setter() {
    Rule rule = Rule.create();
    for (String example : getExamplesContainingNewLineCharacter()) {
      rule.setName(example);
      assertThat(rule.getName()).isEqualTo("test");
    }
  }

  @Test
  public void should_remove_new_line_characters_in_name_with_first_constructor() {
    Rule rule;
    for (String example : getExamplesContainingNewLineCharacter()) {
      rule = new Rule(null, null).setName(example);
      assertThat(rule.getName()).isEqualTo("test");
    }
  }

  @Test
  public void should_remove_new_line_characters_in_name_with_second_constructor() {
    Rule rule;
    for (String example : getExamplesContainingNewLineCharacter()) {
      rule = new Rule(null, null).setName(example);
      assertThat(rule.getName()).isEqualTo("test");
    }
  }

  @Test
  public void default_priority_is_major() {
    Rule rule = Rule.create();
    assertThat(rule.getSeverity()).isEqualTo(RulePriority.MAJOR);

    rule = new Rule("name", "key");
    assertThat(rule.getSeverity()).isEqualTo(RulePriority.MAJOR);

    rule.setSeverity(RulePriority.BLOCKER);
    assertThat(rule.getSeverity()).isEqualTo(RulePriority.BLOCKER);

    rule.setSeverity(null);
    assertThat(rule.getSeverity()).isEqualTo(RulePriority.MAJOR);
  }

  @Test(expected = IllegalStateException.class)
  public void should_not_authorize_unkown_status() {
    Rule.create().setStatus("Unknown");
  }

  @Test
  public void should_set_valid_status() {
    Rule rule = Rule.create().setStatus(Rule.STATUS_DEPRECATED);
    assertThat(rule.getStatus()).isEqualTo(Rule.STATUS_DEPRECATED);

    rule = Rule.create().setStatus(Rule.STATUS_REMOVED);
    assertThat(rule.getStatus()).isEqualTo(Rule.STATUS_REMOVED);

    rule = Rule.create().setStatus(Rule.STATUS_BETA);
    assertThat(rule.getStatus()).isEqualTo(Rule.STATUS_BETA);

    rule = Rule.create().setStatus(Rule.STATUS_READY);
    assertThat(rule.getStatus()).isEqualTo(Rule.STATUS_READY);
  }

  @Test
  public void testTags() {
    Rule rule = Rule.create();
    assertThat(rule.getTags()).isEmpty();
    assertThat(rule.getSystemTags()).isEmpty();

    rule.setTags(new String[] {"tag1", "tag2"});
    assertThat(rule.getTags()).containsOnly("tag1", "tag2");
    assertThat(rule.getSystemTags()).isEmpty();
  }

  private List<String> getExamplesContainingNewLineCharacter() {
    return Arrays.asList("te\nst", "te\ns\nt", "te\rst", "te\n\rst", "te\r\nst");
  }
}
