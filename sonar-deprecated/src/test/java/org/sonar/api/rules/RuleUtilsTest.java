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
package org.sonar.api.rules;

import org.apache.commons.configuration.Configuration;
import org.junit.Test;
import org.mockito.Matchers;
import org.sonar.api.CoreProperties;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RuleUtilsTest {

  @Test
  public void getPriorityWeights() {
    Configuration conf = mock(Configuration.class);
    when(conf.getString(Matchers.eq(CoreProperties.CORE_RULE_WEIGHTS_PROPERTY), anyString())).thenReturn("info=0;minor=1;major=2;critical=5;blocker=10");

    final Map<RulePriority, Integer> map = RuleUtils.getPriorityWeights(conf);

    assertThat(map.get(RulePriority.BLOCKER), is(10));
    assertThat(map.get(RulePriority.CRITICAL), is(5));
    assertThat(map.get(RulePriority.MAJOR), is(2));
    assertThat(map.get(RulePriority.MINOR), is(1));
    assertThat(map.get(RulePriority.INFO), is(0));
  }

  @Test
  public void loadMissingWeights() {
    Configuration conf = mock(Configuration.class);
    when(conf.getString(Matchers.eq(CoreProperties.CORE_RULE_WEIGHTS_PROPERTY), anyString())).thenReturn("foo=0;bar=1;CRITICAL=5");

    final Map<RulePriority, Integer> map = RuleUtils.getPriorityWeights(conf);

    assertThat(map.get(RulePriority.BLOCKER), is(1));
    assertThat(map.get(RulePriority.CRITICAL), is(5));
    assertThat(map.get(RulePriority.MAJOR), is(1));
    assertThat(map.get(RulePriority.MINOR), is(1));
    assertThat(map.get(RulePriority.INFO), is(1));
  }

}
