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
package org.sonar.api.ce.posttask;

import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask.Context;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask.ProjectAnalysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PostProjectAnalysisTaskTest {
  private final Context context = mock(Context.class);
  private final ProjectAnalysis projectAnalysis = Mockito.mock(ProjectAnalysis.class);

  @Test
  public void default_implementation_of_finished_ProjectAnalysis_throws_ISE() {
    PostProjectAnalysisTask underTest = new PostProjectAnalysisTask() {
      @Override
      public String getDescription() {
        throw new UnsupportedOperationException("getDescription not implemented");
      }
    };

    try {
      underTest.finished(projectAnalysis);
      fail("should have thrown an ISE");
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("Provide an implementation of method finished(Context)");
      Mockito.verifyZeroInteractions(projectAnalysis);
    }
  }

  @Test
  public void default_implementation_of_finished_Context_calls_finished_ProjectAnalysis() {
    when(context.getProjectAnalysis()).thenReturn(projectAnalysis);
    boolean[] called = {false};
    PostProjectAnalysisTask underTest = new PostProjectAnalysisTask() {

      // override default implementation which throws an exception
      @Override
      public void finished(ProjectAnalysis analysis) {
        called[0] = true;
        assertThat(analysis).isSameAs(projectAnalysis);
      }

      @Override
      public String getDescription() {
        throw new UnsupportedOperationException("getDescription not implemented");
      }
    };

    underTest.finished(context);

    assertThat(called[0]).isTrue();
    verify(context).getProjectAnalysis();
  }
}
