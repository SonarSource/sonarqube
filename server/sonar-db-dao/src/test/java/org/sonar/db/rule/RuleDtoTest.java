/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.db.rule;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Set;
import org.junit.Test;

import static org.apache.commons.lang.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RuleDtoTest {


  @Test
  public void fail_if_key_is_too_long() {
    assertThatThrownBy(() -> new RuleDto().setRuleKey(repeat("x", 250)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Rule key is too long: ");
  }

  @Test
  public void fail_if_name_is_too_long() {
    assertThatThrownBy(() -> new RuleDto().setName(repeat("x", 300)))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Rule name is too long: ");
  }

  @Test
  public void fail_if_tags_are_too_long() {
    assertThatThrownBy(() -> {
      Set<String> tags = ImmutableSet.of(repeat("a", 2000), repeat("b", 1000), repeat("c", 2000));
      new RuleDto().setTags(tags);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Rule tags are too long: ");
  }

  @Test
  public void tags_are_optional() {
    RuleDto dto = new RuleDto().setTags(Collections.emptySet());
    assertThat(dto.getTags()).isEmpty();
  }
}
