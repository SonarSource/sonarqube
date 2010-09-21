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
package org.sonar.server.startup;

import org.sonar.api.utils.TimeProfiler;
import org.sonar.server.qualitymodel.ModelManager;

public final class RegisterQualityModels {

  private ModelManager manager;

  /**
   * @param registerRulesBeforeModels used only to be started after the creation of check templates
   */
  // NOSONAR the parameter registerRulesBeforeModels is only used to provide the execution order by picocontainer
  public RegisterQualityModels(ModelManager manager, RegisterRules registerRulesBeforeModels) {
    this.manager = manager;
  }

  public void start() {
    TimeProfiler profiler = new TimeProfiler().start("Register quality models");
    manager.registerDefinitions();
    profiler.stop();
  }
}
