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
package org.sonar.server.rule2;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.sonar.core.rule.RuleDto;

import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class RuleTagHelperTest {

  @Test
  public void applyTags() throws Exception {
    RuleDto rule = new RuleDto().setTags(Sets.newHashSet("performance"));
    RuleTagHelper.applyTags(rule, Sets.newHashSet("java8", "security"));
    assertThat(rule.getTags()).containsOnly("java8", "security");
  }

  @Test
  public void applyTags_remove_all_existing_tags() throws Exception {
    RuleDto rule = new RuleDto().setTags(Sets.newHashSet("performance"));
    RuleTagHelper.applyTags(rule, Collections.<String>emptySet());
    assertThat(rule.getTags()).isEmpty();
  }

  @Test
  public void applyTags_validate_format() throws Exception {
    RuleDto rule = new RuleDto();
    RuleTagHelper.applyTags(rule, Sets.newHashSet("java8", "security"));
    assertThat(rule.getTags()).containsOnly("java8", "security");

    try {
      RuleTagHelper.applyTags(rule, Sets.newHashSet("Java Eight"));
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).startsWith("Tag 'Java Eight' is invalid");
    }
  }

  @Test
  public void applyTags_do_not_duplicate_system_tags() throws Exception {
    RuleDto rule = new RuleDto()
      .setTags(Sets.newHashSet("performance"))
      .setSystemTags(Sets.newHashSet("security"));

    RuleTagHelper.applyTags(rule, Sets.newHashSet("java8", "security"));

    assertThat(rule.getTags()).containsOnly("java8");
    assertThat(rule.getSystemTags()).containsOnly("security");
  }
}
