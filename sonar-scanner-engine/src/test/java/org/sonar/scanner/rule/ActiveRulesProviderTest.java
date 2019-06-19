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
package org.sonar.scanner.rule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.LoadedActiveRule;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.DateUtils;
import org.sonarqube.ws.Qualityprofiles.SearchWsResponse.QualityProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
    LoadedActiveRule r1 = mockRule("rule1");
    LoadedActiveRule r2 = mockRule("rule2");
    LoadedActiveRule r3 = mockRule("rule3");

    List<LoadedActiveRule> qp1Rules = ImmutableList.of(r1, r2);
    List<LoadedActiveRule> qp2Rules = ImmutableList.of(r2, r3);
    List<LoadedActiveRule> qp3Rules = ImmutableList.of(r1, r3);

    when(loader.load(eq("qp1"))).thenReturn(qp1Rules);
    when(loader.load(eq("qp2"))).thenReturn(qp2Rules);
    when(loader.load(eq("qp3"))).thenReturn(qp3Rules);

    QualityProfiles profiles = mockProfiles("qp1", "qp2", "qp3");
    ActiveRules activeRules = provider.provide(loader, profiles);

    assertThat(activeRules.findAll()).hasSize(3);
    assertThat(activeRules.findAll()).extracting("ruleKey").containsOnly(
      RuleKey.of("rule1", "rule1"), RuleKey.of("rule2", "rule2"), RuleKey.of("rule3", "rule3"));

    verify(loader).load(eq("qp1"));
    verify(loader).load(eq("qp2"));
    verify(loader).load(eq("qp3"));
    verifyNoMoreInteractions(loader);
  }

  @Test
  public void testParamsAreTransformed() {
    LoadedActiveRule r1 = mockRule("rule1");
    LoadedActiveRule r2 = mockRule("rule2");
    r2.setParams(ImmutableMap.of("foo1", "bar1", "foo2", "bar2"));

    List<LoadedActiveRule> qpRules = ImmutableList.of(r1, r2);
    when(loader.load(eq("qp"))).thenReturn(qpRules);

    QualityProfiles profiles = mockProfiles("qp");
    ActiveRules activeRules = provider.provide(loader, profiles);

    assertThat(activeRules.findAll()).hasSize(2);
    assertThat(activeRules.findAll()).extracting("ruleKey", "params").containsOnly(
      Tuple.tuple(RuleKey.of("rule1", "rule1"), ImmutableMap.of()),
      Tuple.tuple(RuleKey.of("rule2", "rule2"), ImmutableMap.of("foo1", "bar1", "foo2", "bar2")));

    verify(loader).load(eq("qp"));
    verifyNoMoreInteractions(loader);
  }

  private static QualityProfiles mockProfiles(String... keys) {
    List<QualityProfile> profiles = new LinkedList<>();

    for (String k : keys) {
      QualityProfile p = QualityProfile.newBuilder().setKey(k).setLanguage(k).setRulesUpdatedAt(DateUtils.formatDateTime(new Date())).build();
      profiles.add(p);
    }

    return new QualityProfiles(profiles);
  }

  private static LoadedActiveRule mockRule(String name) {
    LoadedActiveRule r = new LoadedActiveRule();
    r.setName(name);
    r.setRuleKey(RuleKey.of(name, name));
    return r;
  }
}
