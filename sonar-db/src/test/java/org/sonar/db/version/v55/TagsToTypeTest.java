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
package org.sonar.db.version.v55;

import java.util.Collections;
import org.junit.Test;
import org.sonar.api.rules.RuleType;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.version.v55.TagsToType.tagsToType;

public class TagsToTypeTest {

  @Test
  public void test_tagsToType() {
    assertThat(tagsToType(asList("misra", "bug"))).isEqualTo(RuleType.BUG);
    assertThat(tagsToType(asList("misra", "security"))).isEqualTo(RuleType.VULNERABILITY);

    // "bug" has priority on "security"
    assertThat(tagsToType(asList("security", "bug"))).isEqualTo(RuleType.BUG);

    // default is "code smell"
    assertThat(tagsToType(asList("clumsy", "spring"))).isEqualTo(RuleType.CODE_SMELL);
    assertThat(tagsToType(Collections.<String>emptyList())).isEqualTo(RuleType.CODE_SMELL);
  }

}
