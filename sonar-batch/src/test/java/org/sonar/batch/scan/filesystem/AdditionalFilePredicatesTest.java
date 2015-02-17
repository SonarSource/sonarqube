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
package org.sonar.batch.scan.filesystem;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DeprecatedDefaultInputFile;

import static org.assertj.core.api.Assertions.assertThat;

public class AdditionalFilePredicatesTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void key() throws Exception {
    FilePredicate predicate = new AdditionalFilePredicates.KeyPredicate("struts:Action.java");

    DefaultInputFile inputFile = new DeprecatedDefaultInputFile("struts", "Action.java");
    assertThat(predicate.apply(inputFile)).isTrue();

    inputFile = new DeprecatedDefaultInputFile("struts", "Filter.java");
    assertThat(predicate.apply(inputFile)).isFalse();
  }
}
