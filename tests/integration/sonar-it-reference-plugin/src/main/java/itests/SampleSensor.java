/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package itests;

import org.apache.commons.i18n.bundles.MessageBundle;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;

public class SampleSensor implements Sensor {
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void analyse(Project project, SensorContext context) {
    System.out.println(" Check usage of plugin dependencies (new feature since sonar 2.2)");


    System.out.print("Loading external dependency from " + getClass().getName() + ": ");
    MessageBundle bundle = new MessageBundle("12345");
    System.out.println("OK");

    System.out.print("  Plugin isolation (can not access other plugin classes): ");
    try {
      Class.forName("org.sonar.plugins.checkstyle.CheckstyleSensor");
      System.out.println("KO");

    } catch (ClassNotFoundException e) {
      System.out.println("OK");
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }
}
