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
package org.sonar.api.server.profile;

import java.util.Map;
import java.util.function.Consumer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInQualityProfile;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.NewBuiltInActiveRule;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.NewBuiltInQualityProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

public class BuiltInQualityProfilesDefinitionTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void coverage() {
    assertThat(new BuiltInQualityProfilesDefinition.Context().profile("Foo", "xoo")).isNull();
  }

  @Test
  public void createEmptyProfile() {
    Map<String, Map<String, BuiltInQualityProfile>> profiles = define(c -> {
      c.createBuiltInQualityProfile("Foo", "xoo").done();
    });
    assertThat(profiles).containsOnlyKeys("xoo");
    assertThat(profiles.get("xoo")).containsOnlyKeys("Foo");
    BuiltInQualityProfile profile = profiles.get("xoo").get("Foo");
    assertThat(profile.name()).isEqualTo("Foo");
    assertThat(profile.language()).isEqualTo("xoo");
    assertThat(profile.isDefault()).isFalse();
  }

  @Test
  public void sanityCheck() {
    Map<String, Map<String, BuiltInQualityProfile>> profiles = define(c -> {
      NewBuiltInQualityProfile profile1 = c.createBuiltInQualityProfile("Foo1", "xoo");
      NewBuiltInActiveRule rule = profile1.activateRule("repo", "rule");
      profile1.done();
      NewBuiltInQualityProfile profile2 = c.createBuiltInQualityProfile("Foo2", "xoo");
      profile2.done();
      NewBuiltInQualityProfile profile3 = c.createBuiltInQualityProfile("Foo1", "xoo2");
      profile3.done();
      assertThat(profile1).isEqualTo(profile1);
      assertThat(profile1).isNotEqualTo(null);
      assertThat(profile1).isNotEqualTo("Foo");
      assertThat(profile1).isNotEqualTo(profile2);
      assertThat(profile1).isNotEqualTo(profile3);
      assertThat(profile1.hashCode()).isNotEqualTo(profile2.hashCode());
      assertThat(profile1.toString()).isEqualTo("NewBuiltInQualityProfile{name='Foo1', language='xoo', default='false'}");
      assertThat(rule.toString()).isEqualTo("[repository=repo, key=rule]");
    });
    BuiltInQualityProfile profile1 = profiles.get("xoo").get("Foo1");
    BuiltInQualityProfile profile2 = profiles.get("xoo").get("Foo2");
    BuiltInQualityProfile profile3 = profiles.get("xoo2").get("Foo1");
    assertThat(profile1).isEqualTo(profile1);
    assertThat(profile1).isNotEqualTo(null);
    assertThat(profile1).isNotEqualTo("Foo");
    assertThat(profile1).isNotEqualTo(profile2);
    assertThat(profile1).isNotEqualTo(profile3);
    assertThat(profile1.hashCode()).isNotEqualTo(profile2.hashCode());
    assertThat(profile1.toString()).isEqualTo("BuiltInQualityProfile{name='Foo1', language='xoo', default='false'}");
    assertThat(profile1.rule(RuleKey.of("repo", "rule")).toString()).isEqualTo("[repository=repo, key=rule]");
  }

  @Test
  public void createDefaultProfile() {
    Map<String, Map<String, BuiltInQualityProfile>> profiles = define(c -> {
      c.createBuiltInQualityProfile("Foo", "xoo")
        .setDefault(true)
        .done();
    });
    assertThat(profiles).containsOnlyKeys("xoo");
    assertThat(profiles.get("xoo")).containsOnlyKeys("Foo");
    BuiltInQualityProfile profile = profiles.get("xoo").get("Foo");
    assertThat(profile.name()).isEqualTo("Foo");
    assertThat(profile.language()).isEqualTo("xoo");
    assertThat(profile.isDefault()).isTrue();
  }

  @Test
  public void duplicateProfile() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("There is already a quality profile with name 'Foo' for language 'xoo'");
    define(c -> {
      c.createBuiltInQualityProfile("Foo", "xoo").done();
      c.createBuiltInQualityProfile("Foo", "xoo").done();
    });
  }

  @Test
  public void createProfileWithRules() {
    Map<String, Map<String, BuiltInQualityProfile>> profiles = define(c -> {
      NewBuiltInQualityProfile profile = c.createBuiltInQualityProfile("Foo", "xoo");
      profile.activateRule("repo", "ruleWithoutParam");
      profile.activateRule("repo", "ruleWithSeverity").overrideSeverity("CRITICAL");
      profile.activateRule("repo", "ruleWithParam").overrideParam("param", "value");
      profile.done();
    });
    assertThat(profiles).containsOnlyKeys("xoo");
    assertThat(profiles.get("xoo")).containsOnlyKeys("Foo");
    BuiltInQualityProfile profile = profiles.get("xoo").get("Foo");
    assertThat(profile.name()).isEqualTo("Foo");
    assertThat(profile.language()).isEqualTo("xoo");
    assertThat(profile.isDefault()).isFalse();
    assertThat(profile.rules())
      .extracting(BuiltInQualityProfilesDefinition.BuiltInActiveRule::repoKey, BuiltInQualityProfilesDefinition.BuiltInActiveRule::ruleKey,
        BuiltInQualityProfilesDefinition.BuiltInActiveRule::overriddenSeverity, r -> r.overriddenParams().size())
      .containsOnly(
        tuple("repo", "ruleWithoutParam", null, 0),
        tuple("repo", "ruleWithSeverity", "CRITICAL", 0),
        tuple("repo", "ruleWithParam", null, 1));
    assertThat(profile.rule(RuleKey.of("repo", "ruleWithParam")).overriddenParam("param").key()).isEqualTo("param");
    assertThat(profile.rule(RuleKey.of("repo", "ruleWithParam")).overriddenParam("param").overriddenValue()).isEqualTo("value");
  }

  @Test
  public void createProfileWithDuplicateRules() {

    define(c -> {
      NewBuiltInQualityProfile profile = c.createBuiltInQualityProfile("Foo", "xoo");
      profile.activateRule("repo", "rule");

      thrown.expect(IllegalArgumentException.class);
      thrown.expectMessage("The rule 'repo:rule' is already activated");

      profile.activateRule("repo", "rule");
    });
  }

  private Map<String, Map<String, BuiltInQualityProfile>> define(Consumer<BuiltInQualityProfilesDefinition.Context> consumer) {
    BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();
    new FakeProfile(consumer).define(context);
    return context.profilesByLanguageAndName();
  }

  private class FakeProfile implements BuiltInQualityProfilesDefinition {

    private Consumer<Context> consumer;

    public FakeProfile(Consumer<Context> consumer) {
      this.consumer = consumer;
    }

    @Override
    public void define(Context context) {
      consumer.accept(context);
    }

  }

}
