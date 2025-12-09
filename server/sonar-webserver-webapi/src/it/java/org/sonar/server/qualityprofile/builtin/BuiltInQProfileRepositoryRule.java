/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.qualityprofile.builtin;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.ExternalResource;
import org.sonar.api.resources.Language;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.db.rule.RuleDto;

import static com.google.common.base.Preconditions.checkState;

public class BuiltInQProfileRepositoryRule extends ExternalResource
  implements BuiltInQProfileRepository, BeforeEachCallback {
  private boolean initializeCalled = false;
  private List<BuiltInQProfile> profiles = new ArrayList<>();

  @Override
  public void beforeEach(ExtensionContext extensionContext) throws Exception {
    before();
  }

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

  public BuiltInQProfile create(BuiltInQualityProfilesDefinition.BuiltInQualityProfile api, RuleDto... rules) {
    BuiltInQProfile.Builder builder = new BuiltInQProfile.Builder()
      .setLanguage(api.language())
      .setName(api.name())
      .setDeclaredDefault(api.isDefault());
    Map<RuleKey, RuleDto> rulesByRuleKey = Arrays.stream(rules)
      .collect(Collectors.toMap(RuleDto::getKey, Function.identity()));
    api.rules().forEach(rule -> {
      RuleKey ruleKey = RuleKey.of(rule.repoKey(), rule.ruleKey());
      RuleDto ruleDto = rulesByRuleKey.get(ruleKey);
      Preconditions.checkState(ruleDto != null, "Rule '%s' not found", ruleKey);
      builder.addRule(new BuiltInQProfile.ActiveRule(ruleDto.getUuid(), rule));
    });
    return builder
      .build();
  }
}
