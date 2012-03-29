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
package org.sonar.plugins.pmd;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.XMLRuleParser;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PmdRuleRepositoryTest {

  @Test
  public void testLoadRepositoryFromXml() {
    ServerFileSystem fileSystem = mock(ServerFileSystem.class);
    PmdRuleRepository repository = new PmdRuleRepository(fileSystem, new XMLRuleParser());
    List<Rule> rules = repository.createRules();
    assertThat(rules.size(), greaterThan(100));
  }

  @Test
  public void shouldLoadExtensions() {
    ServerFileSystem fileSystem = mock(ServerFileSystem.class);
    File file = FileUtils.toFile(getClass().getResource("/org/sonar/plugins/pmd/rules-extension.xml"));
    when(fileSystem.getExtensions("pmd", "xml")).thenReturn(Collections.singletonList(file));
    PmdRuleRepository repository = new PmdRuleRepository(fileSystem, new XMLRuleParser());
    List<Rule> rules = repository.createRules();
    assertThat(rules.size(), greaterThan(100));
    assertThat(rules.get(rules.size() - 1).getKey(), is("Extension"));
  }

}
