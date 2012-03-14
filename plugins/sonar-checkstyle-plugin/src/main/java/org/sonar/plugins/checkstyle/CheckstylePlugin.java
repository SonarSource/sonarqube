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
package org.sonar.plugins.checkstyle;

import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

@Properties({
  @Property(key = CheckstyleConstants.FILTERS_KEY,
    defaultValue = CheckstyleConstants.FILTERS_DEFAULT_VALUE,
    name = "Filters",
    description = "Checkstyle support three error filtering mechanisms : SuppressionCommentFilter, SuppressWithNearbyCommentFilter and SuppressionFilter."
      + "This property allows to configure all those filters with a native XML format."
      + " See <a href='http://checkstyle.sourceforge.net/config.html'>Checkstyle configuration page</a> to get more information on those filters.",
    project = false,
    global = true,
    type = Property.Type.TEXT)})
public final class CheckstylePlugin extends SonarPlugin {

  public List getExtensions() {
    return Arrays.asList(
      CheckstyleSensor.class,
      CheckstyleConfiguration.class,
      CheckstyleExecutor.class,
      CheckstyleAuditListener.class,
      CheckstyleProfileExporter.class,
      CheckstyleProfileImporter.class,
      CheckstyleRuleRepository.class,
      SonarWayProfile.class,
      SunConventionsProfile.class,
      SonarWayWithFindbugsProfile.class);
  }
}
