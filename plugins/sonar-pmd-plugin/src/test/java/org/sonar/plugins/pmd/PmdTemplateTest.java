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

import com.google.common.base.Charsets;
import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDException;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.RuleSets;
import net.sourceforge.pmd.SourceType;
import org.junit.Test;
import org.sonar.api.resources.InputFile;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PmdTemplateTest {
  InputFile inputFile = mock(InputFile.class);
  RuleSets rulesets = mock(RuleSets.class);
  RuleContext ruleContext = mock(RuleContext.class);
  InputStream inputStream = mock(InputStream.class);
  PMD pmd = mock(PMD.class);

  @Test
  public void should_process_input_file() throws PMDException, FileNotFoundException {
    when(inputFile.getFile()).thenReturn(new File("source.java"));
    when(inputFile.getInputStream()).thenReturn(inputStream);

    new PmdTemplate(pmd).process(inputFile, Charsets.UTF_8, rulesets, ruleContext);

    verify(ruleContext).setSourceCodeFilename(new File("source.java").getAbsolutePath());
    verify(pmd).processFile(inputStream, Charsets.UTF_8.displayName(), rulesets, ruleContext);
  }

  @Test
  public void should_ignore_PMD_error() throws PMDException, FileNotFoundException {
    when(inputFile.getFile()).thenReturn(new File("source.java"));
    when(inputFile.getInputStream()).thenReturn(inputStream);
    doThrow(new PMDException("BUG")).when(pmd).processFile(inputStream, Charsets.UTF_8.displayName(), rulesets, ruleContext);

    new PmdTemplate(pmd).process(inputFile, Charsets.UTF_8, rulesets, ruleContext);
  }

  @Test
  public void should_set_java11_version() {
    PmdTemplate.setJavaVersion(pmd, "1.1");

    verify(pmd).setJavaVersion(SourceType.JAVA_13);
  }

  @Test
  public void should_set_java12_version() {
    PmdTemplate.setJavaVersion(pmd, "1.2");

    verify(pmd).setJavaVersion(SourceType.JAVA_13);
  }

  @Test
  public void should_set_java5_version() {
    PmdTemplate.setJavaVersion(pmd, "5");

    verify(pmd).setJavaVersion(SourceType.JAVA_15);
  }

  @Test
  public void should_set_java6_version() {
    PmdTemplate.setJavaVersion(pmd, "6");

    verify(pmd).setJavaVersion(SourceType.JAVA_16);
  }

  @Test(expected = SonarException.class)
  public void should_fail_on_invalid_java_version() {
    new PmdTemplate("12.2");
  }

  @Test
  public void shouldnt_fail_on_valid_java_version() {
    new PmdTemplate("6");
  }
}
