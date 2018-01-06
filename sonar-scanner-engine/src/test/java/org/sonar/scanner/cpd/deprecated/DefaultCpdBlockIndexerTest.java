/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.cpd.deprecated;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogTester;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultCpdBlockIndexerTest {

  private DefaultCpdBlockIndexer engine;
  private MapSettings settings;

  @Rule
  public LogTester logTester = new LogTester();

  @Before
  public void init() {
    settings = new MapSettings();
    engine = new DefaultCpdBlockIndexer(null, null, settings.asConfig(), null);
  }

  @Test
  public void shouldLogExclusions() {
    engine.logExclusions(new String[0]);
    assertThat(logTester.logs()).isEmpty();

    engine.logExclusions(new String[] {"Foo*", "**/Bar*"});

    String message = "Copy-paste detection exclusions:"
      + "\n  Foo*"
      + "\n  **/Bar*";
    assertThat(logTester.logs()).containsExactly(message);
  }

  @Test
  public void shouldReturnDefaultBlockSize() {
    assertThat(DefaultCpdBlockIndexer.getDefaultBlockSize("cobol")).isEqualTo(30);
    assertThat(DefaultCpdBlockIndexer.getDefaultBlockSize("abap")).isEqualTo(20);
    assertThat(DefaultCpdBlockIndexer.getDefaultBlockSize("other")).isEqualTo(10);
  }

  @Test
  public void defaultBlockSize() {

    assertThat(engine.getBlockSize("java")).isEqualTo(10);
  }

  @Test
  public void blockSizeForCobol() {
    settings.setProperty("sonar.cpd.cobol.minimumLines", "42");

    assertThat(engine.getBlockSize("cobol")).isEqualTo(42);
  }

}
