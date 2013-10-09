/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.scan.filesystem;

import org.sonar.api.BatchComponent;

import javax.annotation.CheckForNull;
import java.io.File;
import java.nio.charset.Charset;

/**
 * Facade for local and remote file hashes
 */
public class FileHashes implements BatchComponent {

  private final RemoteFileHashes remoteFileHashes;

  public FileHashes(RemoteFileHashes remoteFileHashes) {
    this.remoteFileHashes = remoteFileHashes;
  }

  @CheckForNull
  public String hash(File file, Charset charset) {
    return FileHashDigest.INSTANCE.hash(file, charset);
  }

  @CheckForNull
  public String remoteHash(String baseRelativePath) {
    return remoteFileHashes.remoteHash(baseRelativePath);
  }
}
