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
package org.sonar.server.startup;

import org.sonar.api.qualitymodel.ModelDefinition;
import org.sonar.api.utils.MessageException;

/**
 * Verify that no quality models are defined by plugin as now a technical debt model is already provided. See SONAR-4752 for detail.
 * @see org.sonar.server.startup.RegisterTechnicalDebtModel
 */
public final class VerifyNoQualityModelsAreDefined {

  private final ModelDefinition[] definitions;

  public VerifyNoQualityModelsAreDefined(ModelDefinition[] definitions) {
    this.definitions = definitions;
  }

  public VerifyNoQualityModelsAreDefined() {
    this.definitions = new ModelDefinition[0];
  }

  public void start() {
    if (definitions.length > 0) {
      throw MessageException.of("The server could not start because the SQALE model definition is already provided by SonarQube. " +
        "You're probably using an old version of the SQALE plugin, please upgrade to a version compatible with the current version of SonarQube.");
    }
  }
}
