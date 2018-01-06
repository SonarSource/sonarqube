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
package org.sonar.server.health;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.assertj.core.api.AbstractAssert;
import org.sonar.process.cluster.health.NodeHealth;

final class HealthAssert extends AbstractAssert<HealthAssert, Health> {
  private Set<NodeHealth> nodeHealths;

  private HealthAssert(Health actual) {
    super(actual, HealthAssert.class);
  }

  public static HealthAssert assertThat(Health actual) {
    return new HealthAssert(actual);
  }

  public HealthAssert forInput(Set<NodeHealth> nodeHealths) {
    this.nodeHealths = nodeHealths;

    return this;
  }

  public HealthAssert hasStatus(Health.Status expected) {
    isNotNull();

    if (actual.getStatus() != expected) {
      failWithMessage(
        "Expected Status of Health to be <%s> but was <%s> for NodeHealth \n%s",
        expected,
        actual.getStatus(),
        printStatusesAndTypes(this.nodeHealths));
    }

    return this;
  }

  public HealthAssert andCauses(String... causes) {
    isNotNull();

    if (!checkCauses(causes)) {
      failWithMessage(
        "Expected causes of Health to contain only \n%s\n but was \n%s\n for NodeHealth \n%s",
        Arrays.asList(causes),
        actual.getCauses(),
        printStatusesAndTypes(this.nodeHealths));
    }

    return this;
  }

  private String printStatusesAndTypes(@Nullable Set<NodeHealth> nodeHealths) {
    if (nodeHealths == null) {
      return "<null>";
    }
    return nodeHealths.stream()
      // sort by type then status for debugging convenience
      .sorted(Comparator.<NodeHealth>comparingInt(s1 -> s1.getDetails().getType().ordinal())
        .thenComparingInt(s -> s.getStatus().ordinal()))
      .map(s -> ImmutableList.of(s.getDetails().getType().name(), s.getStatus().name()))
      .map(String::valueOf)
      .collect(Collectors.joining(","));
  }

  private boolean checkCauses(String... causes) {
    if (causes.length != this.actual.getCauses().size()) {
      return false;
    }
    return Objects.equals(new HashSet<>(Arrays.asList(causes)), this.actual.getCauses());
  }
}
