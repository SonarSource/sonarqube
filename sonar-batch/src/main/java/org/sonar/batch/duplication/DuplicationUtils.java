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
package org.sonar.batch.duplication;

import org.apache.commons.lang.StringEscapeUtils;
import org.sonar.api.batch.sensor.duplication.Duplication;
import org.sonar.api.batch.sensor.duplication.internal.DefaultDuplication;

public class DuplicationUtils {

  private DuplicationUtils() {
    // Utility class
  }

  public static String toXml(Iterable<DefaultDuplication> duplications) {
    StringBuilder xml = new StringBuilder();
    xml.append("<duplications>");
    for (Duplication duplication : duplications) {
      xml.append("<g>");
      toXml(xml, duplication.originBlock());
      for (Duplication.Block part : duplication.duplicates()) {
        toXml(xml, part);
      }
      xml.append("</g>");
    }
    xml.append("</duplications>");
    return xml.toString();
  }

  private static void toXml(StringBuilder xml, Duplication.Block part) {
    xml.append("<b s=\"").append(part.startLine())
      .append("\" l=\"").append(part.length())
      .append("\" r=\"").append(StringEscapeUtils.escapeXml(part.resourceKey()))
      .append("\"/>");
  }
}
