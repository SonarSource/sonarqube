/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.utils;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.InputStream;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class XpathParserTest {

  @Test
  public void parseXml() {
    InputStream xml = getClass().getResourceAsStream("/org/sonar/api/utils/XpathParserTest/sample.xml");
    try {
      XpathParser parser = new XpathParser();
      parser.parse(xml);
      assertThat(parser.getRoot().getNodeName(), is("samples"));
      assertThat(parser.getChildElements("sample").size(), is(2));
      assertThat(parser.getChildElements("sample").get(0).getAttribute("name"), is("one"));
      assertThat(parser.getChildElements("sample").get(1).getAttribute("name"), is("two"));

    } finally {
      IOUtils.closeQuietly(xml);
    }
  }

  @Test(expected=XmlParserException.class)
  public void unvalidXml() {
    InputStream xml = getClass().getResourceAsStream("/org/sonar/api/utils/XpathParserTest/unvalid.xml");
    try {
      XpathParser parser = new XpathParser();
      parser.parse(xml);
      
    } finally {
      IOUtils.closeQuietly(xml);
    }
  }
}
