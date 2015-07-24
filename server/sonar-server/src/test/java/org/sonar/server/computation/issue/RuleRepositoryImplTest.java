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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.sonar.db.rule.RuleTesting.XOO_X1;
import static org.sonar.db.rule.RuleTesting.XOO_X2;

public class RuleRepositoryImplTest {

  RuleCacheLoader cacheLoader = mock(RuleCacheLoader.class);
  RuleRepositoryImpl underTest = new RuleRepositoryImpl(cacheLoader);

  @Test
  public void getByKey() {
    when(cacheLoader.load(XOO_X1)).thenReturn(new DumbRule(XOO_X1));

    assertThat(underTest.getByKey(XOO_X1).getKey()).isEqualTo(XOO_X1);

    // second call -> get from cache
    assertThat(underTest.getByKey(XOO_X1).getKey()).isEqualTo(XOO_X1);
    verify(cacheLoader, times(1)).load(XOO_X1);
  }

  @Test
  public void hasKey() {
    when(cacheLoader.load(XOO_X1)).thenReturn(new DumbRule(XOO_X1));

    assertThat(underTest.hasKey(XOO_X1)).isTrue();
    assertThat(underTest.hasKey(XOO_X2)).isFalse();
  }
}
