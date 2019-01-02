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
package org.sonar.server.rule;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.sonar.db.rule.RuleDto;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class RuleTagHelperTest {

  @Test
  public void applyTags() {
    RuleDto rule = new RuleDto().setTags(Sets.newHashSet("performance"));
    boolean changed = RuleTagHelper.applyTags(rule, Sets.newHashSet("java8", "security"));
    assertThat(rule.getTags()).containsOnly("java8", "security");
    assertThat(changed).isTrue();
  }

  @Test
  public void applyTags_remove_all_existing_tags() {
    RuleDto rule = new RuleDto().setTags(Sets.newHashSet("performance"));
    boolean changed = RuleTagHelper.applyTags(rule, Collections.emptySet());
    assertThat(rule.getTags()).isEmpty();
    assertThat(changed).isTrue();
  }

  @Test
  public void applyTags_no_changes() {
    RuleDto rule = new RuleDto().setTags(Sets.newHashSet("performance"));
    boolean changed = RuleTagHelper.applyTags(rule, Sets.newHashSet("performance"));
    assertThat(rule.getTags()).containsOnly("performance");
    assertThat(changed).isFalse();
  }

  @Test
  public void applyTags_validate_format() {
    RuleDto rule = new RuleDto();
    boolean changed = RuleTagHelper.applyTags(rule, Sets.newHashSet("java8", "security"));
    assertThat(rule.getTags()).containsOnly("java8", "security");
    assertThat(changed).isTrue();

    try {
      RuleTagHelper.applyTags(rule, Sets.newHashSet("Java Eight"));
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).startsWith("Tag 'Java Eight' is invalid");
    }
  }

  @Test
  public void applyTags_do_not_duplicate_system_tags() {
    RuleDto rule = new RuleDto()
      .setTags(Sets.newHashSet("performance"))
      .setSystemTags(Sets.newHashSet("security"));

    boolean changed = RuleTagHelper.applyTags(rule, Sets.newHashSet("java8", "security"));

    assertThat(changed).isTrue();
    assertThat(rule.getTags()).containsOnly("java8");
    assertThat(rule.getSystemTags()).containsOnly("security");
  }
}
