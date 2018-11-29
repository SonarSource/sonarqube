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
package org.sonar.scanner.extension;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.stream.Stream;
import org.junit.Test;
import org.sonar.api.SonarRuntime;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.core.extension.CoreExtension;
import org.sonar.core.extension.CoreExtensionRepository;
import org.sonar.core.platform.ComponentContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.extension.CoreExtensionsInstaller.noAdditionalSideFilter;
import static org.sonar.core.extension.CoreExtensionsInstaller.noExtensionFilter;

public class ScannerCoreExtensionsInstallerTest {
  private SonarRuntime sonarRuntime = mock(SonarRuntime.class);
  private CoreExtensionRepository coreExtensionRepository = mock(CoreExtensionRepository.class);

  private ScannerCoreExtensionsInstaller underTest = new ScannerCoreExtensionsInstaller(sonarRuntime, coreExtensionRepository);

  @Test
  public void install_only_adds_ScannerSide_annotated_extension_to_container() {
    when(coreExtensionRepository.loadedCoreExtensions()).thenReturn(Stream.of(
      new CoreExtension() {
        @Override
        public String getName() {
          return "foo";
        }

        @Override
        public void load(Context context) {
          context.addExtensions(CeClass.class, ScannerClass.class, WebServerClass.class,
            NoAnnotationClass.class, OtherAnnotationClass.class, MultipleAnnotationClass.class);
        }
      }));
    ComponentContainer container = new ComponentContainer();

    underTest.install(container, noExtensionFilter(), noAdditionalSideFilter());

    assertThat(container.getPicoContainer().getComponentAdapters())
      .hasSize(ComponentContainer.COMPONENTS_IN_EMPTY_COMPONENT_CONTAINER + 2);
    assertThat(container.getComponentByType(ScannerClass.class)).isNotNull();
    assertThat(container.getComponentByType(MultipleAnnotationClass.class)).isNotNull();
  }

  @ComputeEngineSide
  public static final class CeClass {

  }

  @ServerSide
  public static final class WebServerClass {

  }

  @ScannerSide
  public static final class ScannerClass {

  }

  @ServerSide
  @ComputeEngineSide
  @ScannerSide
  public static final class MultipleAnnotationClass {

  }

  public static final class NoAnnotationClass {

  }

  @DarkSide
  public static final class OtherAnnotationClass {

  }

  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface DarkSide {
  }

}
