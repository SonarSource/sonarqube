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

import org.apache.commons.io.IOUtils;
import org.sonar.api.Plugin;
import org.sonar.api.Plugins;
import org.sonar.api.profiles.ProfileImporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.*;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.ValidationMessages;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class DeprecatedProfileImporters {

  private Plugins plugins;
  private RuleFinder ruleFinder;
  private RulesRepository[] deprecatedRepositories;

  public DeprecatedProfileImporters(Plugins plugins, RuleFinder ruleFinder, RulesRepository[] deprecatedRepositories) {
    this.deprecatedRepositories = deprecatedRepositories;
    this.plugins = plugins;
    this.ruleFinder = ruleFinder;
  }

  public DeprecatedProfileImporters(Plugins plugins, RuleFinder ruleFinder) {
    this.deprecatedRepositories = new RulesRepository[0];
    this.plugins = plugins;
    this.ruleFinder = ruleFinder;
  }

  public List<ProfileImporter> create() {
    List<ProfileImporter> result = new ArrayList<ProfileImporter>();
    for (RulesRepository repo : deprecatedRepositories) {
      if (repo instanceof ConfigurationImportable) {
        result.add(new DeprecatedProfileImporter(getPlugin(repo), ruleFinder, repo));
      }
    }
    return result;
  }

  private Plugin getPlugin(RulesRepository repository) {
    return plugins.getPluginByExtension(repository);
  }
}

class DeprecatedProfileImporter extends ProfileImporter {
  private RulesRepository importableRepository;
  private RuleFinder ruleFinder;

  protected DeprecatedProfileImporter(Plugin plugin, RuleFinder ruleFinder, RulesRepository importableRepository) {
    super(plugin.getKey(), plugin.getName());
    this.importableRepository = importableRepository;
    this.ruleFinder = ruleFinder;
    setSupportedLanguages(importableRepository.getLanguage().getKey());
  }

  @Override
  public RulesProfile importProfile(Reader reader, ValidationMessages messages) {
    List<Rule> rules = new ArrayList<Rule>(ruleFinder.findAll(RuleQuery.create().withRepositoryKey(getKey())));
    try {
      RulesProfile profile = RulesProfile.create(getKey(), getName());
      List<ActiveRule> activeRules = ((ConfigurationImportable) importableRepository).importConfiguration(IOUtils.toString(reader), rules);
      profile.setActiveRules(activeRules);
      return profile;

    } catch (IOException e) {
      throw new SonarException("Fail to load the profile definition", e);
    }
  }
}
