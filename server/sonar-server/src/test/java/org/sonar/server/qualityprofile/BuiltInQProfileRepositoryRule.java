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
package org.sonar.server.qualityprofile;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.rules.ExternalResource;
import org.sonar.api.resources.Language;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.rule.RuleDefinitionDto;

import static com.google.common.base.Preconditions.checkState;

public class BuiltInQProfileRepositoryRule extends ExternalResource implements BuiltInQProfileRepository {
  private boolean initializeCalled = false;
  private List<BuiltInQProfile> profiles = new ArrayList<>();

  @Override
  protected void before() {
    this.initializeCalled = false;
    this.profiles.clear();
  }

  @Override
  public void initialize() {
    checkState(!initializeCalled, "initialize must be called only once");
    this.initializeCalled = true;
  }

  @Override
  public List<BuiltInQProfile> get() {
    checkState(initializeCalled, "initialize must be called first");

    return ImmutableList.copyOf(profiles);
  }

  public boolean isInitialized() {
    return initializeCalled;
  }

  public BuiltInQProfile add(Language language, String profileName) {
    return add(language, profileName, false);
  }

  public BuiltInQProfile add(Language language, String profileName, boolean isDefault) {
    return add(language, profileName, isDefault, new BuiltInQProfile.ActiveRule[0]);
  }

  public BuiltInQProfile add(Language language, String profileName, boolean isDefault, BuiltInQProfile.ActiveRule... rules) {
    BuiltInQProfile builtIn = create(language, profileName, isDefault, rules);
    profiles.add(builtIn);
    return builtIn;
  }

  public BuiltInQProfile create(Language language, String profileName, boolean isDefault, BuiltInQProfile.ActiveRule... rules) {
    BuiltInQProfile.Builder builder = new BuiltInQProfile.Builder()
      .setLanguage(language.getKey())
      .setName(profileName)
      .setDeclaredDefault(isDefault);
    Arrays.stream(rules).forEach(builder::addRule);
    return builder.build();
  }

  public BuiltInQProfile create(BuiltInQualityProfilesDefinition.BuiltInQualityProfile api, RuleDefinitionDto... rules) {
    BuiltInQProfile.Builder builder = new BuiltInQProfile.Builder()
      .setLanguage(api.language())
      .setName(api.name())
      .setDeclaredDefault(api.isDefault());
    Map<RuleKey, RuleDefinitionDto> rulesByRuleKey = Arrays.stream(rules)
      .collect(MoreCollectors.uniqueIndex(RuleDefinitionDto::getKey));
    api.rules().forEach(rule -> {
      RuleKey ruleKey = RuleKey.of(rule.repoKey(), rule.ruleKey());
      RuleDefinitionDto ruleDefinition = rulesByRuleKey.get(ruleKey);
      Preconditions.checkState(ruleDefinition != null, "Rule '%s' not found", ruleKey);
      builder.addRule(rule, ruleDefinition.getId());
    });
    return builder
      .build();
  }
}
