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
package org.sonar.scanner.phases;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.sonar.scanner.bootstrap.PostJobExtensionDictionnary;
import org.sonar.scanner.postjob.PostJobWrapper;
import org.sonar.scanner.postjob.PostJobsExecutor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PostJobsExecutorTest {
  private PostJobsExecutor executor;

  private PostJobExtensionDictionnary selector = mock(PostJobExtensionDictionnary.class);
  private PostJobWrapper job1 = mock(PostJobWrapper.class);
  private PostJobWrapper job2 = mock(PostJobWrapper.class);

  @Before
  public void setUp() {
    executor = new PostJobsExecutor(selector);
  }

  @Test
  public void should_execute_post_jobs() {
    when(selector.selectPostJobs()).thenReturn(Arrays.asList(job1, job2));
    when(job1.shouldExecute()).thenReturn(true);
    executor.execute();

    verify(job1).shouldExecute();
    verify(job2).shouldExecute();
    verify(job1).execute();
    verifyNoMoreInteractions(job1, job2);
  }
}
