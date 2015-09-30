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
package org.sonar.home.cache;

import org.junit.Before;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class TTLCacheInvalidationTest {
  private Path testFile;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() throws IOException {
    testFile = temp.newFile().toPath();
  }

  @Test
  public void testExpired() throws IOException {
    TTLCacheInvalidation invalidation = new TTLCacheInvalidation(-100);
    assertThat(invalidation.test(testFile)).isEqualTo(true);
  }

  @Test
  public void testValid() throws IOException {
    TTLCacheInvalidation invalidation = new TTLCacheInvalidation(100_000);
    assertThat(invalidation.test(testFile)).isEqualTo(false);
  }
}
