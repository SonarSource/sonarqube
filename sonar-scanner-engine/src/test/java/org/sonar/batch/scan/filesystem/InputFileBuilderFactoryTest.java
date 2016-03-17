/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.scan.filesystem;

import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.config.Settings;
import org.sonar.api.scan.filesystem.PathResolver;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class InputFileBuilderFactoryTest {
  @Test
  public void create_builder() {
    PathResolver pathResolver = new PathResolver();
    LanguageDetectionFactory langDetectionFactory = mock(LanguageDetectionFactory.class, Mockito.RETURNS_MOCKS);
    StatusDetectionFactory statusDetectionFactory = mock(StatusDetectionFactory.class, Mockito.RETURNS_MOCKS);
    DefaultModuleFileSystem fs = mock(DefaultModuleFileSystem.class);

    InputFileBuilderFactory factory = new InputFileBuilderFactory(ProjectDefinition.create().setKey("struts"), pathResolver, langDetectionFactory,
      statusDetectionFactory, new Settings(), new FileMetadata());
    InputFileBuilder builder = factory.create(fs);

    assertThat(builder.langDetection()).isNotNull();
    assertThat(builder.statusDetection()).isNotNull();
    assertThat(builder.pathResolver()).isSameAs(pathResolver);
    assertThat(builder.fs()).isSameAs(fs);
    assertThat(builder.moduleKey()).isEqualTo("struts");
  }
}
