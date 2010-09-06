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

import org.junit.Test;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;

public class PostJobsExecutorTest {

  @Test
  public void doNotExecuteOnModules() {
    PostJob job1 = mock(PostJob.class);
    Project module = new Project("module").setParent(new Project("project"));

    PostJobsExecutor executor = new PostJobsExecutor(Arrays.<PostJob>asList(job1), mock(MavenPluginExecutor.class));
    executor.execute(module, mock(SensorContext.class));

    verify(job1, never()).executeOn((Project) anyObject(), (SensorContext) anyObject());
  }

  @Test
  public void executeAllPostJobs() {
    PostJob job1 = mock(PostJob.class);
    PostJob job2 = mock(PostJob.class);
    List<PostJob> jobs = Arrays.asList(job1, job2);

    PostJobsExecutor executor = new PostJobsExecutor(jobs, mock(MavenPluginExecutor.class));
    Project project = new Project("project");
    SensorContext context = mock(SensorContext.class);
    executor.execute(project, context);

    verify(job1).executeOn(project, context);
    verify(job2).executeOn(project, context);

  }

  static class FakePostJob implements PostJob {
    public void executeOn(Project project, SensorContext context) {
    }
  }
}
