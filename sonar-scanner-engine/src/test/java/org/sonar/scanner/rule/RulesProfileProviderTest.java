/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.Arrays;
import org.junit.Test;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.RuleKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RulesProfileProviderTest {

  private ModuleQProfiles qProfiles = mock(ModuleQProfiles.class);
  private MapSettings settings = new MapSettings();
  private RulesProfileProvider provider = new RulesProfileProvider();

  @Test
  public void merge_profiles() {
    QProfile qProfile = new QProfile("java-sw", "Sonar way", "java", null);
    when(qProfiles.findAll()).thenReturn(Arrays.asList(qProfile));

    RulesProfile profile = provider.provide(qProfiles, new ActiveRulesBuilder().build(), settings.asConfig());

    // merge of all profiles
    assertThat(profile).isNotNull().isInstanceOf(RulesProfileWrapper.class);
    assertThat(profile.getLanguage()).isEqualTo("");
    assertThat(profile.getName()).isEqualTo("SonarQube");
    assertThat(profile.getActiveRules()).isEmpty();
    try {
      profile.getId();
      fail();
    } catch (IllegalStateException e) {
      // id must not be used at all
    }
  }

  @Test
  public void keep_compatibility_with_single_language_projects() {
    settings.setProperty("sonar.language", "java");

    QProfile qProfile = new QProfile("java-sw", "Sonar way", "java", null);
    when(qProfiles.findByLanguage("java")).thenReturn(qProfile);

    RulesProfile profile = provider.provide(qProfiles, new ActiveRulesBuilder().build(), settings.asConfig());

    // no merge, directly the old hibernate profile
    assertThat(profile).isNotNull();
    assertThat(profile.getLanguage()).isEqualTo("java");
    assertThat(profile.getName()).isEqualTo("Sonar way");
  }

  @Test
  public void support_rule_templates() {
    QProfile qProfile = new QProfile("java-sw", "Sonar way", "java", null);
    when(qProfiles.findAll()).thenReturn(Arrays.asList(qProfile));
    ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();
    activeRulesBuilder.create(RuleKey.of("java", "S001")).setTemplateRuleKey("T001").setLanguage("java").activate();

    RulesProfile profile = provider.provide(qProfiles, activeRulesBuilder.build(), settings.asConfig());

    assertThat(profile.getActiveRule("java", "S001").getRule().getTemplate().getKey()).isEqualTo("T001");
  }
}
