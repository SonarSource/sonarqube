/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.rule;

import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleDefinitions;

/**
 * Loads all instances of RuleDefinitions and initializes RuleRepositories. Used at server startup.
 */
public class RuleDefinitionsLoader implements ServerComponent {
  private final RuleDefinitions[] definitions;
  private final RuleRepositories repositories;

  public RuleDefinitionsLoader(RuleRepositories repositories, RuleDefinitions[] definitions) {
    this.repositories = repositories;
    this.definitions = definitions;
  }

  public RuleDefinitionsLoader(RuleRepositories repositories) {
    this(repositories, new RuleDefinitions[0]);
  }

  public RuleDefinitions.Context load() {
    RuleDefinitions.Context context = new RuleDefinitions.Context();
    for (RuleDefinitions definition : definitions) {
      definition.define(context);
    }
    repositories.register(context);
    return context;
  }
}
