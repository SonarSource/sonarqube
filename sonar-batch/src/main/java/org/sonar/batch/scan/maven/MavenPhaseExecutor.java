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
package org.sonar.batch.scan.maven;

import com.google.common.base.Strings;
import org.sonar.api.BatchComponent;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;

public class MavenPhaseExecutor implements BatchComponent {

  public static final String PROP_PHASE = "sonar.phase";

  private final MavenPluginExecutor executor;
  private final DefaultModuleFileSystem fs;
  private final Settings settings;

  public MavenPhaseExecutor(DefaultModuleFileSystem fs, MavenPluginExecutor executor, Settings settings) {
    this.fs = fs;
    this.executor = executor;
    this.settings = settings;
  }

  public void execute(Project project) {
    String mavenPhase = settings.getString(PROP_PHASE);
    if (!Strings.isNullOrEmpty(mavenPhase)) {
      executor.execute(project, fs, mavenPhase);
    }
  }
}
