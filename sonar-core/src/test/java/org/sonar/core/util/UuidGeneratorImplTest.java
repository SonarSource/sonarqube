/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.core.util;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UuidGeneratorImplTest {
  private UuidGeneratorImpl underTest = new UuidGeneratorImpl();

  @Test
  public void generate_returns_unique_values_without_common_initial_letter_given_more_than_one_millisecond_between_generate_calls() throws InterruptedException {
    Base64.Encoder encoder = Base64.getEncoder();
    int count = 30;
    Set<String> uuids = new HashSet<>(count);
    for (int i = 0; i < count; i++) {
      Thread.sleep(5);
      uuids.add(encoder.encodeToString(underTest.generate()));
    }
    assertThat(uuids).hasSize(count);

    Iterator<String> iterator = uuids.iterator();
    String firstUuid = iterator.next();
    String base = firstUuid.substring(0, firstUuid.length() - 4);
    for (int i = 1; i < count; i++) {
      assertThat(iterator.next()).describedAs("i=" + i).doesNotStartWith(base);
    }
  }

  @Test
  public void generate_concurrent_test() throws InterruptedException {
    int rounds = 500;
    List<byte[]> uuids1 = new ArrayList<>(rounds);
    List<byte[]> uuids2 = new ArrayList<>(rounds);
    Thread t1 = new Thread(() -> {
      for (int i = 0; i < rounds; i++) {
        uuids1.add(underTest.generate());
      }
    });
    Thread t2 = new Thread(() -> {
      for (int i = 0; i < rounds; i++) {
        uuids2.add(underTest.generate());
      }
    });
    t1.start();
    t2.start();
    t1.join();
    t2.join();

    Base64.Encoder encoder = Base64.getEncoder();
    Set<String> uuids = new HashSet<>(rounds * 2);
    uuids1.forEach(bytes -> uuids.add(encoder.encodeToString(bytes)));
    uuids2.forEach(bytes -> uuids.add(encoder.encodeToString(bytes)));
    assertThat(uuids).hasSize(rounds * 2);
  }

  @Test
  public void generate_from_FixedBase_returns_unique_values_where_only_last_4_later_letter_change() {
    Base64.Encoder encoder = Base64.getEncoder();
    int count = 100_000;
    Set<String> uuids = new HashSet<>(count);

    UuidGenerator.WithFixedBase withFixedBase = underTest.withFixedBase();
    for (int i = 0; i < count; i++) {
      uuids.add(encoder.encodeToString(withFixedBase.generate(i)));
    }
    assertThat(uuids).hasSize(count);

    Iterator<String> iterator = uuids.iterator();
    String firstUuid = iterator.next();
    String base = firstUuid.substring(0, firstUuid.length() - 4);
    while (iterator.hasNext()) {
      assertThat(iterator.next()).startsWith(base);
    }
  }

  @Test
  public void generate_from_FixedBase_concurrent_test() throws InterruptedException {
    UuidGenerator.WithFixedBase withFixedBase = underTest.withFixedBase();
    int rounds = 500;
    List<byte[]> uuids1 = new ArrayList<>(rounds);
    List<byte[]> uuids2 = new ArrayList<>(rounds);
    AtomicInteger cnt = new AtomicInteger();
    Thread t1 = new Thread(() -> {
      for (int i = 0; i < rounds; i++) {
        uuids1.add(withFixedBase.generate(cnt.getAndIncrement()));
      }
    });
    Thread t2 = new Thread(() -> {
      for (int i = 0; i < rounds; i++) {
        uuids2.add(withFixedBase.generate(cnt.getAndIncrement()));
      }
    });
    t1.start();
    t2.start();
    t1.join();
    t2.join();

    Base64.Encoder encoder = Base64.getEncoder();
    Set<String> uuids = new HashSet<>(rounds * 2);
    uuids1.forEach(bytes -> uuids.add(encoder.encodeToString(bytes)));
    uuids2.forEach(bytes -> uuids.add(encoder.encodeToString(bytes)));
    assertThat(uuids).hasSize(rounds * 2);
  }
}
