/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.rule2.index;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class RuleMappingTest {

  @Test
  public void all_rule_fields() throws Exception {
    assertThat(RuleNormalizer.RuleField.ALL_KEYS).contains(
      RuleNormalizer.RuleField.KEY.key(), RuleNormalizer.RuleField.REPOSITORY.key(), RuleNormalizer.RuleField.TAGS.key(),
      RuleNormalizer.RuleField.CREATED_AT.key());
  }

  @Test
  public void key_of_rule_field() throws Exception {
    assertThat(RuleNormalizer.RuleField.INTERNAL_KEY.key()).isEqualTo("internalKey");
  }
}
