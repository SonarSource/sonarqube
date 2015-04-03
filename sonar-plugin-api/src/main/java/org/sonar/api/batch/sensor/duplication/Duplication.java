/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.batch.sensor.duplication;

import com.google.common.annotations.Beta;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.batch.sensor.SensorContext;

import java.util.List;

/**
 * <p/>
 * A {@link Duplication} is a list of duplicated {@link Block}s. One block
 * is considered as the original code and all others are duplicates.
 * Use {@link SensorContext#newDuplication()} to manually create a duplication. Use {@link SensorContext#duplicationTokenBuilder(org.sonar.api.batch.fs.InputFile)}
 * to feed tokens and let the core compute duplications.
 * @since 5.1
 */
@Beta
public interface Duplication {

  class Block {
    private final String resourceKey;
    private final int startLine;
    private final int length;

    public Block(String resourceKey, int startLine, int length) {
      this.resourceKey = resourceKey;
      this.startLine = startLine;
      this.length = length;
    }

    public String resourceKey() {
      return resourceKey;
    }

    public int startLine() {
      return startLine;
    }

    public int length() {
      return length;
    }

    // Just for unit tests
    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (obj == this) {
        return true;
      }
      if (obj.getClass() != getClass()) {
        return false;
      }
      Block rhs = (Block) obj;
      return new EqualsBuilder()
        .append(resourceKey, rhs.resourceKey)
        .append(startLine, rhs.startLine)
        .append(length, rhs.length).isEquals();
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder(13, 43)
        .append(resourceKey)
        .append(startLine)
        .append(length).toHashCode();
    }

    @Override
    public String toString() {
      return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).
        append("resourceKey", resourceKey).
        append("startLine", startLine).
        append("length", length).
        toString();
    }
  }

  Block originBlock();

  List<Block> duplicates();

}
