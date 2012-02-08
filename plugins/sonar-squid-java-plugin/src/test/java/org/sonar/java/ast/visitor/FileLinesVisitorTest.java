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
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.PersistenceMode;
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

  @Before
  public void setUp() {
    squid = new Squid(new JavaSquidConfiguration());
    context = mock(SensorContext.class);
  }

  @Test
  public void analyseTestNcloc() {
    squid.register(SonarAccessor.class).setSensorContext(context);
    squid.register(JavaAstScanner.class).scanFile(SquidTestUtils.getInputFile("/metrics/ncloc/TestNcloc.java"));

    ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
    ArgumentCaptor<Measure> measureCaptor = ArgumentCaptor.forClass(Measure.class);
    verify(context, times(1)).saveMeasure(resourceCaptor.capture(), measureCaptor.capture());
    assertThat(resourceCaptor.getValue().getKey(), is("[default].TestNcloc"));
    Measure measure = measureCaptor.getValue();
    assertThat(measure.getMetricKey(), is(CoreMetrics.NCLOC_DATA_KEY));
    assertThat(measure.getPersistenceMode(), is(PersistenceMode.DATABASE));
    assertThat(measure.getData(), is("1,3,4,5,6,7,8,13,14,15,16,17,19,20,21,22,23,24,25,26,27,28,29,30,32,39"));
  }

}
