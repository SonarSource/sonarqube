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
package org.sonar.api.batch.sensor.issue.internal;

import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.issue.fix.NewInputFileEdit;
import org.sonar.api.batch.sensor.issue.fix.NewTextEdit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class NoOpNewQuickFixTest {
  @Test
  public void newInputFileEdit_creates_no_ops() {
    NoOpNewQuickFix newQuickFix = new NoOpNewQuickFix();
    NewInputFileEdit newInputFileEdit = newQuickFix.newInputFileEdit();
    assertThat(newInputFileEdit).isInstanceOf(NoOpNewQuickFix.NoOpNewInputFileEdit.class);
    NewTextEdit newTextEdit = newInputFileEdit.newTextEdit();
    assertThat(newTextEdit).isInstanceOf(NoOpNewQuickFix.NoOpNewTextEdit.class);
  }

  @Test
  public void no_method_throws_exception() {
    NoOpNewQuickFix newQuickFix = new NoOpNewQuickFix();
    assertThat(newQuickFix.message("msg")).isEqualTo(newQuickFix);
    NewInputFileEdit newInputFileEdit = newQuickFix.newInputFileEdit();

    assertThat(newQuickFix.addInputFileEdit(newInputFileEdit)).isEqualTo(newQuickFix);
    assertThat(newInputFileEdit.on(mock(InputFile.class))).isEqualTo(newInputFileEdit);
    NewTextEdit newTextEdit = newInputFileEdit.newTextEdit();
    assertThat(newInputFileEdit.addTextEdit(newTextEdit)).isEqualTo(newInputFileEdit);
    assertThat(newTextEdit.at(mock(TextRange.class))).isEqualTo(newTextEdit);
    assertThat(newTextEdit.withNewText("text")).isEqualTo(newTextEdit);
  }
}
