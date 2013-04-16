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
package org.sonar.batch.phases;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.maven.DependsUponMavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;
import org.sonar.batch.events.EventBus;
import org.sonar.batch.local.DryRunExporter;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;
import org.sonar.batch.scan.maven.MavenPluginExecutor;

import java.util.Collection;

public class PostJobsExecutor implements BatchComponent {
  private static final Logger LOG = LoggerFactory.getLogger(PostJobsExecutor.class);

  private final BatchExtensionDictionnary selector;
  private final Project project;
  private final DefaultModuleFileSystem fs;
  private final MavenPluginExecutor mavenExecutor;
  private final DryRunExporter localModeExporter;
  private final EventBus eventBus;

  public PostJobsExecutor(BatchExtensionDictionnary selector, Project project, DefaultModuleFileSystem fs, MavenPluginExecutor mavenExecutor,
      DryRunExporter localModeExporter, EventBus eventBus) {
    this.selector = selector;
    this.project = project;
    this.fs = fs;
    this.mavenExecutor = mavenExecutor;
    this.localModeExporter = localModeExporter;
    this.eventBus = eventBus;
  }

  public void execute(SensorContext context) {
    Collection<PostJob> postJobs = selector.select(PostJob.class, project, true);

    eventBus.fireEvent(new PostJobPhaseEvent(Lists.newArrayList(postJobs), true));
    execute(context, postJobs);
    exportLocalModeResults(context);
    eventBus.fireEvent(new PostJobPhaseEvent(Lists.newArrayList(postJobs), false));
  }

  private void execute(SensorContext context, Collection<PostJob> postJobs) {
    logPostJobs(postJobs);

    for (PostJob postJob : postJobs) {
      LOG.info("Executing post-job {}", postJob.getClass());
      eventBus.fireEvent(new PostJobExecutionEvent(postJob, true));
      executeMavenPlugin(postJob);
      postJob.executeOn(project, context);
      eventBus.fireEvent(new PostJobExecutionEvent(postJob, false));
    }
  }

  private void logPostJobs(Collection<PostJob> postJobs) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Post-jobs : {}", StringUtils.join(postJobs, " -> "));
    }
  }

  private void executeMavenPlugin(PostJob job) {
    if (job instanceof DependsUponMavenPlugin) {
      MavenPluginHandler handler = ((DependsUponMavenPlugin) job).getMavenPluginHandler(project);
      if (handler != null) {
        mavenExecutor.execute(project, fs, handler);
      }
    }
  }

  private void exportLocalModeResults(SensorContext context) {
    localModeExporter.execute(context);
  }
}
