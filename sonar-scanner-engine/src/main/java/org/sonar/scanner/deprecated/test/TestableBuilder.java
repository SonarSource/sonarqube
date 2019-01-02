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
package org.sonar.scanner.deprecated.test;

import javax.annotation.CheckForNull;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputFile.Type;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.test.MutableTestable;
import org.sonar.scanner.deprecated.perspectives.PerspectiveBuilder;

public class TestableBuilder extends PerspectiveBuilder<MutableTestable> {

  public TestableBuilder() {
    super(MutableTestable.class);
  }

  @CheckForNull
  @Override
  public MutableTestable loadPerspective(Class<MutableTestable> perspectiveClass, InputComponent component) {
    if (component.isFile()) {
      InputFile inputFile = (InputFile) component;
      if (inputFile.type() == Type.MAIN) {
        return new DefaultTestable((DefaultInputFile) inputFile);
      }
    }
    return null;
  }
}
