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

import org.sonarqube.ws.QualityProfiles.WsSearchResponse.QualityProfile;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.rule.RuleKey;
import org.sonarqube.ws.Rules.Rule;

import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ActiveRulesProviderTest {
  private ActiveRulesProvider provider;

  @Mock
  private DefaultActiveRulesLoader loader;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    provider = new ActiveRulesProvider();
  }

  @Test
  public void testCombinationOfRules() {
    Rule r1 = mockRule("rule1");
    Rule r2 = mockRule("rule2");
    Rule r3 = mockRule("rule3");

    List<Rule> qp1Rules = ImmutableList.of(r1, r2);
    List<Rule> qp2Rules = ImmutableList.of(r2, r3);
    List<Rule> qp3Rules = ImmutableList.of(r1, r3);

    when(loader.load("qp1", null)).thenReturn(qp1Rules);
    when(loader.load("qp2", null)).thenReturn(qp2Rules);
    when(loader.load("qp3", null)).thenReturn(qp3Rules);

    ModuleQProfiles profiles = mockProfiles("qp1", "qp2", "qp3");
    ActiveRules activeRules = provider.provide(loader, profiles);

    assertThat(activeRules.findAll()).hasSize(3);
    assertThat(activeRules.findAll()).extracting("ruleKey").containsOnly(
      RuleKey.of("rule1", "rule1"), RuleKey.of("rule2", "rule2"), RuleKey.of("rule3", "rule3"));

    verify(loader).load("qp1", null);
    verify(loader).load("qp2", null);
    verify(loader).load("qp3", null);
    verifyNoMoreInteractions(loader);
  }

  private static ModuleQProfiles mockProfiles(String... keys) {
    List<QualityProfile> profiles = new LinkedList<>();

    for (String k : keys) {
      QualityProfile p = QualityProfile.newBuilder().setKey(k).setLanguage(k).build();
      profiles.add(p);
    }

    return new ModuleQProfiles(profiles);
  }

  private static Rule mockRule(String name) {
    return Rule.newBuilder().setName(name).setRepo(name).setKey(name).build();
  }
}
