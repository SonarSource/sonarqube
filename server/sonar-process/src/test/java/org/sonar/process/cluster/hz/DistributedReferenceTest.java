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
package org.sonar.process.cluster.hz;

import com.hazelcast.map.IMap;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DistributedReferenceTest {

  private final IMap<String, String> map = new MockIMap<>();

  @Test
  void set_whenValueIsNull_KeyValuePairIsRemoved() {
    DistributedReference<String> stringDistributedReference = new DistributedReference<>(map);

    stringDistributedReference.set("value");

    assertThat(stringDistributedReference.get()).isEqualTo("value");

    stringDistributedReference.set(null);

    assertThat(stringDistributedReference.get()).isNull();

  }

  @Test
  void get_returnsExpectedValue() {
    DistributedReference<String> stringDistributedReference = new DistributedReference<>(map);

    stringDistributedReference.set("value");

    assertThat(stringDistributedReference.get()).isEqualTo("value");
  }

  @Test
  void compareAndSet_whenExpectedValueMatches_newValueIsSet() {
    DistributedReference<String> stringDistributedReference = new DistributedReference<>(map);

    stringDistributedReference.set("oldValue");

    boolean result = stringDistributedReference.compareAndSet("oldValue", "newValue");

    assertThat(result).isTrue();
    assertThat(stringDistributedReference.get()).isEqualTo("newValue");
  }

  @Test
  void compareAndSet_whenExpectedValueMatches_newValueIsSetEvenIfNull() {
    DistributedReference<String> stringDistributedReference = new DistributedReference<>(map);

    stringDistributedReference.set("value");

    boolean result = stringDistributedReference.compareAndSet("value", null);

    assertThat(result).isTrue();
    assertThat(stringDistributedReference.get()).isNull();
  }

  @Test
  void compareAndSet_whenExpectedValueMatchesNull_newValueIsSet() {
    DistributedReference<String> stringDistributedReference = new DistributedReference<>(map);

    boolean result = stringDistributedReference.compareAndSet(null, "newValue");

    assertThat(result).isTrue();
    assertThat(stringDistributedReference.get()).isEqualTo("newValue");
  }

  @Test
  void compareAndSet_whenExpectedValueDoesNotMatchNull_newValueIsNotSet() {
    DistributedReference<String> stringDistributedReference = new DistributedReference<>(map);

    boolean result = stringDistributedReference.compareAndSet("oldValue", "newValue");

    assertThat(result).isFalse();
    assertThat(stringDistributedReference.get()).isNull();
  }

  @Test
  void compareAndSet_whenExpectedValueDoesNotMatch_previousValueIsKept() {
    DistributedReference<String> stringDistributedReference = new DistributedReference<>(map);

    stringDistributedReference.set("oldValue");

    boolean result = stringDistributedReference.compareAndSet("value", "newValue");

    assertThat(result).isFalse();
    assertThat(stringDistributedReference.get()).isEqualTo("oldValue");
  }


}

