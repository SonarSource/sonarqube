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
package org.sonar.server.l18n;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
import org.sonar.core.i18n.DefaultI18n;
import org.sonar.core.platform.PluginRepository;
import org.sonar.core.extension.CoreExtension;
import org.sonar.core.extension.CoreExtensionRepository;

/**
 * Subclass of {@link DefaultI18n} which supports Core Extensions.
 */
@ServerSide
@ComputeEngineSide
public class ServerI18n extends DefaultI18n {
  private final CoreExtensionRepository coreExtensionRepository;

  public ServerI18n(PluginRepository pluginRepository, System2 system2, CoreExtensionRepository coreExtensionRepository) {
    super(pluginRepository, system2);
    this.coreExtensionRepository = coreExtensionRepository;
  }

  @Override
  protected void initialize() {
    super.initialize();

    coreExtensionRepository.loadedCoreExtensions()
      .map(CoreExtension::getName)
      .forEach(this::initPlugin);
  }

  @VisibleForTesting
  @Override
  protected void doStart(ClassLoader classloader) {
    super.doStart(classloader);
  }
}
