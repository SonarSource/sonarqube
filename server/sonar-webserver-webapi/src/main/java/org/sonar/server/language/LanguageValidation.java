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
package org.sonar.server.language;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.picocontainer.Startable;
import org.sonar.api.resources.Language;
import org.sonar.server.plugins.ServerPluginRepository;

public class LanguageValidation implements Startable {

  private final ServerPluginRepository pluginRepository;
  private final Language[] languages;

  public LanguageValidation(ServerPluginRepository pluginRepository) {
    this.pluginRepository = pluginRepository;
    this.languages = new Language[0];
  }

  public LanguageValidation(ServerPluginRepository pluginRepository, Language... languages) {
    this.pluginRepository = pluginRepository;
    this.languages = languages;
  }

  public void start() {
    Arrays.stream(languages).collect(Collectors.toMap(Language::getKey, x -> x, (x, y) -> {
      String pluginX = pluginRepository.getPluginKey(x);
      String pluginY = pluginRepository.getPluginKey(y);

      throw new IllegalStateException(String.format("There are two languages declared with the same key '%s' declared "
          + "by the plugins '%s' and '%s'. Please uninstall one of the conflicting plugins.",
        x.getKey(), pluginX, pluginY));
    }));
  }

  @Override public void stop() {
    // nothing to do
  }
}
