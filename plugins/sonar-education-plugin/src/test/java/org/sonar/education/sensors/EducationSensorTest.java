/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.education.sensors;

import java.nio.charset.Charset;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.education.EducationRulesDefinition.EDUCATION_KEY;

public class EducationSensorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  protected final ActiveRules activeRules = mock(ActiveRules.class);

  @Before
  public void before() {
    when(activeRules.find(any())).thenReturn(mock(ActiveRule.class));
  }

  protected DefaultInputFile newTestFile(String content) {
    return new TestInputFileBuilder("foo", "hello.edu")
      .setLanguage(EDUCATION_KEY)
      .setContents(content)
      .setCharset(Charset.defaultCharset())
      .build();
  }
}
