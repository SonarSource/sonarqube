/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.plugins.java;

import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.commonrules.api.CommonRulesEngine;
import org.sonar.commonrules.api.CommonRulesEngineProvider;

public class JavaCommonRulesEngineProvider extends CommonRulesEngineProvider {

  public JavaCommonRulesEngineProvider() {
    super();
  }

  public JavaCommonRulesEngineProvider(Project project) {
    super(project);
  }

  @Override
  protected void doActivation(CommonRulesEngine engine) {
    engine.activateRule("InsufficientBranchCoverage");
    engine.activateRule("InsufficientCommentDensity");
    engine.activateRule("DuplicatedBlocks");
    engine.activateRule("InsufficientLineCoverage");
  }

  @Override
  protected String getLanguageKey() {
    return Java.KEY;
  }

}
