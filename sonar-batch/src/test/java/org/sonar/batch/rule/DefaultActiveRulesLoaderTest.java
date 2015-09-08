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
package org.sonar.batch.rule;

import org.sonar.batch.repository.DefaultProjectRepositoriesFactory;

import com.google.common.collect.ImmutableList;
import org.sonar.batch.protocol.input.ActiveRule;
import org.junit.Test;
import org.sonar.batch.protocol.input.ProjectRepositories;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.junit.Before;

public class DefaultActiveRulesLoaderTest {
  private DefaultActiveRulesLoader loader;
  private DefaultProjectRepositoriesFactory factory;
  private ProjectRepositories projectRepositories;

  private ActiveRule response;

  @Before
  public void setUp() {
    response = mock(ActiveRule.class);
    when(response.ruleKey()).thenReturn("rule");

    projectRepositories = new ProjectRepositories();
    projectRepositories.addActiveRule(response);

    factory = mock(DefaultProjectRepositoriesFactory.class);
    when(factory.create()).thenReturn(projectRepositories);
    loader = new DefaultActiveRulesLoader(factory);
  }

  @Test
  public void test() {
    Collection<String> profiles = ImmutableList.of("profile1");
    Collection<ActiveRule> activeRules = loader.load(profiles, "project");

    assertThat(activeRules).hasSize(1);
    assertThat(activeRules.iterator().next().ruleKey()).isEqualTo("rule");

    verify(factory).create();
    verifyNoMoreInteractions(factory);
  }

}
