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
package org.sonar.db.source;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

import static org.sonar.db.source.FileSourceDto.LINES_HASHES_SPLITTER;

public class LineHashesWithUuidDto {
  private String uuid;
  private String path;
  private String lineHashes;

  public String getUuid() {
    return uuid;
  }

  public String getPath() {
    return path;
  }

  /** Used by MyBatis */
  public String getRawLineHashes() {
    return lineHashes;
  }

  /** Used by MyBatis */
  public void setRawLineHashes(@Nullable String lineHashes) {
    this.lineHashes = lineHashes;
  }

  public List<String> getLineHashes() {
    if (lineHashes == null) {
      return Collections.emptyList();
    }
    return LINES_HASHES_SPLITTER.splitToList(lineHashes);
  }
}
