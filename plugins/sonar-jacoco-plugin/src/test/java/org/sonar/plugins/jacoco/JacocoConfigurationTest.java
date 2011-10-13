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
package org.sonar.plugins.jacoco;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class JacocoConfigurationTest {

  private Configuration configuration;
  private JacocoConfiguration jacocoConfiguration;

  @Before
  public void setUp() {
    JaCoCoAgentDownloader downloader = mock(JaCoCoAgentDownloader.class);
    when(downloader.getAgentJarFile()).thenReturn(new File("jacocoagent.jar"));

    configuration = new BaseConfiguration();

    jacocoConfiguration = new JacocoConfiguration(configuration, downloader);
  }

  @Test
  public void defaults() {
    assertThat(jacocoConfiguration.getReportPath(), is("target/jacoco.exec"));
    assertThat(jacocoConfiguration.getJvmArgument(), is("-javaagent:jacocoagent.jar=destfile=target/jacoco.exec"));

    assertThat(jacocoConfiguration.getItReportPath(), is(""));

    assertThat(jacocoConfiguration.getAntTargets(), is(new String[] {}));
  }

  @Test
  public void shouldReturnAntTargets() {
    configuration.setProperty(JacocoConfiguration.ANT_TARGETS_PROPERTY, "test");
    assertThat(jacocoConfiguration.getAntTargets(), is(new String[] { "test" }));

    configuration.setProperty(JacocoConfiguration.ANT_TARGETS_PROPERTY, "test1,test2");
    assertThat(jacocoConfiguration.getAntTargets(), is(new String[] { "test1", "test2" }));
  }

  @Test
  public void shouldReturnItReportPath() {
    configuration.setProperty(JacocoConfiguration.IT_REPORT_PATH_PROPERTY, "target/it-jacoco.exec");

    assertThat(jacocoConfiguration.getItReportPath(), is("target/it-jacoco.exec"));
  }

  @Test
  public void shouldSetDestfile() {
    configuration.setProperty(JacocoConfiguration.REPORT_PATH_PROPERTY, "jacoco.exec");

    assertThat(jacocoConfiguration.getReportPath(), is("jacoco.exec"));
    assertThat(jacocoConfiguration.getJvmArgument(), is("-javaagent:jacocoagent.jar=destfile=jacoco.exec"));
  }

  @Test
  public void shouldSetIncludesAndExcludes() {
    configuration.setProperty(JacocoConfiguration.INCLUDES_PROPERTY, "org.sonar.*");
    configuration.setProperty(JacocoConfiguration.EXCLUDES_PROPERTY, "org.sonar.api.*");
    configuration.setProperty(JacocoConfiguration.EXCLCLASSLOADER_PROPERTY, "sun.reflect.DelegatingClassLoader");

    assertThat(jacocoConfiguration.getJvmArgument(),
        is("-javaagent:jacocoagent.jar=destfile=target/jacoco.exec,includes=org.sonar.*,excludes=org.sonar.api.*,exclclassloader=sun.reflect.DelegatingClassLoader"));
  }

}
