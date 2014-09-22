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
package org.sonar.java.api;

import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class JavaUtilsTest {

  @Test
  public void shouldAbbreviatePackage() {
    assertThat(JavaUtils.abbreviatePackage(""), is(""));
    assertThat(JavaUtils.abbreviatePackage("com"), is("com"));
    assertThat(JavaUtils.abbreviatePackage("com.foo"), is("com.f"));
    assertThat(JavaUtils.abbreviatePackage("com.foo.bar.buz"), is("com.f.b.b"));
    assertThat(JavaUtils.abbreviatePackage("..."), is(""));
    assertThat(JavaUtils.abbreviatePackage("com.foo."), is("com.f"));
    assertThat(JavaUtils.abbreviatePackage("com.foo..bar"), is("com.f.b"));
  }

  @Test
  public void shouldReturnDefaultJavaVersion() {
    Settings configuration = new Settings();
    Project project = new Project("").setSettings(configuration);

    assertThat(JavaUtils.getSourceVersion(project), is("1.5"));
    assertThat(JavaUtils.getTargetVersion(project), is("1.5"));
  }

  @Test
  public void shouldReturnSpecifiedJavaVersion() {
    Settings configuration = new Settings();
    Project project = new Project("").setSettings(configuration);
    configuration.setProperty(JavaUtils.JAVA_SOURCE_PROPERTY, "1.4");
    configuration.setProperty(JavaUtils.JAVA_TARGET_PROPERTY, "1.6");

    assertThat(JavaUtils.getSourceVersion(project), is("1.4"));
    assertThat(JavaUtils.getTargetVersion(project), is("1.6"));
  }

}
