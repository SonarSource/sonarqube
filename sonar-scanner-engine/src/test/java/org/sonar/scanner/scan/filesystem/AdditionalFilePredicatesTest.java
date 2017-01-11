/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.scanner.scan.filesystem;

import org.junit.Test;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.IndexedFile;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.scanner.scan.filesystem.AdditionalFilePredicates;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;

public class AdditionalFilePredicatesTest {

  @Test
  public void key() {
    FilePredicate predicate = new AdditionalFilePredicates.KeyPredicate("struts:Action.java");

    IndexedFile indexedFile = new DefaultIndexedFile("struts", Paths.get("module"), "Action.java");
    assertThat(predicate.apply(indexedFile)).isTrue();

    indexedFile = new DefaultIndexedFile("struts", Paths.get("module"), "Filter.java");
    assertThat(predicate.apply(indexedFile)).isFalse();
  }
}
