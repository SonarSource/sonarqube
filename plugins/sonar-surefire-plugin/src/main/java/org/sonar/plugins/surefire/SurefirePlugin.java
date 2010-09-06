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
package org.sonar.plugins.surefire;

import org.sonar.api.*;

import java.util.ArrayList;
import java.util.List;

@Properties({
    @Property(
        key = CoreProperties.SUREFIRE_REPORTS_PATH_PROPERTY,
        name = "Report path",
        description = "Path (absolute or relative) to XML report files.",
        project = true,
        global = false)
})
public class SurefirePlugin implements Plugin {

  public String getKey() {
    return CoreProperties.SUREFIRE_PLUGIN;
  }

  public String getName() {
    return "Surefire";
  }

  public String getDescription() {
    return "<a href=' http://maven.apache.org/plugins/maven-surefire-plugin/'>Surefire</a> is a test framework for Maven.";
  }

  public List<Class<? extends Extension>> getExtensions() {
    List<Class<? extends Extension>> extensions = new ArrayList<Class<? extends Extension>>();
    extensions.add(SurefireSensor.class);
    return extensions;
  }

}
