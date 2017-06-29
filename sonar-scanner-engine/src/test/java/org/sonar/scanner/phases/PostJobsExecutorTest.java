/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.phases;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.PostJob;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.resources.Project;
import org.sonar.scanner.bootstrap.ScannerExtensionDictionnary;
import org.sonar.scanner.events.EventBus;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PostJobsExecutorTest {
  PostJobsExecutor executor;

  DefaultInputModule module = new DefaultInputModule("project");
  ScannerExtensionDictionnary selector = mock(ScannerExtensionDictionnary.class);
  PostJob job1 = mock(PostJob.class);
  PostJob job2 = mock(PostJob.class);
  SensorContext context = mock(SensorContext.class);

  @Before
  public void setUp() {
    executor = new PostJobsExecutor(selector, module, mock(EventBus.class));
  }

  @Test
  public void should_execute_post_jobs() {
    when(selector.select(PostJob.class, module, true, null)).thenReturn(Arrays.asList(job1, job2));

    executor.execute(context);

    verify(job1).executeOn(any(Project.class), eq(context));
    verify(job2).executeOn(any(Project.class), eq(context));
  }
}
