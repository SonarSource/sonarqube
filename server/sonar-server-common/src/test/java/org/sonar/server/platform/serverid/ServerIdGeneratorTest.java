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
package org.sonar.server.platform.serverid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ServerIdGeneratorTest {
  private final ServerIdGenerator underTest = new ServerIdGenerator();

  @Test
  public void generate_concurrent_test() throws InterruptedException {
    int rounds = 500;
    List<String> ids1 = new ArrayList<>(rounds);
    List<String> ids2 = new ArrayList<>(rounds);
    Thread t1 = new Thread(() -> {
      for (int i = 0; i < rounds; i++) {
        ids1.add(underTest.generate());
      }
    });
    Thread t2 = new Thread(() -> {
      for (int i = 0; i < rounds; i++) {
        ids2.add(underTest.generate());
      }
    });
    t1.start();
    t2.start();
    t1.join();
    t2.join();

    Set<String> ids = new HashSet<>(rounds * 2);
    ids.addAll(ids1);
    ids.addAll(ids2);
    assertThat(ids).hasSize(rounds * 2);
  }

}
