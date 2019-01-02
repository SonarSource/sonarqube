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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rules.RuleFinder;
import org.sonar.db.DbClient;
import org.sonar.db.rule.RuleDao;
import org.sonar.server.organization.TestDefaultOrganizationProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebServerRuleFinderImplTest {

  private DbClient dbClient = mock(DbClient.class);
  private TestDefaultOrganizationProvider defaultOrganizationProvider = TestDefaultOrganizationProvider.fromUuid("1111");
  private WebServerRuleFinderImpl underTest = new WebServerRuleFinderImpl(dbClient, defaultOrganizationProvider);

  @Before
  public void setUp() throws Exception {
    when(dbClient.ruleDao()).thenReturn(mock(RuleDao.class));
  }

  @Test
  public void constructor_initializes_with_non_caching_delegate() {
    assertThat(underTest.delegate).isInstanceOf(DefaultRuleFinder.class);
  }

  @Test
  public void startCaching_sets_caching_delegate() {
    underTest.startCaching();

    assertThat(underTest.delegate).isInstanceOf(CachingRuleFinder.class);
  }

  @Test
  public void stopCaching_restores_non_caching_delegate() {
    RuleFinder nonCachingDelegate = underTest.delegate;

    underTest.startCaching();
    underTest.stopCaching();

    assertThat(underTest.delegate).isSameAs(nonCachingDelegate);
  }
}
