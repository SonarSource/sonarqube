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
package org.sonar.plugins.jacoco;

import org.apache.tools.ant.*;
import org.sonar.api.batch.CoverageExtension;
import org.sonar.api.batch.Initializer;
import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.api.resources.Project;

import java.util.Map;

@SupportedEnvironment("ant")
public class JacocoAntInitializer extends Initializer implements CoverageExtension {

  private final TaskEnhancer[] taskEnhancers = new TaskEnhancer[] { new JavaLikeTaskEnhancer("java"), new JavaLikeTaskEnhancer("junit"), new TestngTaskEnhancer() };

  private org.apache.tools.ant.Project antProject;
  private JacocoConfiguration configuration;

  public JacocoAntInitializer(org.apache.tools.ant.Project antProject, JacocoConfiguration configuration) {
    this.antProject = antProject;
    this.configuration = configuration;
  }

  @Override
  public boolean shouldExecuteOnProject(org.sonar.api.resources.Project project) {
    return project.getAnalysisType().equals(Project.AnalysisType.DYNAMIC);
  }

  @Override
  public void execute(org.sonar.api.resources.Project project) {
    Map<String, Target> hastable = antProject.getTargets();
    String jvmArg = configuration.getJvmArgument();
    String[] names = configuration.getAntTargets();
    for (String name : names) {
      Target target = hastable.get(name);
      if (target == null) {
        JaCoCoUtils.LOG.warn("Target '{}' not found", name);
      } else {
        // Enhance target
        for (Task task : target.getTasks()) {
          for (TaskEnhancer enhancer : taskEnhancers) {
            if (enhancer.supportsTask(task)) {
              enhancer.enhanceTask(task, jvmArg);
            }
          }
        }
        // Execute target
        target.performTasks();
      }
    }
  }

  private static class TestngTaskEnhancer extends TaskEnhancer {
    @Override
    public boolean supportsTask(Task task) {
      return "testng".equals(task.getTaskName());
    }
  }

  /**
   * Basic task enhancer that can handle all 'java like' tasks. That is, tasks
   * that have a top level fork attribute and nested jvmargs elements
   */
  private static class JavaLikeTaskEnhancer extends TaskEnhancer {
    private String taskName;

    public JavaLikeTaskEnhancer(String taskName) {
      this.taskName = taskName;
    }

    @Override
    public boolean supportsTask(final Task task) {
      return taskName.equals(task.getTaskName());
    }

    @Override
    public void enhanceTask(final Task task, final String jvmArg) {
      final RuntimeConfigurable configurableWrapper = task.getRuntimeConfigurableWrapper();

      final String forkValue = (String) configurableWrapper.getAttributeMap().get("fork");

      if (forkValue == null || !org.apache.tools.ant.Project.toBoolean(forkValue)) {
        throw new BuildException("Coverage can only be applied on a forked VM");
      }

      super.enhanceTask(task, jvmArg);
    }

  }

  private abstract static class TaskEnhancer {
    /**
     * @param task Task instance to enhance
     * @return <code>true</code> if this enhancer is capable of enhancing the requested task
     */
    public abstract boolean supportsTask(Task task);

    /**
     * Attempt to enhance the supplied task with coverage information. This
     * operation may fail if the task is being executed in the current VM
     * 
     * @param task Task instance to enhance (usually an {@link UnknownElement})
     * @param jvmArg
     * @throws BuildException Thrown if this enhancer can handle this type of task, but this instance can not be enhanced for some reason.
     */
    public void enhanceTask(Task task, String jvmArg) {
      addJvmArg((UnknownElement) task, jvmArg);
    }

    public void addJvmArg(final UnknownElement task, final String jvmArg) {
      final UnknownElement el = new UnknownElement("jvmarg");
      el.setTaskName("jvmarg");
      el.setQName("jvmarg");

      final RuntimeConfigurable runtimeConfigurableWrapper = el.getRuntimeConfigurableWrapper();
      runtimeConfigurableWrapper.setAttribute("value", jvmArg);

      task.getRuntimeConfigurableWrapper().addChild(runtimeConfigurableWrapper);

      task.addChild(el);
    }
  }

}
