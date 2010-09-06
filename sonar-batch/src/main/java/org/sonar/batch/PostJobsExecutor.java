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
package org.sonar.batch;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.maven.DependsUponMavenPlugin;
import org.sonar.api.batch.maven.MavenPluginHandler;
import org.sonar.api.resources.Project;

import java.util.Collection;

public class PostJobsExecutor implements CoreJob {
  private static final Logger LOG = LoggerFactory.getLogger(PostJobsExecutor.class);

  private Collection<PostJob> postJobs;
  private MavenPluginExecutor mavenExecutor;

  public PostJobsExecutor(Project project, BatchExtensionDictionnary selector, MavenPluginExecutor mavenExecutor) {
    postJobs = selector.select(PostJob.class, project, true);
    this.mavenExecutor = mavenExecutor;
  }

  protected PostJobsExecutor(Collection<PostJob> postJobs, MavenPluginExecutor mavenExecutor) {
    this.postJobs = postJobs;
    this.mavenExecutor = mavenExecutor;
  }

  public void execute(Project project, SensorContext context) {
    if (shouldExecuteOn(project)) {
      logPostJobs();

      for (PostJob postJob : postJobs) {
        LOG.info("Executing post-job {}", postJob.getClass());
        executeMavenPlugin(project, postJob);
        postJob.executeOn(project, context);
      }
    }
  }

  private void logPostJobs() {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Post-jobs : {}", StringUtils.join(postJobs, " -> "));
    }
  }

  private boolean shouldExecuteOn(Project project) {
    return postJobs != null && project.isRoot();
  }


  private void executeMavenPlugin(Project project, PostJob job) {
    if (job instanceof DependsUponMavenPlugin) {
      MavenPluginHandler handler = ((DependsUponMavenPlugin) job).getMavenPluginHandler(project);
      if (handler != null) {
        mavenExecutor.execute(project, handler);
      }
    }
  }
}
