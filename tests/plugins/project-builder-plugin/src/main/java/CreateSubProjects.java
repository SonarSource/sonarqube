/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import java.io.File;
import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Settings;

/**
 * This plugin relates to projects/project-builder sample
 */
public final class CreateSubProjects extends ProjectBuilder {

  private Settings settings;

  public CreateSubProjects(Settings settings) {
    // A real implementation should for example use the configuration
    this.settings = settings;
  }

  @Override
  protected void build(ProjectReactor reactor) {
    if (!settings.getBoolean("sonar.enableProjectBuilder")) {
      return;
    }
    System.out.println("---> Creating sub-projects");
    ProjectDefinition root = reactor.getRoot();

    // add two modules
    createSubProjectWithSourceDir(root);
    createSubProjectWithSourceFiles(root);
  }

  private ProjectDefinition createSubProjectWithSourceDir(ProjectDefinition root) {
    File baseDir = new File(root.getBaseDir(), "module_a");
    ProjectDefinition subProject = ProjectDefinition.create();
    subProject.setBaseDir(baseDir).setWorkDir(new File(baseDir, "target/.sonar"));
    subProject.setKey("com.sonarsource.it.projects.batch:project-builder-module-a");
    subProject.setVersion(root.getVersion());
    subProject.setName("Module A");
    subProject.addSources("src");
    root.addSubProject(subProject);
    return subProject;
  }

  private ProjectDefinition createSubProjectWithSourceFiles(ProjectDefinition root) {
    File baseDir = new File(root.getBaseDir(), "module_b");
    ProjectDefinition subProject = ProjectDefinition.create();
    subProject.setBaseDir(baseDir).setWorkDir(new File(baseDir, "target/.sonar"));
    subProject.setKey("com.sonarsource.it.projects.batch:project-builder-module-b");
    subProject.setVersion(root.getVersion());
    subProject.setName("Module B");
    subProject.addSources("src/HelloB.java");
    root.addSubProject(subProject);
    return subProject;
  }
}
