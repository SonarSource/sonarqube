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

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.SourceType;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PmdTemplateTest {
  PMD pmd = mock(PMD.class);

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
}
