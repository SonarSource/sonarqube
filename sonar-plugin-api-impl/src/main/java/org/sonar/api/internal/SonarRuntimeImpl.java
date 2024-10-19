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
package org.sonar.api.internal;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarProduct;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.utils.Version;

import static java.util.Objects.requireNonNull;
import static org.sonar.api.utils.Preconditions.checkArgument;

/**
 * @since 6.0
 */
@Immutable
public class SonarRuntimeImpl implements SonarRuntime {

  private final Version apiVersion;
  private final SonarProduct product;
  private final SonarQubeSide sonarQubeSide;
  private final SonarEdition edition;

  private SonarRuntimeImpl(Version apiVersion, SonarProduct product, @Nullable SonarQubeSide sonarQubeSide, @Nullable SonarEdition edition) {
    this.edition = edition;
    requireNonNull(product);
    checkArgument((product == SonarProduct.SONARQUBE) == (sonarQubeSide != null), "sonarQubeSide should be provided only for SonarQube product");
    checkArgument((product == SonarProduct.SONARQUBE) == (edition != null), "edition should be provided only for SonarQube product");
    this.apiVersion = requireNonNull(apiVersion);
    this.product = product;
    this.sonarQubeSide = sonarQubeSide;
  }

  @Override
  public Version getApiVersion() {
    return apiVersion;
  }

  @Override
  public SonarProduct getProduct() {
    return product;
  }

  @Override
  public SonarQubeSide getSonarQubeSide() {
    if (sonarQubeSide == null) {
      throw new UnsupportedOperationException("Can only be called in SonarQube");
    }
    return sonarQubeSide;
  }

  @Override
  public SonarEdition getEdition() {
    if (sonarQubeSide == null) {
      throw new UnsupportedOperationException("Can only be called in SonarQube");
    }
    return edition;
  }

  /**
   * Create an instance for SonarQube runtime environment.
   */
  public static SonarRuntime forSonarQube(Version apiVersion, SonarQubeSide side, SonarEdition edition) {
    return new SonarRuntimeImpl(apiVersion, SonarProduct.SONARQUBE, side, edition);
  }

  /**
   * Create an instance for SonarLint runtime environment.
   */
  public static SonarRuntime forSonarLint(Version version) {
    return new SonarRuntimeImpl(version, SonarProduct.SONARLINT, null, null);
  }

}
