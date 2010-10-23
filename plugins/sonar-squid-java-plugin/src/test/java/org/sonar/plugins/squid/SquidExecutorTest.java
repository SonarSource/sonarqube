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
package org.sonar.plugins.squid;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparisons.greaterThan;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.checks.AnnotationCheckFactory;
import org.sonar.api.checks.CheckFactory;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.squid.Squid;
import org.sonar.squid.measures.Metric;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;

public class SquidExecutorTest {

  @Test
  public void scanSources() throws IOException, URISyntaxException {
    SquidExecutor executor = new SquidExecutor(true, "LOG, logger", createCheckFactory(), Charset.defaultCharset());
    executor.scan(SquidTestUtils.getStrutsCoreSources(), Collections.<File> emptyList());

    assertThat(executor.isSourceScanned(), is(true));
    assertThat(executor.getSquid().getProject().getInt(Metric.LINES), greaterThan(1000));
  }

  @Test
  public void doNotScanBytecodeIfNoSources() throws IOException, URISyntaxException {
    SquidExecutor executor = new SquidExecutor(true, "LOG, logger", createCheckFactory(), Charset.defaultCharset());
    executor.scan(Collections.<File> emptyList(), Arrays.asList(SquidTestUtils.getStrutsCoreJar()));

    assertThat(executor.isSourceScanned(), is(false));
    assertThat(executor.isBytecodeScanned(), is(false));
  }

  @Test
  public void scanBytecode() throws IOException, URISyntaxException {
    SquidExecutor executor = new SquidExecutor(true, "LOG, logger", createCheckFactory(), Charset.defaultCharset());
    executor.scan(SquidTestUtils.getStrutsCoreSources(), Arrays.asList(SquidTestUtils.getStrutsCoreJar()));

    assertThat(executor.isSourceScanned(), is(true));
    assertThat(executor.getSquid().getProject().getInt(Metric.LINES), greaterThan(1000));
    assertThat(executor.isBytecodeScanned(), is(true));
  }

  @Test
  public void doNotSaveMeasuresIfSourceNotScanned() {
    SquidExecutor executor = new SquidExecutor(true, "LOG, logger", createCheckFactory(), Charset.defaultCharset());
    SensorContext context = mock(SensorContext.class);

    assertThat(executor.isSourceScanned(), is(false));
    executor.save(new Project("p1"), context, null);

    verifyZeroInteractions(context);
  }

  @Test
  public void scanThenSaveMeasures() throws IOException, URISyntaxException {
    SquidExecutor executor = new SquidExecutor(true, "LOG, logger", createCheckFactory(), Charset.defaultCharset());
    SensorContext context = mock(SensorContext.class);

    executor.scan(SquidTestUtils.getStrutsCoreSources(), Collections.<File> emptyList());
    executor.save(new Project("p1"), context, null);

    assertThat(executor.isSourceScanned(), is(true));
    executor.save(new Project("p1"), context, null);

    verify(context, atLeast(100)).saveMeasure((Resource) anyObject(), (org.sonar.api.measures.Metric) anyObject(), anyDouble());
  }

  @Test
  public void flushSquidAfterUsage() {
    Squid squid = mock(Squid.class);
    SquidExecutor executor = new SquidExecutor(squid);
    executor.initSonarProxy(new SquidSearchProxy());

    verify(squid).flush();
  }

  private CheckFactory createCheckFactory() {
    RulesProfile profile = RulesProfile.create();
    CheckFactory checkFactory = AnnotationCheckFactory.create(profile, "repo", Collections.<Class> emptyList());
    return checkFactory;
  }
}
