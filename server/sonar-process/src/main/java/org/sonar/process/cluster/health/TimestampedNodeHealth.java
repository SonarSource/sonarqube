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
package org.sonar.process.cluster.health;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

public final class TimestampedNodeHealth implements Externalizable {
  private NodeHealth nodeHealth;
  private long timestamp;

  public TimestampedNodeHealth() {
    // required by Externalizable
  }

  public TimestampedNodeHealth(NodeHealth nodeHealth, long timestamp) {
    this.nodeHealth = nodeHealth;
    this.timestamp = timestamp;
  }

  public NodeHealth getNodeHealth() {
    return nodeHealth;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeLong(timestamp);
    out.writeObject(nodeHealth);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    this.timestamp = in.readLong();
    this.nodeHealth = (NodeHealth) in.readObject();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TimestampedNodeHealth that = (TimestampedNodeHealth) o;
    return timestamp == that.timestamp &&
        Objects.equals(nodeHealth, that.nodeHealth);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodeHealth, timestamp);
  }

  @Override
  public String toString() {
    return nodeHealth + "@" + timestamp;
  }
}
