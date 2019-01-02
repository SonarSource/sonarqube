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
package org.sonar.ce.task.projectanalysis.filemove;

import com.google.common.base.Optional;
import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.Component;

import static java.util.Objects.requireNonNull;

public interface MovedFilesRepository {
  /**
   * The original file for the specified component if it was registered as a moved file in the repository.
   * <p>
   * Calling this method with a Component which is not a file, will always return {@link Optional#absent()}.
   * </p>
   */
  Optional<OriginalFile> getOriginalFile(Component file);

  final class OriginalFile {
    private final long id;
    private final String uuid;
    private final String key;

    public OriginalFile(long id, String uuid, String key) {
      this.id = id;
      this.uuid = requireNonNull(uuid, "uuid can not be null");
      this.key = requireNonNull(key, "key can not be null");
    }

    public long getId() {
      return id;
    }

    public String getUuid() {
      return uuid;
    }

    public String getKey() {
      return key;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      OriginalFile that = (OriginalFile) o;
      return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
      return uuid.hashCode();
    }

    @Override
    public String toString() {
      return "OriginalFile{" +
          "id=" + id +
          ", uuid='" + uuid + '\'' +
          ", key='" + key + '\'' +
          '}';
    }
  }
}
