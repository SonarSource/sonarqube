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
package org.sonar.server.rule;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.test.TestUtils;

import static com.google.common.collect.FluentIterable.from;
import static org.assertj.core.api.Assertions.assertThat;

public class RuleKeyFunctionsTest {

  @Test
  public void stringToRuleKey() {
    Collection<String> strings = Arrays.asList("js:S001", "java:S002");
    List<RuleKey> keys = from(strings).transform(RuleKeyFunctions.stringToRuleKey()).toList();

    assertThat(keys).containsExactly(RuleKey.of("js", "S001"), RuleKey.of("java", "S002"));
  }

  @Test
  public void on_static_methods() {
    assertThat(TestUtils.hasOnlyPrivateConstructors(RuleKeyFunctions.class)).isTrue();
  }
}
