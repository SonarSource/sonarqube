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
package org.sonar.server.health;

import com.google.common.base.Strings;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.junit.Test;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.ThrowableAssert.ThrowingCallable;

public class HealthTest {

  private final Random random = new Random();
  private final Health.Status anyStatus = Health.Status.values()[random.nextInt(Health.Status.values().length)];
  private final Set<String> randomCauses = IntStream.range(0, random.nextInt(5)).mapToObj(s -> secure().nextAlphanumeric(3)).collect(Collectors.toSet());

  @Test
  public void build_throws_NPE_if_status_is_null() {
    Health.Builder builder = Health.builder();

    expectStatusNotNullNPE(() -> builder.build());
  }

  @Test
  public void setStatus_throws_NPE_if_status_is_null() {
    Health.Builder builder = Health.builder();

    expectStatusNotNullNPE(() -> builder.setStatus(null));
  }

  @Test
  public void getStatus_returns_status_from_builder() {
    Health underTest = Health.builder().setStatus(anyStatus).build();

    assertThat(underTest.getStatus()).isEqualTo(anyStatus);
  }

  @Test
  public void addCause_throws_NPE_if_arg_is_null() {
    Health.Builder builder = Health.builder();

    assertThatThrownBy(() -> builder.addCause(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessageContaining("cause can't be null");
  }

  @Test
  public void addCause_throws_IAE_if_arg_is_empty() {
    Health.Builder builder = Health.builder();

    expectCauseCannotBeEmptyIAE(() -> builder.addCause(""));
  }

  @Test
  public void addCause_throws_IAE_if_arg_contains_only_spaces() {
    Health.Builder builder = Health.builder();

    expectCauseCannotBeEmptyIAE(() -> builder.addCause(Strings.repeat(" ", 1 + random.nextInt(5))));
  }

  @Test
  public void getCause_returns_causes_from_builder() {
    Health.Builder builder = Health.builder().setStatus(anyStatus);
    randomCauses.forEach(builder::addCause);
    Health underTest = builder.build();

    assertThat(underTest.getCauses())
      .isEqualTo(randomCauses);
  }

  @Test
  public void green_constant() {
    assertThat(Health.builder().setStatus(Health.Status.GREEN).build()).isEqualTo(Health.GREEN);
  }

  @Test
  public void equals_is_based_on_status_and_causes() {
    Health.Builder builder1 = Health.builder();
    Health.Builder builder2 = Health.builder();

    builder1.setStatus(anyStatus);
    builder2.setStatus(anyStatus);
    randomCauses.forEach(s -> {
      builder1.addCause(s);
      builder2.addCause(s);
    });

    assertThat(builder1.build())
      .isEqualTo(builder1.build())
      .isEqualTo(builder2.build())
      .isEqualTo(builder2.build());
  }

  @Test
  public void not_equals_to_null_nor_other_type() {
    assertThat(Health.GREEN)
      .isNotNull()
      .isNotEqualTo(new Object())
      .isNotEqualTo(Health.Status.GREEN);
  }

  @Test
  public void hashcode_is_based_on_status_and_causes() {
    Health.Builder builder1 = Health.builder();
    Health.Builder builder2 = Health.builder();
    builder1.setStatus(anyStatus);
    builder2.setStatus(anyStatus);
    randomCauses.forEach(s -> {
      builder1.addCause(s);
      builder2.addCause(s);
    });

    assertThat(builder1.build().hashCode())
      .isEqualTo(builder1.build().hashCode())
      .isEqualTo(builder2.build().hashCode())
      .isEqualTo(builder2.build().hashCode());
  }

  @Test
  public void verify_toString() {
    assertThat(Health.GREEN).hasToString("Health{GREEN, causes=[]}");
    Health.Builder builder = Health.builder().setStatus(anyStatus);
    randomCauses.forEach(builder::addCause);

    String underTest = builder.build().toString();

    AbstractCharSequenceAssert<?, String> a = assertThat(underTest)
      .describedAs("toString for status %s and causes %s", anyStatus, randomCauses);
    if (randomCauses.isEmpty()) {
      a.isEqualTo("Health{" + anyStatus + ", causes=[]}");
    } else if (randomCauses.size() == 1) {
      a.isEqualTo("Health{" + anyStatus + ", causes=[" + randomCauses.iterator().next() + "]}");
    } else {
      a.startsWith("Health{" + anyStatus + ", causes=[")
        .endsWith("]}")
        .contains(randomCauses);
    }
  }

  private void expectStatusNotNullNPE(ThrowingCallable shouldRaiseThrowable) {
    assertThatThrownBy(shouldRaiseThrowable)
      .isInstanceOf(NullPointerException.class)
      .hasMessageContaining("status can't be null");
  }

  private void expectCauseCannotBeEmptyIAE(ThrowingCallable shouldRaiseThrowable) {
    assertThatThrownBy(shouldRaiseThrowable)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("cause can't be empty");
  }
}
