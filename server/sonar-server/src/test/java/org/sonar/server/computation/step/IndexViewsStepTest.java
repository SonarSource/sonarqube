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

package org.sonar.server.computation.step;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.resources.Qualifiers;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.view.index.ViewIndexer;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class IndexViewsStepTest {

  @Mock
  ComputationContext context;

  @Mock
  ViewIndexer indexer;

  IndexViewsStep step;

  @Before
  public void setUp() throws Exception {
    step = new IndexViewsStep(indexer);
  }

  @Test
  public void index_view_on_view() throws Exception {
    when(context.getProject()).thenReturn(ComponentTesting.newProjectDto("ABCD").setQualifier(Qualifiers.VIEW));

    step.execute(context);

    verify(indexer).index("ABCD");
  }

  @Test
  public void not_index_view_on_project() throws Exception {
    when(context.getProject()).thenReturn(ComponentTesting.newProjectDto().setQualifier(Qualifiers.PROJECT));

    step.execute(context);

    verifyZeroInteractions(indexer);
  }

}
