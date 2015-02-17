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
package org.sonar.plugins.core.sensors;

import org.junit.Test;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DirectoriesDecoratorTest {

  @Test
  public void doNotInsertZeroOnFiles() {
    DirectoriesDecorator decorator = new DirectoriesDecorator();
    Resource file = File.create("foo.php");
    DecoratorContext context = mock(DecoratorContext.class);

    decorator.decorate(file, context);

    verify(context, never()).saveMeasure(eq(CoreMetrics.DIRECTORIES), anyDouble());
  }

  @Test
  public void directoryCountsForOne() {
    DirectoriesDecorator decorator = new DirectoriesDecorator();
    Resource directory = Directory.create("org/foo");
    DecoratorContext context = mock(DecoratorContext.class);
    decorator.decorate(directory, context);
    verify(context).saveMeasure(CoreMetrics.DIRECTORIES, 1.0);
  }

  @Test
  public void countProjectDirectories() {
    DirectoriesDecorator decorator = new DirectoriesDecorator();
    Resource project = new Project("project");
    DecoratorContext context = mock(DecoratorContext.class);

    when(context.getChildrenMeasures(CoreMetrics.DIRECTORIES)).thenReturn(Arrays.<Measure>asList(
      new Measure(CoreMetrics.DIRECTORIES, 1.0),
      new Measure(CoreMetrics.DIRECTORIES, 1.0),
      new Measure(CoreMetrics.DIRECTORIES, 1.0)
      ));
    decorator.decorate(project, context);
    verify(context).saveMeasure(CoreMetrics.DIRECTORIES, 3.0);
  }

  @Test
  public void noProjectValueWhenOnlyPackages() {
    DirectoriesDecorator decorator = new DirectoriesDecorator();
    Resource project = new Project("project");
    DecoratorContext context = mock(DecoratorContext.class);
    when(context.getChildrenMeasures(CoreMetrics.DIRECTORIES)).thenReturn(Collections.<Measure>emptyList());
    when(context.getChildrenMeasures(CoreMetrics.PACKAGES)).thenReturn(Arrays.<Measure>asList(
      new Measure(CoreMetrics.PACKAGES, 1.0),
      new Measure(CoreMetrics.PACKAGES, 1.0)
      ));
    decorator.decorate(project, context);
    verify(context, never()).saveMeasure(eq(CoreMetrics.DIRECTORIES), anyDouble());
  }
}
