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
package org.sonar.server.computation.issue;

import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.rule.RuleDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RuleCacheTest {

  @Test
  public void ruleName() throws Exception {
    RuleCacheLoader loader = mock(RuleCacheLoader.class);
    when(loader.load(RuleKey.of("squid", "R002"))).thenReturn(new RuleDto().setName("Rule Two"));
    RuleCache cache = new RuleCache(loader);
    assertThat(cache.ruleName(RuleKey.of("squid", "R001"))).isNull();
    assertThat(cache.ruleName(RuleKey.of("squid", "R002"))).isEqualTo("Rule Two");
  }
}
