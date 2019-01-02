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
package org.sonar.core.extension;

import com.google.common.collect.ImmutableSet;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class CoreExtensionRepositoryImplTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CoreExtensionRepositoryImpl underTest = new CoreExtensionRepositoryImpl();

  @Test
  public void loadedCoreExtensions_fails_with_ISE_if_called_before_setLoadedCoreExtensions() {
    expectRepositoryNotInitializedISE();

    underTest.loadedCoreExtensions();
  }

  @Test
  @UseDataProvider("coreExtensionsSets")
  public void loadedCoreExtensions_returns_CoreExtensions_from_setLoadedCoreExtensions(Set<CoreExtension> coreExtensions) {
    underTest.setLoadedCoreExtensions(coreExtensions);

    assertThat(underTest.loadedCoreExtensions().collect(Collectors.toSet()))
      .isEqualTo(coreExtensions);
  }

  @Test
  public void setLoadedCoreExtensions_fails_with_NPE_if_argument_is_null() {
    expectedException.expect(NullPointerException.class);

    underTest.setLoadedCoreExtensions(null);
  }

  @Test
  @UseDataProvider("coreExtensionsSets")
  public void setLoadedCoreExtensions_fails_with_ISE_if_called_twice(Set<CoreExtension> coreExtensions) {
    underTest.setLoadedCoreExtensions(coreExtensions);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Repository has already been initialized");

    underTest.setLoadedCoreExtensions(coreExtensions);
  }

  @Test
  public void installed_fails_with_ISE_if_called_before_setLoadedCoreExtensions() {
    CoreExtension coreExtension = newCoreExtension();

    expectRepositoryNotInitializedISE();

    underTest.installed(coreExtension);
  }

  @Test
  @UseDataProvider("coreExtensionsSets")
  public void installed_fails_with_IAE_if_CoreExtension_is_not_loaded(Set<CoreExtension> coreExtensions) {
    underTest.setLoadedCoreExtensions(coreExtensions);
    CoreExtension coreExtension = newCoreExtension();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Specified CoreExtension has not been loaded first");

    underTest.installed(coreExtension);
  }

  @Test
  @UseDataProvider("coreExtensionsSets")
  public void installed_fails_with_NPE_if_CoreExtension_is_null(Set<CoreExtension> coreExtensions) {
    underTest.setLoadedCoreExtensions(coreExtensions);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("coreExtension can't be null");

    underTest.installed(null);
  }

  @Test
  public void isInstalled_fails_with_ISE_if_called_before_setLoadedCoreExtensions() {
    expectRepositoryNotInitializedISE();

    underTest.isInstalled("foo");
  }

  @Test
  @UseDataProvider("coreExtensionsSets")
  public void isInstalled_returns_false_for_not_loaded_CoreExtension(Set<CoreExtension> coreExtensions) {
    underTest.setLoadedCoreExtensions(coreExtensions);

    assertThat(underTest.isInstalled("not loaded")).isFalse();
  }

  @Test
  public void isInstalled_returns_false_for_loaded_but_not_installed_CoreExtension() {
    CoreExtension coreExtension = newCoreExtension();
    underTest.setLoadedCoreExtensions(singleton(coreExtension));

    assertThat(underTest.isInstalled(coreExtension.getName())).isFalse();
  }

  @Test
  public void isInstalled_returns_true_for_loaded_and_installed_CoreExtension() {
    CoreExtension coreExtension = newCoreExtension();
    underTest.setLoadedCoreExtensions(singleton(coreExtension));
    underTest.installed(coreExtension);

    assertThat(underTest.isInstalled(coreExtension.getName())).isTrue();
  }

  private void expectRepositoryNotInitializedISE() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Repository has not been initialized yet");
  }

  @DataProvider
  public static Object[][] coreExtensionsSets() {
    return new Object[][] {
      {emptySet()},
      {singleton(newCoreExtension())},
      {ImmutableSet.of(newCoreExtension(), newCoreExtension())},
    };
  }

  private static int nameCounter = 0;

  private static CoreExtension newCoreExtension() {
    String name = "name_" + nameCounter;
    nameCounter++;
    return newCoreExtension(name);
  }

  private static CoreExtension newCoreExtension(String name) {
    return new CoreExtension() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      public void load(Context context) {
        throw new UnsupportedOperationException("load should not be called");
      }
    };
  }
}
