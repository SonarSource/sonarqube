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
package org.sonar.xoo.extensions;

import java.io.File;
import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;

public class XooProjectBuilder extends ProjectBuilder {

  private Settings settings;

  public XooProjectBuilder(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void build(Context context) {
    if (!settings.getBoolean("sonar.xoo.enableProjectBuilder")) {
      return;
    }
    ProjectDefinition root = context.projectReactor().getRoot();
    root.resetSources();

    ProjectDefinition module = ProjectDefinition.create()
      .setKey(root.getKey() + ":module1")
      .setName("Module 1");

    module.setBaseDir(new File(root.getBaseDir(), "module1"));
    module.setWorkDir(new File(root.getWorkDir(), "module1"));

    module.setSources("src");

    root.addSubProject(module);
  }

}
