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
package org.sonar.server.qualityprofile;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;

import static java.lang.String.format;

/**
 * Bridge between deprecated {@link ProfileDefinition} API and new {@link BuiltInQualityProfilesDefinition}
 */
public class BuiltInQProfileDefinitionsBridge implements BuiltInQualityProfilesDefinition {
  private static final Logger LOGGER = Loggers.get(BuiltInQProfileDefinitionsBridge.class);

  private final List<ProfileDefinition> definitions;

  /**
   * Requires for pico container when no {@link ProfileDefinition} is defined at all
   */
  public BuiltInQProfileDefinitionsBridge() {
    this(new ProfileDefinition[0]);
  }

  public BuiltInQProfileDefinitionsBridge(ProfileDefinition... definitions) {
    this.definitions = ImmutableList.copyOf(definitions);
  }

  @Override
  public void define(Context context) {
    Profiler profiler = Profiler.create(Loggers.get(getClass()));
    for (ProfileDefinition definition : definitions) {
      profiler.start();
      ValidationMessages validation = ValidationMessages.create();
      RulesProfile profile = definition.createProfile(validation);
      validation.log(LOGGER);
      if (profile == null) {
        profiler.stopDebug(format("Loaded definition %s that return no profile", definition));
      } else {
        if (!validation.hasErrors()) {
          define(context, profile);
        }
        profiler.stopDebug(format("Loaded deprecated profile definition %s for language %s", profile.getName(), profile.getLanguage()));
      }
    }
  }

  private static void define(Context context, RulesProfile profile) {
    NewBuiltInQualityProfile newQp = context.createBuiltInQualityProfile(profile.getName(), profile.getLanguage())
      .setDefault(profile.getDefaultProfile().booleanValue());

    for (org.sonar.api.rules.ActiveRule ar : profile.getActiveRules()) {
      NewBuiltInActiveRule newActiveRule = newQp.activateRule(ar.getRepositoryKey(), ar.getRuleKey());
      RulePriority overriddenSeverity = ar.getOverriddenSeverity();
      if (overriddenSeverity != null) {
        newActiveRule.overrideSeverity(overriddenSeverity.name());
      }
      for (ActiveRuleParam param : ar.getActiveRuleParams()) {
        newActiveRule.overrideParam(param.getKey(), param.getValue());
      }
    }
    newQp.done();
  }

}
