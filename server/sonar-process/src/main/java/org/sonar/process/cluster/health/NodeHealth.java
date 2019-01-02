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
package org.sonar.process.cluster.health;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * <p>{@link Externalizable} because this class is written to and from Hazelcast.</p>
 */
public class NodeHealth implements Externalizable {
  private Status status;
  private Set<String> causes;
  private NodeDetails details;

  /**
   * Required for Serialization
   */
  public NodeHealth() {
  }

  private NodeHealth(Builder builder) {
    this.status = builder.status;
    this.causes = ImmutableSet.copyOf(builder.causes);
    this.details = builder.details;
  }

  public static Builder newNodeHealthBuilder() {
    return new Builder();
  }

  public Status getStatus() {
    return status;
  }

  public Set<String> getCauses() {
    return causes;
  }

  public NodeDetails getDetails() {
    return details;
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(status.ordinal());
    out.writeInt(causes.size());
    for (String cause : causes) {
      out.writeUTF(cause);
    }
    out.writeObject(details);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    this.status = Status.values()[in.readInt()];
    int size = in.readInt();
    if (size > 0) {
      Set<String> readCauses = new HashSet<>(size);
      for (int i = 0; i < size; i++) {
        readCauses.add(in.readUTF());
      }
      this.causes = ImmutableSet.copyOf(readCauses);
    } else {
      this.causes = ImmutableSet.of();
    }
    this.details = (NodeDetails) in.readObject();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NodeHealth that = (NodeHealth) o;
    return status == that.status &&
      causes.equals(that.causes) &&
      details.equals(that.details);
  }

  @Override
  public int hashCode() {
    return Objects.hash(status, causes, details);
  }

  @Override
  public String toString() {
    return "NodeHealth{" +
      "status=" + status +
      ", causes=" + causes +
      ", details=" + details +
      '}';
  }

  public static class Builder {
    private static final String STATUS_CANT_BE_NULL = "status can't be null";
    private static final String DETAILS_CANT_BE_NULL = "details can't be null";

    private Status status;
    private Set<String> causes = new HashSet<>(0);
    private NodeDetails details;

    private Builder() {
      // use static factory method
    }

    public Builder setStatus(Status status) {
      this.status = requireNonNull(status, STATUS_CANT_BE_NULL);
      return this;
    }

    public Builder clearCauses() {
      this.causes.clear();
      return this;
    }

    public Builder addCause(String cause) {
      requireNonNull(cause, "cause can't be null");
      String trimmed = cause.trim();
      Preconditions.checkArgument(!trimmed.isEmpty(), "cause can't be empty");
      causes.add(cause);
      return this;
    }

    public Builder setDetails(NodeDetails details) {
      requireNonNull(details, DETAILS_CANT_BE_NULL);
      this.details = details;
      return this;
    }

    public NodeHealth build() {
      requireNonNull(status, STATUS_CANT_BE_NULL);
      requireNonNull(details, DETAILS_CANT_BE_NULL);
      return new NodeHealth(this);
    }
  }

  public enum Status {
    GREEN, YELLOW, RED
  }
}
