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
import org.sonar.api.profiles.ProfilePrototype;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RulesRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DeprecatedProfiles {

  private RulesRepository[] deprecatedRepositories;
  private Plugins plugins;
  private CheckProfile[] deprecatedCheckProfiles;
  private CheckProfileProvider[] deprecatedCheckProfileProviders;

  public DeprecatedProfiles(Plugins plugins, RulesRepository[] r, CheckProfile[] deprecatedCheckProfiles, CheckProfileProvider[] deprecatedCheckProfileProviders) {
    this.deprecatedRepositories = r;
    this.plugins = plugins;
    this.deprecatedCheckProfiles = deprecatedCheckProfiles;
    this.deprecatedCheckProfileProviders = deprecatedCheckProfileProviders;
  }

  public DeprecatedProfiles(Plugins plugins, RulesRepository[] r, CheckProfile[] deprecatedCheckProfiles) {
    this.deprecatedRepositories = r;
    this.plugins = plugins;
    this.deprecatedCheckProfiles = deprecatedCheckProfiles;
    this.deprecatedCheckProfileProviders = new CheckProfileProvider[0];
  }

  public DeprecatedProfiles(Plugins plugins, RulesRepository[] r, CheckProfileProvider[] deprecatedCheckProfileProviders) {
    this.deprecatedRepositories = r;
    this.plugins = plugins;
    this.deprecatedCheckProfiles = new CheckProfile[0];
    this.deprecatedCheckProfileProviders = deprecatedCheckProfileProviders;
  }

  public DeprecatedProfiles(Plugins plugins, RulesRepository[] r) {
    this.deprecatedRepositories = r;
    this.plugins = plugins;
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
      for (ActiveRule activeRule : deprecated.getActiveRules()) {
        String repositoryKey = activeRule.getRepositoryKey();
        if (StringUtils.isBlank(repositoryKey)) {
          repositoryKey = getPluginKey(repository);
        }
        ProfilePrototype.RulePrototype rule = providedProfile.activateRule(repositoryKey, activeRule.getRuleKey(), activeRule.getPriority());
        for (ActiveRuleParam arp : activeRule.getActiveRuleParams()) {
          rule.setParameter(arp.getKey(), arp.getValue());
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
      ProfilePrototype.RulePrototype rule = definition.activateRule(check.getRepositoryKey(), check.getTemplateKey(), priority);
      for (Map.Entry<String, String> entry : rule.getParameters().entrySet()) {
        rule.setParameter(entry.getKey(), entry.getValue());
      }
    }
    return definition;
  }

  private String getPluginKey(RulesRepository repository) {
    return plugins.getPluginByExtension(repository).getKey();
  }

  public static class DefaultProfileDefinition extends ProfileDefinition {

    private ProfilePrototype prototype;

    DefaultProfileDefinition(String name, String language) {
      this.prototype = ProfilePrototype.create(name, language);
    }

    public static DefaultProfileDefinition create(String name, String language) {
      return new DefaultProfileDefinition(name, language);
    }

    @Override
    public ProfilePrototype createPrototype() {
      return prototype;
    }

    public List<ProfilePrototype.RulePrototype> getRules() {
      return prototype.getRules();
    }

    public List<ProfilePrototype.RulePrototype> getRulesByRepositoryKey(String repositoryKey) {
      return prototype.getRulesByRepositoryKey(repositoryKey);
    }

    public ProfilePrototype.RulePrototype activateRule(String repositoryKey, String key, RulePriority nullablePriority) {
      return prototype.activateRule(repositoryKey, key, nullablePriority);
    }
  }

}
