/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.batch.bootstrap;

import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.ComponentContainer;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.task.Task;
import org.sonar.api.task.TaskComponent;
import org.sonar.api.task.TaskDefinition;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.batch.scan.DeprecatedProjectReactorBuilder;
import org.sonar.batch.scan.ProjectReactorBuilder;
import org.sonar.batch.scan.ScanTask;
import org.sonar.batch.tasks.ListTask;
import org.sonar.batch.tasks.Tasks;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.resource.DefaultResourcePermissions;

import java.util.Map;

public class TaskContainer extends ComponentContainer {

  private final Map<String, String> taskProperties;

  public TaskContainer(ComponentContainer parent, Map<String, String> taskProperties) {
    super(parent);
    this.taskProperties = taskProperties;
  }

  @Override
  protected void doBeforeStart() {
    installCoreTasks();
    installTaskExtensions();
    installComponentsUsingTaskExtensions();
  }

  void installCoreTasks() {
    add(new TaskProperties(taskProperties, getParent().getComponentByType(BootstrapProperties.class).property(CoreProperties.ENCRYPTION_SECRET_KEY_PATH)));
    add(
      ScanTask.DEFINITION, ScanTask.class,
      ListTask.DEFINITION, ListTask.class,
      projectReactorBuilder());
  }

  private void installTaskExtensions() {
    getComponentByType(ExtensionInstaller.class).install(this, new ExtensionMatcher() {
      public boolean accept(Object extension) {
        return ExtensionUtils.isType(extension, TaskComponent.class);
      }
    });
  }

  private Class<?> projectReactorBuilder() {
    if (isRunnerVersionLessThan2Dot4()) {
      return DeprecatedProjectReactorBuilder.class;
    }
    return ProjectReactorBuilder.class;
  }

  private boolean isRunnerVersionLessThan2Dot4() {
    EnvironmentInformation env = this.getComponentByType(EnvironmentInformation.class);
    // Starting from SQ Runner 2.4 the key is "SonarQubeRunner"
    return env != null && "SonarRunner".equals(env.getKey());
  }

  private void installComponentsUsingTaskExtensions() {
    add(
      ResourceTypes.class,
      PermissionFacade.class,
      DefaultResourcePermissions.class,
      Tasks.class);
  }

  @Override
  public void doAfterStart() {
    // default value is declared in CorePlugin
    String taskKey = getComponentByType(Settings.class).getString(CoreProperties.TASK);

    TaskDefinition def = getComponentByType(Tasks.class).definition(taskKey);
    if (def == null) {
      throw MessageException.of("Task " + taskKey + " does not exist");
    }
    Task task = getComponentByType(def.taskClass());
    if (task != null) {
      task.execute();
    } else {
      throw new IllegalStateException("Task " + taskKey + " is badly defined");
    }
  }
}
