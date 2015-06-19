/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.computation.qualitygate;

import com.google.common.base.Optional;
import javax.annotation.CheckForNull;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class QualityGateHolderImpl implements MutableQualityGateHolder {
  private boolean initialized = false;
  @CheckForNull
  private Optional<QualityGate> qualityGate;

  @Override
  public void setQualityGate(QualityGate qualityGate) {
    // fail fast
    requireNonNull(qualityGate);
    checkNotInitialized();

    this.initialized = true;
    this.qualityGate = of(qualityGate);
  }

  @Override
  public void setNoQualityGate() {
    checkNotInitialized();

    this.initialized = true;
    this.qualityGate = absent();
  }

  private void checkNotInitialized() {
    checkState(!initialized, "QualityGateHolder can be initialized only once");
  }

  @Override
  public Optional<QualityGate> getQualityGate() {
    checkState(initialized, "QualityGate has not been set yet");
    return qualityGate;
  }
}
