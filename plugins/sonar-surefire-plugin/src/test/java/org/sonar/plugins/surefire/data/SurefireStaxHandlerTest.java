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
package org.sonar.plugins.surefire.data;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.utils.StaxParser;
import org.sonar.test.TestUtils;

import javax.xml.stream.XMLStreamException;
import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

public class SurefireStaxHandlerTest {

  private UnitTestIndex index;

  @Before
  public void setUp() {
    index = new UnitTestIndex();
  }

  @Test
  public void shouldLoadInnerClasses() throws XMLStreamException {
    parse("innerClasses.xml");

    UnitTestClassReport publicClass = index.get("org.apache.commons.collections.bidimap.AbstractTestBidiMap");
    assertThat(publicClass.getTests(), is(2L));

    UnitTestClassReport innerClass1 = index.get("org.apache.commons.collections.bidimap.AbstractTestBidiMap$TestBidiMapEntrySet");
    assertThat(innerClass1.getTests(), is(2L));

    UnitTestClassReport innerClass2 = index.get("org.apache.commons.collections.bidimap.AbstractTestBidiMap$TestInverseBidiMap");
    assertThat(innerClass2.getTests(), is(3L));
    assertThat(innerClass2.getDurationMilliseconds(), is(30 + 1L));
    assertThat(innerClass2.getErrors(), is(1L));
  }

  @Test
  public void shouldSuiteAsInnerClass() throws XMLStreamException {
    parse("suiteInnerClass.xml");
    assertThat(index.size(), is(0));
  }

  @Test
  public void shouldHaveSkippedTests() throws XMLStreamException {
    parse("skippedTests.xml");
    UnitTestClassReport report = index.get("org.sonar.Foo");
    assertThat(report.getTests(), is(3L));
    assertThat(report.getSkipped(), is(1L));
  }

  @Test
  public void shouldHaveZeroTests() throws XMLStreamException {
    parse("zeroTests.xml");
    assertThat(index.size(), is(0));
  }

  @Test
  public void shouldHaveTestOnRootPackage() throws XMLStreamException {
    parse("rootPackage.xml");
    assertThat(index.size(), is(1));
    UnitTestClassReport report = index.get("NoPackagesTest");
    assertThat(report.getTests(), is(2L));
  }

  @Test
  public void shouldHaveErrorsAndFailures() throws XMLStreamException {
    parse("errorsAndFailures.xml");
    UnitTestClassReport report = index.get("org.sonar.Foo");
    assertThat(report.getErrors(), is(1L));
    assertThat(report.getFailures(), is(1L));
    assertThat(report.getResults().size(), is(2));

    // failure
    UnitTestResult failure = report.getResults().get(0);
    assertThat(failure.getDurationMilliseconds(), is(5L));
    assertThat(failure.getStatus(), is(UnitTestResult.STATUS_FAILURE));
    assertThat(failure.getName(), is("testOne"));
    assertThat(failure.getMessage(), startsWith("expected"));

    // error
    UnitTestResult error = report.getResults().get(1);
    assertThat(error.getDurationMilliseconds(), is(0L));
    assertThat(error.getStatus(), is(UnitTestResult.STATUS_ERROR));
    assertThat(error.getName(), is("testTwo"));
  }

  @Test
  public void shouldSupportMultipleSuitesInSameReport() throws XMLStreamException {
    parse("multipleSuites.xml");

    assertThat(index.get("org.sonar.JavaNCSSCollectorTest").getTests(), is(11L));
    assertThat(index.get("org.sonar.SecondTest").getTests(), is(4L));
  }

  private void parse(String path) throws XMLStreamException {
    File xml = TestUtils.getResource(getClass(), path);
    SurefireStaxHandler staxParser = new SurefireStaxHandler(index);
    StaxParser parser = new StaxParser(staxParser, false);
    parser.parse(xml);
  }
}
