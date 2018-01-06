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
package org.sonar.scanner.bootstrap;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.utils.TempFolder;
import org.sonar.core.util.UuidFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobalContainerTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private GlobalContainer createContainer(List<Object> extensions) {
    Map<String, String> props = ImmutableMap.of(CoreProperties.WORKING_DIRECTORY, temp.getRoot().getAbsolutePath(),
      CoreProperties.GLOBAL_WORKING_DIRECTORY, temp.getRoot().getAbsolutePath());

    GlobalContainer container = GlobalContainer.create(props, extensions);
    container.doBeforeStart();
    return container;
  }

  @Test
  public void should_add_components() {
    GlobalContainer container = createContainer(Collections.emptyList());

    assertThat(container.getComponentByType(UuidFactory.class)).isNotNull();
    assertThat(container.getComponentByType(TempFolder.class)).isNotNull();
  }

  @Test
  public void should_add_bootstrap_extensions() {
    GlobalContainer container = createContainer(Lists.newArrayList(Foo.class, new Bar()));

    assertThat(container.getComponentByType(Foo.class)).isNotNull();
    assertThat(container.getComponentByType(Bar.class)).isNotNull();
  }

  @Test
  public void shouldFormatTime() {
    assertThat(GlobalContainer.formatTime(1 * 60 * 60 * 1000 + 2 * 60 * 1000 + 3 * 1000 + 400)).isEqualTo("1:02:03.400 s");
    assertThat(GlobalContainer.formatTime(2 * 60 * 1000 + 3 * 1000 + 400)).isEqualTo("2:03.400 s");
    assertThat(GlobalContainer.formatTime(3 * 1000 + 400)).isEqualTo("3.400 s");
    assertThat(GlobalContainer.formatTime(400)).isEqualTo("0.400 s");
  }

  @BatchSide
  public static class Foo {

  }

  @BatchSide
  public static class Bar {

  }

}
