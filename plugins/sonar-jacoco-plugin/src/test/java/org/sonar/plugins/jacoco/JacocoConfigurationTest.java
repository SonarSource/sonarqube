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
package org.sonar.plugins.jacoco;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.plugins.java.api.JavaSettings;

import java.io.File;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JacocoConfigurationTest {

  private Settings settings;
  private JacocoConfiguration jacocoConfiguration;
  private JavaSettings javaSettings;

  @Before
  public void setUp() {
    JaCoCoAgentDownloader downloader = mock(JaCoCoAgentDownloader.class);
    when(downloader.getAgentJarFile()).thenReturn(new File("jacocoagent.jar"));
    javaSettings = mock(JavaSettings.class);
    settings = new Settings(new PropertyDefinitions(JacocoConfiguration.class));

    jacocoConfiguration = new JacocoConfiguration(settings, downloader, javaSettings);
  }

  @Test
  public void defaults() {
    assertThat(jacocoConfiguration.getReportPath(), is("target/jacoco.exec"));
    assertThat(jacocoConfiguration.getJvmArgument(), is("-javaagent:jacocoagent.jar=destfile=target/jacoco.exec,excludes=*_javassist_*"));

    assertThat(jacocoConfiguration.getItReportPath(), nullValue());

    assertThat(jacocoConfiguration.getAntTargets(), is(new String[]{}));
  }

  @Test
  public void shouldReturnAntTargets() {
    settings.setProperty(JacocoConfiguration.ANT_TARGETS_PROPERTY, "test");
    assertThat(jacocoConfiguration.getAntTargets(), is(new String[]{"test"}));

    settings.setProperty(JacocoConfiguration.ANT_TARGETS_PROPERTY, "test1,test2");
    assertThat(jacocoConfiguration.getAntTargets(), is(new String[]{"test1", "test2"}));
  }

  @Test
  public void shouldReturnItReportPath() {
    settings.setProperty(JacocoConfiguration.IT_REPORT_PATH_PROPERTY, "target/it-jacoco.exec");

    assertThat(jacocoConfiguration.getItReportPath(), is("target/it-jacoco.exec"));
  }

  @Test
  public void shouldSetDestfile() {
    settings.setProperty(JacocoConfiguration.REPORT_PATH_PROPERTY, "jacoco.exec");

    assertThat(jacocoConfiguration.getReportPath(), is("jacoco.exec"));
    assertThat(jacocoConfiguration.getJvmArgument(), is("-javaagent:jacocoagent.jar=destfile=jacoco.exec,excludes=*_javassist_*"));
  }

  @Test
  public void shouldSetIncludesAndExcludes() {
    settings.setProperty(JacocoConfiguration.INCLUDES_PROPERTY, "org.sonar.*");
    settings.setProperty(JacocoConfiguration.EXCLUDES_PROPERTY, "org.sonar.api.*");
    settings.setProperty(JacocoConfiguration.EXCLCLASSLOADER_PROPERTY, "sun.reflect.DelegatingClassLoader");

    assertThat(jacocoConfiguration.getJvmArgument(),
        is("-javaagent:jacocoagent.jar=destfile=target/jacoco.exec,includes=org.sonar.*,excludes=org.sonar.api.*,exclclassloader=sun.reflect.DelegatingClassLoader"));
  }

}
