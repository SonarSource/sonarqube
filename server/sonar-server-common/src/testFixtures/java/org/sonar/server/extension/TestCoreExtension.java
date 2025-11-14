/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.extension;

import java.util.stream.Stream;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.core.extension.CoreExtension;
import org.sonar.core.extension.PlatformLevel;
import org.sonar.db.MyBatisConfExtension;

public class TestCoreExtension implements CoreExtension {
  @Override
  public String getName() {
    return "mockCoreExtension";
  }

  @Override
  public void load(Context context) {
    context.addExtensions(
      TestBean1.class,
      TestBean2.class,
      TestBean3.class,
      TestBean4.class
    );
  }

  @PlatformLevel(1)
  @ComputeEngineSide
  @ServerSide
  public static class TestBean1 implements MyBatisConfExtension {
    public boolean called = false;

    @Override
    public Stream<Class<?>> getMapperClasses() {
      called = true;
      return Stream.empty();
    }
  }

  @PlatformLevel(2)
  @ComputeEngineSide
  @ServerSide
  public static class TestBean2 {
  }

  @PlatformLevel(3)
  @ComputeEngineSide
  @ServerSide
  public static class TestBean3 {
  }

  @PlatformLevel(4)
  @ComputeEngineSide
  @ServerSide
  public static class TestBean4 {
  }
}
