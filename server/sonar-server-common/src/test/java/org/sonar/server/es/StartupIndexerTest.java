/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.server.es.StartupIndexer.Type.SYNCHRONOUS;

public class StartupIndexerTest {
  private final StartupIndexer underTest = () -> null;

  @Test
  public void getType() {
    Assertions.assertThat(underTest.getType()).isEqualTo(SYNCHRONOUS);
  }

  @Test
  public void triggerAsyncIndexOnStartup() {
    assertThatThrownBy(() -> underTest.triggerAsyncIndexOnStartup(Collections.emptySet()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("ASYNCHRONOUS StartupIndexer must implement initAsyncIndexOnStartup");
  }

  @Test
  public void indexOnStartup() {
    assertThatThrownBy(() -> underTest.indexOnStartup(Collections.emptySet()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("SYNCHRONOUS StartupIndexer must implement indexOnStartup");
  }

}
