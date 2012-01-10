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
package org.sonar.plugins.cpd;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.CpdMapping;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Project;

public class PmdEngineTest {

  @Test
  public void shouldNotFailWhenNoMappings() {
    PmdEngine engine = new PmdEngine();
    assertThat(engine.isLanguageSupported(Java.INSTANCE), is(false));
  }

  @Test
  public void shouldCheckLanguageSupport() {
    CpdMapping mapping = mock(CpdMapping.class);
    when(mapping.getLanguage()).thenReturn(Java.INSTANCE);
    PmdEngine engine = new PmdEngine(new CpdMapping[] { mapping });
    assertThat(engine.isLanguageSupported(Java.INSTANCE), is(true));

    Language anotherLanguage = mock(Language.class);
    assertThat(engine.isLanguageSupported(anotherLanguage), is(false));
  }

  @Test
  public void defaultMinimumTokens() {
    Project project = createJavaProject().setConfiguration(new PropertiesConfiguration());

    PmdEngine engine = new PmdEngine();
    assertEquals(CoreProperties.CPD_MINIMUM_TOKENS_DEFAULT_VALUE, engine.getMinimumTokens(project));
  }

  @Test
  public void generalMinimumTokens() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty("sonar.cpd.minimumTokens", "33");
    Project project = createJavaProject().setConfiguration(conf);

    PmdEngine engine = new PmdEngine();
    assertEquals(33, engine.getMinimumTokens(project));
  }

  @Test
  public void minimumTokensByLanguage() {
    PropertiesConfiguration conf = new PropertiesConfiguration();
    conf.setProperty("sonar.cpd.minimumTokens", "100");
    conf.setProperty("sonar.cpd.php.minimumTokens", "33");

    Project phpProject = createPhpProject().setConfiguration(conf);
    Project javaProject = createJavaProject().setConfiguration(conf);

    PmdEngine engine = new PmdEngine();
    assertEquals(100, engine.getMinimumTokens(javaProject));
    assertEquals(33, engine.getMinimumTokens(phpProject));
  }

  private Project createJavaProject() {
    return new Project("java_project").setLanguageKey("java");
  }

  private Project createPhpProject() {
    return new Project("php_project").setLanguageKey("php");
  }

}
