/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrap;

import org.junit.Test;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.platform.ComponentContainer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AnalyseProjectModuleTest {
  @Test
  public void should_register_batch_extensions() {
    final ExtensionInstaller extensionInstaller = mock(ExtensionInstaller.class);
    Module bootstrapModule = new Module() {
      @Override
      protected void configure() {
        // used to install project extensions
        container.addSingleton(extensionInstaller);
      }
    };
    bootstrapModule.init();
    AnalyseProjectModule module = new AnalyseProjectModule(null);
    bootstrapModule.installChild(module);

    verify(extensionInstaller).installBatchExtensions(any(ComponentContainer.class), eq(InstantiationStrategy.PER_BATCH));
  }
}
