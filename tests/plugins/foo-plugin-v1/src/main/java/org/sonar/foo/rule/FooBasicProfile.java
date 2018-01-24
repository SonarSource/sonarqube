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
package org.sonar.foo.rule;

import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.ValidationMessages;

import static org.sonar.api.rules.RulePriority.MAJOR;
import static org.sonar.foo.Foo.KEY;
import static org.sonar.foo.rule.FooRulesDefinition.FOO_REPOSITORY;

public class FooBasicProfile extends ProfileDefinition {

  private final RuleFinder ruleFinder;

  public FooBasicProfile(RuleFinder ruleFinder) {
    this.ruleFinder = ruleFinder;
  }

  @Override
  public RulesProfile createProfile(ValidationMessages validation) {
    final RulesProfile profile = RulesProfile.create("Basic", KEY);
    activateRule(profile, FOO_REPOSITORY, "UnchangedRule", MAJOR);
    activateRule(profile, FOO_REPOSITORY, "ChangedRule", MAJOR);
    activateRule(profile, FOO_REPOSITORY, "ToBeDeactivatedRule", MAJOR);
    activateRule(profile, FOO_REPOSITORY, "ToBeRemovedRule", MAJOR);
    activateRule(profile, FOO_REPOSITORY, "RuleWithUnchangedParameter", MAJOR);
    activateRule(profile, FOO_REPOSITORY, "RuleWithChangedParameter", MAJOR);
    activateRule(profile, FOO_REPOSITORY, "RuleWithRemovedParameter", MAJOR);
    activateRule(profile, FOO_REPOSITORY, "RuleWithAddedParameter", MAJOR);
    activateRule(profile, FOO_REPOSITORY, "ToBeRenamed", MAJOR);
    activateRule(profile, FOO_REPOSITORY, "ToBeRenamedAndMoved", MAJOR);
    return profile;
  }

  private ActiveRule activateRule(RulesProfile profile, String repo, String key, RulePriority severity) {
    return profile.activateRule(ruleFinder.findByKey(repo, key), severity);
  }

}
