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

import java.io.IOException;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.postjob.PostJob;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.scanner.bootstrap.ScannerExtensionDictionnary;
import org.sonar.scanner.events.EventBus;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PostJobsExecutorTest {

  private PostJobsExecutor executor;

  private ScannerExtensionDictionnary selector = mock(ScannerExtensionDictionnary.class);
  private PostJob job1 = mock(PostJob.class);
  private PostJob job2 = mock(PostJob.class);
  private PostJobContext context = mock(PostJobContext.class);

  @Before
  public void setUp() throws IOException {
    executor = new PostJobsExecutor(selector, mock(EventBus.class));
  }

  @Test
  public void should_execute_post_jobs() {
    when(selector.selectPostJobs()).thenReturn(Arrays.asList(job1, job2));

    executor.execute(context);

    verify(job1).execute(eq(context));
    verify(job2).execute(eq(context));
  }
}
