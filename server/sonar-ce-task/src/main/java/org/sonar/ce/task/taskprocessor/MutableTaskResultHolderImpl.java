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
package org.sonar.ce.task.taskprocessor;

import javax.annotation.CheckForNull;
import org.sonar.ce.task.CeTaskResult;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class MutableTaskResultHolderImpl implements MutableTaskResultHolder {
  @CheckForNull
  private CeTaskResult result;

  @Override
  public CeTaskResult getResult() {
    checkState(this.result != null, "No CeTaskResult has been set in the holder");
    return this.result;
  }

  @Override
  public void setResult(CeTaskResult taskResult) {
    requireNonNull(taskResult, "taskResult can not be null");
    checkState(this.result == null, "CeTaskResult has already been set in the holder");
    this.result = taskResult;
  }
}
