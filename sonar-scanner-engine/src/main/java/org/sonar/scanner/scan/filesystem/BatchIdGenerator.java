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
package org.sonar.scanner.scan.filesystem;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

import javax.annotation.concurrent.ThreadSafe;

import org.sonar.api.batch.fs.InputComponent;

/**
 * Generates unique IDs for any {@link InputComponent}. 
 * The IDs must be unique among all types of components and for all modules in the project.
 * The ID should never be 0, as it is sometimes used to indicate invalid components. 
 */
@ThreadSafe
public class BatchIdGenerator implements IntSupplier {
  private AtomicInteger nextBatchId = new AtomicInteger(1);

  @Override
  public int getAsInt() {
    return nextBatchId.getAndIncrement();
  }
}
