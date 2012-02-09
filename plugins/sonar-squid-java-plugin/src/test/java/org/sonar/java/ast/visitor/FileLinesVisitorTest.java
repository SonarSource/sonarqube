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
package org.sonar.java.ast.visitor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.resources.Resource;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.SquidTestUtils;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.plugins.squid.SonarAccessor;
import org.sonar.squid.Squid;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class FileLinesVisitorTest {

  private Squid squid;
  private SensorContext context;
  private FileLinesContext measures;

  @Before
  public void setUp() {
    squid = new Squid(new JavaSquidConfiguration());
    context = mock(SensorContext.class);
    measures = mock(FileLinesContext.class);
  }

  @Test
  public void analyseTestNcloc() {
    ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
    when(context.createFileLinesContext(resourceCaptor.capture()))
        .thenReturn(measures);

    squid.register(SonarAccessor.class).setSensorContext(context);
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/metrics/ncloc/TestNcloc.java"));

    assertThat(resourceCaptor.getValue().getKey(), is("[default].TestNcloc"));
    verify(measures).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 1, 1);
    verify(measures).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 2, 0);
    verify(measures).setIntValue(CoreMetrics.NCLOC_DATA_KEY, 3, 1);
    verify(measures).save();
  }

}
