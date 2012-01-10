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
package org.sonar.plugins.findbugs;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.SonarException;

public class FindbugsXmlReportParserTest {

  private List<FindbugsXmlReportParser.Violation> violations;

  @Before
  public void init() {
    File findbugsXmlReport = getFile("/org/sonar/plugins/findbugs/findbugsReport.xml");
    FindbugsXmlReportParser xmlParser = new FindbugsXmlReportParser(findbugsXmlReport);
    violations = xmlParser.getViolations();
  }

  @Test(expected = SonarException.class)
  public void createFindbugsXmlReportParserWithUnexistedReportFile() {
    File xmlReport = new File("doesntExist.xml");
    new FindbugsXmlReportParser(xmlReport);
  }

  @Test
  public void testGetViolations() {
    assertThat(violations.size(), is(3));

    FindbugsXmlReportParser.Violation fbViolation = violations.get(0);
    assertThat(fbViolation.getType(), is("AM_CREATES_EMPTY_ZIP_FILE_ENTRY"));
    assertThat(fbViolation.getLongMessage(),
        is("Empty zip file entry created in org.sonar.commons.ZipUtils._zip(String, File, ZipOutputStream)"));
    assertThat(fbViolation.getStart(), is(107));
    assertThat(fbViolation.getEnd(), is(107));
    assertThat(fbViolation.getClassName(), is("org.sonar.commons.ZipUtils"));
    assertThat(fbViolation.getSourcePath(), is("org/sonar/commons/ZipUtils.java"));
  }

  @Test
  public void testGetSonarJavaFileKey() {
    FindbugsXmlReportParser.Violation violation = new FindbugsXmlReportParser.Violation();
    violation.className = "org.sonar.batch.Sensor";
    assertThat(violation.getSonarJavaFileKey(), is("org.sonar.batch.Sensor"));
    violation.className = "Sensor";
    assertThat(violation.getSonarJavaFileKey(), is("Sensor"));
    violation.className = "org.sonar.batch.Sensor$1";
    assertThat(violation.getSonarJavaFileKey(), is("org.sonar.batch.Sensor"));
  }

  private final File getFile(String filename) {
    try {
      return new File(getClass().getResource(filename).toURI());
    } catch (URISyntaxException e) {
      throw new SonarException("Unable to open file " + filename, e);
    }
  }
}
