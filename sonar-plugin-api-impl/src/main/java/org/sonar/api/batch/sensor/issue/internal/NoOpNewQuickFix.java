/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.api.batch.sensor.issue.internal;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.issue.fix.NewInputFileEdit;
import org.sonar.api.batch.sensor.issue.fix.NewQuickFix;
import org.sonar.api.batch.sensor.issue.fix.NewTextEdit;

public class NoOpNewQuickFix implements NewQuickFix {
  @Override
  public NewQuickFix message(String message) {
    return this;
  }

  @Override
  public NewInputFileEdit newInputFileEdit() {
    return new NoOpNewInputFileEdit();
  }

  @Override
  public NewQuickFix addInputFileEdit(NewInputFileEdit newInputFileEdit) {
    return this;
  }

  public static class NoOpNewInputFileEdit implements NewInputFileEdit {

    @Override
    public NewInputFileEdit on(InputFile inputFile) {
      return this;
    }

    @Override
    public NewTextEdit newTextEdit() {
      return new NoOpNewTextEdit();
    }

    @Override
    public NewInputFileEdit addTextEdit(NewTextEdit newTextEdit) {
      return this;
    }
  }

  public static class NoOpNewTextEdit implements NewTextEdit {
    @Override
    public NewTextEdit at(TextRange range) {
      return this;
    }

    @Override
    public NewTextEdit withNewText(String newText) {
      return this;
    }
  }
}
