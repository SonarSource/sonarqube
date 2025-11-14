/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.source.linereader;

import java.util.Optional;
import org.sonar.db.protobuf.DbFileSources;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

@FunctionalInterface
public interface LineReader {

  Optional<ReadError> read(DbFileSources.Line.Builder lineBuilder);

  enum Data {
    COVERAGE, DUPLICATION, HIGHLIGHTING, SCM, SYMBOLS
  }

  record ReadError(Data data, int line) {
    public ReadError {
      requireNonNull(data);
      checkArgument(line >= 0);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ReadError readError = (ReadError) o;
      return line == readError.line &&
        data == readError.data;
    }

    @Override
    public String toString() {
      return "ReadError{" +
        "data=" + data +
        ", line=" + line +
        '}';
    }
  }
}
