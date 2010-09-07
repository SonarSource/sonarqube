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

import org.sonar.api.Plugin;
import org.sonar.api.Plugins;
import org.sonar.api.ServerComponent;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ConfigurationExportable;
import org.sonar.api.rules.RulesRepository;
import org.sonar.api.utils.SonarException;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public final class DeprecatedProfileExporters implements ServerComponent {

  private Plugins plugins;
  private RulesRepository[] deprecatedRepositories;

  public DeprecatedProfileExporters(Plugins plugins, RulesRepository[] deprecatedRepositories) {
    this.deprecatedRepositories = deprecatedRepositories;
    this.plugins = plugins;
  }

  public DeprecatedProfileExporters(Plugins plugins) {
    this.deprecatedRepositories = new RulesRepository[0];
    this.plugins = plugins;
  }

  public List<ProfileExporter> create() {
    List<ProfileExporter> result = new ArrayList<ProfileExporter>();
    for (RulesRepository repo : deprecatedRepositories) {
      if (repo instanceof ConfigurationExportable) {
        result.add(new DeprecatedProfileExporter(getPlugin(repo), repo));
      }
    }
    return result;
  }

  private Plugin getPlugin(RulesRepository repository) {
    return plugins.getPluginByExtension(repository);
  }
}

class DeprecatedProfileExporter extends ProfileExporter {
  private RulesRepository exportableRepository;

  protected DeprecatedProfileExporter(Plugin plugin, RulesRepository exportableRepository) {
    super(plugin.getKey(), plugin.getName());
    this.exportableRepository = exportableRepository;
    setSupportedLanguages(exportableRepository.getLanguage().getKey());
    setMimeType("application/xml");
  }


  @Override
  public void exportProfile(RulesProfile profile, Writer writer) {
    String xml = ((ConfigurationExportable)exportableRepository).exportConfiguration(profile);
    if (xml != null) {
      try {
        writer.append(xml);
      } catch (IOException e) {
        throw new SonarException("Can not export profile", e);
      }
    }
  }
}