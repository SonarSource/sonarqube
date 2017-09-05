/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.cluster.health;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

/**
 * <p>{@link Externalizable} because this class is written to and from Hazelcast.</p>
 */
public class NodeDetails implements Externalizable {
  private Type type;
  private String name;
  private String host;
  private int port;
  private long started;

  /**
   * Required for Serialization
   */
  public NodeDetails() {
  }

  private NodeDetails(Builder builder) {
    this.type = builder.type;
    this.name = builder.name;
    this.host = builder.host;
    this.port = builder.port;
    this.started = builder.started;
  }

  public static Builder newNodeDetailsBuilder() {
    return new Builder();
  }

  public Type getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public long getStarted() {
    return started;
  }

  @Override
  public String toString() {
    return "NodeDetails{" +
      "type=" + type +
      ", name='" + name + '\'' +
      ", host='" + host + '\'' +
      ", port=" + port +
      ", started=" + started +
      '}';
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(type.ordinal());
    out.writeUTF(name);
    out.writeUTF(host);
    out.writeInt(port);
    out.writeLong(started);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    this.type = Type.values()[in.readInt()];
    this.name = in.readUTF();
    this.host = in.readUTF();
    this.port = in.readInt();
    this.started = in.readLong();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NodeDetails that = (NodeDetails) o;
    return port == that.port &&
      started == that.started &&
      type == that.type &&
      name.equals(that.name) &&
      host.equals(that.host);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, name, host, port, started);
  }

  public static class Builder {
    private Type type;
    private String name;
    private String host;
    private int port;
    private long started;

    private Builder() {
      // use static factory method
    }

    public Builder setType(Type type) {
      this.type = checkType(type);
      return this;
    }

    public Builder setName(String name) {
      this.name = checkString(name, "name");
      return this;
    }

    public Builder setHost(String host) {
      this.host = checkString(host, "host");
      return this;
    }

    public Builder setPort(int port) {
      checkPort(port);
      this.port = port;
      return this;
    }

    public Builder setStarted(long started) {
      checkStarted(started);
      this.started = started;
      return this;
    }

    public NodeDetails build() {
      checkType(type);
      checkString(name, "name");
      checkString(host, "host");
      checkPort(port);
      checkStarted(started);
      return new NodeDetails(this);
    }

    private static Type checkType(Type type) {
      return requireNonNull(type, "type can't be null");
    }

    private static String checkString(String name, String label) {
      checkNotNull(name, "%s can't be null", label);
      String value = name.trim();
      checkArgument(!value.isEmpty(), "%s can't be empty", label);
      return value;
    }

    private static void checkPort(int port) {
      checkArgument(port > 0, "port must be > 0");
    }

    private static void checkStarted(long started) {
      checkArgument(started > 0, "started must be > 0");
    }
  }

  public enum Type {
    APPLICATION, SEARCH
  }
}
