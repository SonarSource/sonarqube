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
package org.sonar.api.batch.fs.internal.predicates;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.sonar.api.batch.fs.FileSystem.Index;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.scan.filesystem.PathResolver;

/**
 * @since 6.6
 */
class URIPredicate extends AbstractFilePredicate {

  private final URI uri;
  private final Path baseDir;

  URIPredicate(URI uri, Path baseDir) {
    this.baseDir = baseDir;
    this.uri = uri;
  }

  @Override
  public boolean apply(InputFile f) {
    return uri.equals(f.uri());
  }

  @Override
  public Iterable<InputFile> get(Index index) {
    Path path = Paths.get(uri);
    Optional<String> relative = PathResolver.relativize(baseDir, path);
    if (!relative.isPresent()) {
      return Collections.emptyList();
    }
    InputFile f = index.inputFile(relative.get());
    return f != null ? Arrays.asList(f) : Collections.<InputFile>emptyList();
  }

  @Override
  public int priority() {
    return USE_INDEX;
  }
}
