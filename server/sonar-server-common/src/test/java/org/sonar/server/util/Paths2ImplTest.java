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
package org.sonar.server.util;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.net.URI;
import java.nio.file.Paths;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static java.nio.file.Path.of;

@RunWith(DataProviderRunner.class)
public class Paths2ImplTest {
  @Test
  public void getInstance_returns_the_same_object_for_every_call() {
    assertThat(Paths2Impl.getInstance())
      .isSameAs(Paths2Impl.getInstance())
      .isSameAs(Paths2Impl.getInstance());
  }

  @Test
  @UseDataProvider("getStringParameters")
  public void get_String_returns_result_of_Paths_get(String first, String... others) {
    assertThat(Paths2Impl.getInstance().get(first, others))
      .isEqualTo(Paths.get(first, others));
  }

  @DataProvider
  public static Object[][] getStringParameters() {
    return new Object[][] {
      {"a", new String[] {}},
      {"a", new String[] {"b"}},
      {"a", new String[] {"b", "c"}}
    };
  }

  @Test
  @UseDataProvider("getURIParameter")
  public void get_URI_returns_result_of_Paths_get(URI uri) {
    assertThat(Paths2Impl.getInstance().get(uri))
      .isEqualTo(Paths.get(uri));
  }

  @DataProvider
  public static Object[][] getURIParameter() {
    return new Object[][] {
      {URI.create("file:///")},
      {URI.create("file:///a")},
      {URI.create("file:///b/c")}
    };
  }

  @Test
  @UseDataProvider("getStringParameters")
  public void exists_returns_result_of_Paths_exists(String first, String... others) {
    assertThat(Paths2Impl.getInstance().exists(first, others))
      .isEqualTo(of(first, others).toFile().exists());
  }
}
