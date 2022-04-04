/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.es;

import java.util.Collections;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.sonar.server.es.StartupIndexer.Type.SYNCHRONOUS;

public class StartupIndexerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private StartupIndexer underTest = () -> null;

  @Test
  public void getType() {
    Assertions.assertThat(underTest.getType()).isEqualTo(SYNCHRONOUS);
  }

  @Test
  public void triggerAsyncIndexOnStartup() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("ASYNCHRONE StartupIndexer must implement initAsyncIndexOnStartup");

    underTest.triggerAsyncIndexOnStartup(Collections.emptySet());
  }

  @Test
  public void indexOnStartup() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("SYNCHRONE StartupIndexer must implement indexOnStartup");

    underTest.indexOnStartup(Collections.emptySet());
  }

}
