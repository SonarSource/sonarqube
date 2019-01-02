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
package org.sonar.duplications.block;

import java.util.List;

/**
 * Represents all blocks in a file.
 */
public final class FileBlocks {

  private final String resourceId;
  private final List<Block> blocks;

  public FileBlocks(String resourceId, List<Block> blocks) {
    this.resourceId = resourceId;
    this.blocks = blocks;
  }

  public String resourceId() {
    return resourceId;
  }

  public List<Block> blocks() {
    return blocks;
  }

}
