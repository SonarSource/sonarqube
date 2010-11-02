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
package org.sonar.plugins.findbugs;

import java.util.ArrayList;
import java.util.List;

import org.sonar.api.CoreProperties;
import org.sonar.api.Extension;
import org.sonar.api.Plugin;
import org.sonar.api.Properties;
import org.sonar.api.Property;

@Properties({
    @Property(
        key = CoreProperties.FINDBUGS_EFFORT_PROPERTY,
        defaultValue = CoreProperties.FINDBUGS_EFFORT_DEFAULT_VALUE,
        name = "Effort",
        description = "Effort of the bug finders. Valid values are Min, Default and Max. Setting 'Max' increases precision but also increases memory consumption.",
        project = true, module = true, global = true),
    @Property(
        key = CoreProperties.FINDBUGS_TIMEOUT_PROPERTY,
        defaultValue = CoreProperties.FINDBUGS_TIMEOUT_DEFAULT_VALUE + "",
        name = "Timeout",
        description = "Specifies the amount of time, in milliseconds, that FindBugs may run before it is assumed to be hung and is terminated. The default is 600,000 milliseconds, which is ten minutes.",
        project = true, module = true, global = true) })
public class FindbugsPlugin implements Plugin {

  public String getKey() {
    return CoreProperties.FINDBUGS_PLUGIN;
  }

  public String getName() {
    return "Findbugs";
  }

  public String getDescription() {
    return "FindBugs is a program that uses static analysis to look for bugs in Java code. It can detect a variety of common coding mistakes, including thread synchronization problems, misuse of API methods... You can find more by going to the <a href='http://findbugs.sourceforge.net'>Findbugs web site</a>.";
  }

  public List<Class<? extends Extension>> getExtensions() {
    List<Class<? extends Extension>> list = new ArrayList<Class<? extends Extension>>();
    list.add(FindbugsSensor.class);
    list.add(FindbugsConfiguration.class);
    list.add(FindbugsExecutor.class);
    list.add(FindbugsRuleRepository.class);
    list.add(FindbugsProfileExporter.class);
    list.add(FindbugsProfileImporter.class);
    list.add(SonarWayWithFindbugsProfile.class);
    return list;
  }
}
