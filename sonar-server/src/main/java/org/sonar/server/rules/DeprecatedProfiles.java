/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.rules;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.Plugins;
import org.sonar.api.checks.profiles.Check;
import org.sonar.api.checks.profiles.CheckProfile;
import org.sonar.api.checks.profiles.CheckProfileProvider;
import org.sonar.api.profiles.ProfileDefinition;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.*;
import org.sonar.api.utils.ValidationMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DeprecatedProfiles {

  private RulesRepository[] deprecatedRepositories;
  private Plugins plugins;
  private RuleFinder ruleFinder;
  private CheckProfile[] deprecatedCheckProfiles;
  private CheckProfileProvider[] deprecatedCheckProfileProviders;

  public DeprecatedProfiles(Plugins plugins, RuleFinder ruleFinder, RulesRepository[] r, CheckProfile[] deprecatedCheckProfiles, CheckProfileProvider[] deprecatedCheckProfileProviders) {
    this.deprecatedRepositories = r;
    this.plugins = plugins;
    this.ruleFinder = ruleFinder;
    this.deprecatedCheckProfiles = deprecatedCheckProfiles;
    this.deprecatedCheckProfileProviders = deprecatedCheckProfileProviders;
  }

  public DeprecatedProfiles(Plugins plugins, RuleFinder ruleFinder, RulesRepository[] r, CheckProfile[] deprecatedCheckProfiles) {
    this.deprecatedRepositories = r;
    this.plugins = plugins;
    this.ruleFinder = ruleFinder;
    this.deprecatedCheckProfiles = deprecatedCheckProfiles;
    this.deprecatedCheckProfileProviders = new CheckProfileProvider[0];
  }

  public DeprecatedProfiles(Plugins plugins, RuleFinder ruleFinder, RulesRepository[] r, CheckProfileProvider[] deprecatedCheckProfileProviders) {
    this.deprecatedRepositories = r;
    this.plugins = plugins;
    this.ruleFinder = ruleFinder;
    this.deprecatedCheckProfiles = new CheckProfile[0];
    this.deprecatedCheckProfileProviders = deprecatedCheckProfileProviders;
  }

  public DeprecatedProfiles(Plugins plugins, RuleFinder ruleFinder, RulesRepository[] r) {
    this.deprecatedRepositories = r;
    this.plugins = plugins;
    this.ruleFinder = ruleFinder;
    this.deprecatedCheckProfiles = new CheckProfile[0];
    this.deprecatedCheckProfileProviders = new CheckProfileProvider[0];
  }

  public List<ProfileDefinition> getProfiles() {
    List<ProfileDefinition> profiles = new ArrayList<ProfileDefinition>();
    for (RulesRepository repository : deprecatedRepositories) {
      profiles.addAll(loadFromDeprecatedRepository(repository));
    }
    for (CheckProfile deprecatedCheckProfile : deprecatedCheckProfiles) {
      profiles.add(loadFromDeprecatedCheckProfile(deprecatedCheckProfile));
    }
    for (CheckProfileProvider provider : deprecatedCheckProfileProviders) {
      for (CheckProfile deprecatedCheckProfile : provider.provide()) {
        profiles.add(loadFromDeprecatedCheckProfile(deprecatedCheckProfile));
      }
    }
    return profiles;
  }

  private List<ProfileDefinition> loadFromDeprecatedRepository(RulesRepository repository) {
    List<ProfileDefinition> result = new ArrayList<ProfileDefinition>();

    for (int index = 0; index < repository.getProvidedProfiles().size(); index++) {
      RulesProfile deprecated = (RulesProfile) repository.getProvidedProfiles().get(index);
      DefaultProfileDefinition providedProfile = DefaultProfileDefinition.create(deprecated.getName(), repository.getLanguage().getKey());
      for (ActiveRule deprecatedActiveRule : deprecated.getActiveRules()) {
        String repositoryKey = deprecatedActiveRule.getRepositoryKey();
        if (StringUtils.isBlank(repositoryKey)) {
          repositoryKey = getPluginKey(repository);
        }
        Rule rule = ruleFinder.findByKey(repositoryKey, deprecatedActiveRule.getRuleKey());
        if (rule != null) {
          ActiveRule activeRule = providedProfile.activateRule(rule, deprecatedActiveRule.getPriority());
          for (ActiveRuleParam arp : deprecatedActiveRule.getActiveRuleParams()) {
            activeRule.setParameter(arp.getKey(), arp.getValue());
          }
        }
      }
      result.add(providedProfile);
    }
    return result;
  }

  private ProfileDefinition loadFromDeprecatedCheckProfile(CheckProfile cp) {
    DefaultProfileDefinition definition = DefaultProfileDefinition.create(cp.getName(), cp.getLanguage());
    for (Check check : cp.getChecks()) {
      RulePriority priority = null;
      if (check.getPriority() != null) {
        priority = RulePriority.fromCheckPriority(check.getPriority());
      }
      Rule rule = ruleFinder.findByKey(check.getRepositoryKey(), check.getTemplateKey());
      if (rule != null) {
        ActiveRule activeRule = definition.activateRule(rule, priority);
        for (Map.Entry<String, String> entry : check.getProperties().entrySet()) {
          activeRule.setParameter(entry.getKey(), entry.getValue());
        }
      }
    }
    return definition;
  }

  private String getPluginKey(RulesRepository repository) {
    return plugins.getPluginByExtension(repository).getKey();
  }

  public static class DefaultProfileDefinition extends ProfileDefinition {

    private RulesProfile profile;

    DefaultProfileDefinition(String name, String language) {
      this.profile = RulesProfile.create(name, language);
    }

    public static DefaultProfileDefinition create(String name, String language) {
      return new DefaultProfileDefinition(name, language);
    }

    @Override
    public RulesProfile createProfile(ValidationMessages validation) {
      return profile;
    }

    public List<ActiveRule> getRules() {
      return profile.getActiveRules();
    }


    public List<ActiveRule> getRulesByRepositoryKey(String repositoryKey) {
      return profile.getActiveRulesByRepository(repositoryKey);
    }

    public ActiveRule activateRule(Rule rule, RulePriority nullablePriority) {
      return profile.activateRule(rule, nullablePriority);
    }
  }

}
