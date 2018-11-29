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
package org.sonar.scanner.phases;

import javax.annotation.concurrent.Immutable;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;

import static org.sonar.api.config.internal.MultivalueProperty.parseAsCsv;

@Immutable
public class ModuleCoverageExclusions extends AbstractCoverageExclusions {

  public ModuleCoverageExclusions(DefaultInputModule module) {
    super(k -> {
      String value = module.properties().get(k);
      return value != null ? parseAsCsv(k, value) : new String[0];
    }, DefaultInputFile::getModuleRelativePath);
  }
}
