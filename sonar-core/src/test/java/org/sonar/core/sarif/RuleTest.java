/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.core.sarif;

import java.util.Set;
import org.junit.Test;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;


public class RuleTest {

  @Test
  public void equals_matchOnlyOnId() {
    Rule rule1 = createRule();
    Rule rule1Bis = createRule(rule1.getId()) ;
    Rule rule2 = withRuleId(rule1, rule1.getId() + randomAlphanumeric(3));

    assertThat(rule1).isEqualTo(rule1Bis).isNotEqualTo(rule2);
  }

  @Test
  public void equals_notMatchWithNull(){
    Rule rule1 = createRule();

    assertThat(rule1).isNotEqualTo(null);
  }

  @Test
  public void equals_matchWithSameObject(){
    Rule rule1 = createRule();

    assertThat(rule1).isEqualTo(rule1);
  }

  private static Rule withRuleId(Rule rule, String id) {
    return Rule.builder()
      .id(id)
      .name(rule.getName())
      .shortDescription(rule.getName())
      .fullDescription(rule.getName())
      .help(rule.getFullDescription().getText())
      .properties(rule.getProperties())
      .build();
  }

  private static Rule createRule() {
    return createRule(randomAlphanumeric(5));
  }

  private static Rule createRule(String id) {
    return Rule.builder()
      .id(id)
      .name(randomAlphanumeric(5))
      .shortDescription(randomAlphanumeric(5))
      .fullDescription(randomAlphanumeric(10))
      .help(randomAlphanumeric(10))
      .properties(PropertiesBag.of(randomAlphanumeric(3), Set.of(randomAlphanumeric(4))))
      .build();
  }

}
