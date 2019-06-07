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
package org.sonar.scanner.cpd;

import org.sonar.api.CoreProperties;
import org.sonar.api.config.Configuration;
import org.sonar.duplications.block.BlockChunker;
import org.sonar.api.batch.fs.internal.DefaultInputProject;

public class CpdSettings {
  private final Configuration settings;

  public CpdSettings(Configuration config) {
    this.settings = config;
  }

  public boolean isCrossProjectDuplicationEnabled() {
    return settings.getBoolean(CoreProperties.CPD_CROSS_PROJECT).orElse(false);
  }

  /**
   * Not applicable to Java, as the {@link BlockChunker} that it uses does not record start and end units of each block.
   * Also, it uses statements instead of tokens.
   */
  int getMinimumTokens(String languageKey) {
    return settings.getInt("sonar.cpd." + languageKey + ".minimumTokens").orElse(100);
  }
}
