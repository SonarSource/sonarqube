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
package org.sonar.ce.task.projectanalysis.filemove;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.component.Component;

import static java.util.Objects.requireNonNull;

public interface MovedFilesRepository {
  /**
   * The original file for the specified component if it was registered as a moved file in the repository.
   * <p>
   * Calling this method with a Component which is not a file, will always return {@link Optional#empty()}.
   * </p>
   */
  Optional<OriginalFile> getOriginalFile(Component file);

  /**
   * The original file for the specified component if it was registered as a moved file inside the scope of a Pull Request.
   * <p>
   * Calling this method with a Component which is not a file, will always return {@link Optional#empty()}.
   * </p>
   */
  Optional<OriginalFile> getOriginalPullRequestFile(Component file);

  record OriginalFile(String uuid, String key) {
    public OriginalFile(String uuid, String key) {
      this.uuid = requireNonNull(uuid, "uuid can not be null");
      this.key = requireNonNull(key, "key can not be null");
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
        "uuid='" + uuid + '\'' +
        ", key='" + key + '\'' +
        '}';
    }
  }
}
