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

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class Health {
  /**
   * The GREEN status without any cause as a constant, for convenience and optimisation.
   */
  public static final Health GREEN = newHealthCheckBuilder()
    .setStatus(Status.GREEN)
    .build();

  private final Status status;
  private final Set<String> causes;

  public Health(Builder builder) {
    this.status = builder.status;
    this.causes = ImmutableSet.copyOf(builder.causes);
  }

  public Status getStatus() {
    return status;
  }

  public Set<String> getCauses() {
    return causes;
  }

  public static Builder newHealthCheckBuilder() {
    return new Builder();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Health health = (Health) o;
    return status == health.status &&
      Objects.equals(causes, health.causes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, causes);
  }

  @Override
  public String toString() {
    return "Health{" + status +
      ", causes=" + causes +
      '}';
  }

  /**
   * Builder of {@link Health} which supports being reused for optimization.
   */
  public static class Builder {
    private Status status;
    private Set<String> causes = new HashSet<>(0);

    private Builder() {
      // use static factory method
    }

    public Builder clear() {
      this.status = null;
      this.causes.clear();
      return this;
    }

    public Builder setStatus(Status status) {
      this.status = checkStatus(status);
      return this;
    }

    public Builder addCause(String cause) {
      requireNonNull(cause, "cause can't be null");
      checkArgument(!cause.trim().isEmpty(), "cause can't be empty");
      causes.add(cause);
      return this;
    }

    public Health build() {
      checkStatus(this.status);
      return new Health(this);
    }

    private static Status checkStatus(Status status) {
      return requireNonNull(status, "status can't be null");
    }
  }

  public enum Status {
    /**
     * Fully working
     */
    GREEN,
    /**
     * Yellow: Working but something must be fixed to make SQ fully operational
     */
    YELLOW,
    /**
     * Red: Not working
     */
    RED
  }
}
