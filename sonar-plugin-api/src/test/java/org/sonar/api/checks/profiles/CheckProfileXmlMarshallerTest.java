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
package org.sonar.api.checks.profiles;

import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.sonar.check.Priority;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class CheckProfileXmlMarshallerTest {

  private String expectedXml;

  @Before
  public void loadExpectedXml() throws IOException {
    InputStream input = getClass().getResourceAsStream("/org/sonar/api/checks/profiles/CheckProfileXmlMarshallerTest/profile.xml");
    try {
      expectedXml = IOUtils.toString(input);
    } finally {
      IOUtils.closeQuietly(input);
    }
  }


  @Test
  public void toXml() throws IOException, SAXException {
    CheckProfile profile = new CheckProfile("one", "java");
    Check check1 = new Check("checkstyle", "C1");
    check1.setPriority(Priority.MINOR);
    check1.addProperty("min", "1");
    check1.addProperty("max", "3");
    profile.addCheck(check1);

    StringWriter writer = new StringWriter();
    CheckProfileXmlMarshaller.toXml(profile, writer);

    XMLUnit.setIgnoreWhitespace(true);
    Diff diff = XMLUnit.compareXML(expectedXml, writer.toString());
    assertTrue(diff.similar());
  }


  @Test
  public void fromXml() throws IOException, SAXException {
    CheckProfile profile = CheckProfileXmlMarshaller.fromXml(new StringReader(expectedXml));
    assertThat(profile.getName(), is("one"));
    assertThat(profile.getLanguage(), is("java"));
    assertThat(profile.getChecks("checkstyle").size(), is(1));
    assertThat(profile.getChecks("unknown").size(), is(0));

    Check check = profile.getChecks("checkstyle").get(0);
    assertThat(check.getRepositoryKey(), is("checkstyle"));
    assertThat(check.getTemplateKey(), is("C1"));
    assertThat(check.getPriority(), is(Priority.MINOR));

    assertThat(check.getProperties().size(), is(2));
    assertThat(check.getProperty("min"), is("1"));
    assertThat(check.getProperty("max"), is("3"));
  }

  @Test
  public void fromXmlInClasspath() {
    CheckProfile profile = CheckProfileXmlMarshaller.fromXmlInClasspath("/org/sonar/api/checks/profiles/CheckProfileXmlMarshallerTest/profile.xml");
    assertThat(profile.getName(), is("one"));
  }
}
