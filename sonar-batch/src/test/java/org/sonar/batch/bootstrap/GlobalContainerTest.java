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
package org.sonar.batch.bootstrap;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.BatchExtension;
import org.sonar.api.utils.TempFolder;
import org.sonar.core.config.Logback;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobalContainerTest {
  @Test
  public void should_add_components() {
    GlobalContainer container = GlobalContainer.create(Collections.<String, String>emptyMap(), Collections.emptyList());
    container.doBeforeStart();

    assertThat(container.getComponentByType(Logback.class)).isNotNull();
    assertThat(container.getComponentByType(TempFolder.class)).isNotNull();
  }

  @Test
  public void should_add_bootstrap_extensions() {
    GlobalContainer container = GlobalContainer.create(Collections.<String, String>emptyMap(), Lists.newArrayList(Foo.class, new Bar()));
    container.doBeforeStart();

    assertThat(container.getComponentByType(Foo.class)).isNotNull();
    assertThat(container.getComponentByType(Bar.class)).isNotNull();
  }

  public static class Foo implements BatchExtension {

  }

  public static class Bar implements BatchExtension {

  }

}
