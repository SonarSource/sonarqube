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
package org.sonar.core.extension;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sonar.api.Property;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.core.platform.ExtensionContainer;
import org.sonar.core.platform.ListContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.extension.CoreExtensionsInstaller.noAdditionalSideFilter;
import static org.sonar.core.extension.CoreExtensionsInstaller.noExtensionFilter;

@RunWith(DataProviderRunner.class)
public class CoreExtensionsInstallerTest {
  private final SonarRuntime sonarRuntime = mock(SonarRuntime.class);
  private final CoreExtensionRepository coreExtensionRepository = mock(CoreExtensionRepository.class);
  private final CoreExtensionsInstaller underTest = new CoreExtensionsInstaller(sonarRuntime, coreExtensionRepository, WestSide.class) {

  };

  private final ArgumentCaptor<CoreExtension.Context> contextCaptor = ArgumentCaptor.forClass(CoreExtension.Context.class);
  private static int name_counter = 0;

  @Test
  public void install_has_no_effect_if_CoreExtensionRepository_has_no_loaded_CoreExtension() {
    ListContainer container = new ListContainer();
    underTest.install(container, noExtensionFilter(), noAdditionalSideFilter());
    assertAddedExtensions(container, 0);
  }

  @Test
  public void install_calls_load_method_on_all_loaded_CoreExtension() {
    CoreExtension coreExtension1 = newCoreExtension();
    CoreExtension coreExtension2 = newCoreExtension();
    CoreExtension coreExtension3 = newCoreExtension();
    CoreExtension coreExtension4 = newCoreExtension();
    List<CoreExtension> coreExtensions = ImmutableList.of(coreExtension1, coreExtension2, coreExtension3, coreExtension4);
    InOrder inOrder = Mockito.inOrder(coreExtension1, coreExtension2, coreExtension3, coreExtension4);
    when(coreExtensionRepository.loadedCoreExtensions()).thenReturn(coreExtensions.stream());
    ListContainer container = new ListContainer();

    underTest.install(container, noExtensionFilter(), noAdditionalSideFilter());

    inOrder.verify(coreExtension1).load(contextCaptor.capture());
    inOrder.verify(coreExtension2).load(contextCaptor.capture());
    inOrder.verify(coreExtension3).load(contextCaptor.capture());
    inOrder.verify(coreExtension4).load(contextCaptor.capture());
    // verify each core extension gets its own Context
    assertThat(contextCaptor.getAllValues())
      .hasSameElementsAs(ImmutableSet.copyOf(contextCaptor.getAllValues()));
  }

  @Test
  public void install_provides_runtime_from_constructor_in_context() {
    CoreExtension coreExtension1 = newCoreExtension();
    CoreExtension coreExtension2 = newCoreExtension();
    when(coreExtensionRepository.loadedCoreExtensions()).thenReturn(Stream.of(coreExtension1, coreExtension2));
    ListContainer container = new ListContainer();

    underTest.install(container, noExtensionFilter(), noAdditionalSideFilter());

    verify(coreExtension1).load(contextCaptor.capture());
    verify(coreExtension2).load(contextCaptor.capture());
    assertThat(contextCaptor.getAllValues())
      .extracting(CoreExtension.Context::getRuntime)
      .containsOnly(sonarRuntime);
  }

  @Test
  public void install_provides_new_Configuration_when_getBootConfiguration_is_called_and_there_is_none_in_container() {
    CoreExtension coreExtension1 = newCoreExtension();
    CoreExtension coreExtension2 = newCoreExtension();
    when(coreExtensionRepository.loadedCoreExtensions()).thenReturn(Stream.of(coreExtension1, coreExtension2));
    ListContainer container = new ListContainer();

    underTest.install(container, noExtensionFilter(), noAdditionalSideFilter());

    verify(coreExtension1).load(contextCaptor.capture());
    verify(coreExtension2).load(contextCaptor.capture());
    // verify each core extension gets its own configuration
    assertThat(contextCaptor.getAllValues())
      .hasSameElementsAs(ImmutableSet.copyOf(contextCaptor.getAllValues()));
  }

  @Test
  public void install_provides_Configuration_from_container_when_getBootConfiguration_is_called() {
    CoreExtension coreExtension1 = newCoreExtension();
    CoreExtension coreExtension2 = newCoreExtension();
    when(coreExtensionRepository.loadedCoreExtensions()).thenReturn(Stream.of(coreExtension1, coreExtension2));
    Configuration configuration = new MapSettings().asConfig();
    ExtensionContainer container = mock(ExtensionContainer.class);
    when(container.getComponentByType(Configuration.class)).thenReturn(configuration);
    underTest.install(container, noExtensionFilter(), noAdditionalSideFilter());

    verify(coreExtension1).load(contextCaptor.capture());
    verify(coreExtension2).load(contextCaptor.capture());
    assertThat(contextCaptor.getAllValues())
      .extracting(CoreExtension.Context::getBootConfiguration)
      .containsOnly(configuration);
  }

  @Test
  @UseDataProvider("allMethodsToAddExtension")
  public void install_installs_extensions_annotated_with_expected_annotation(BiConsumer<CoreExtension.Context, Collection<Object>> extensionAdder) {
    List<Object> extensions = ImmutableList.of(WestSideClass.class, EastSideClass.class, OtherSideClass.class, Latitude.class);
    CoreExtension coreExtension = newCoreExtension(context -> extensionAdder.accept(context, extensions));
    when(coreExtensionRepository.loadedCoreExtensions()).thenReturn(Stream.of(coreExtension));
    ExtensionContainer container = mock(ExtensionContainer.class);

    underTest.install(container, noExtensionFilter(), noAdditionalSideFilter());

    verify(container).addExtension(coreExtension.getName(), WestSideClass.class);
    verify(container).declareExtension(coreExtension.getName(), OtherSideClass.class);
    verify(container).declareExtension(coreExtension.getName(), EastSideClass.class);
    verify(container).addExtension(coreExtension.getName(), Latitude.class);
    verifyNoMoreInteractions(container);
  }

  @Test
  @UseDataProvider("allMethodsToAddExtension")
  public void install_does_not_install_extensions_annotated_with_expected_annotation_but_filtered_out(BiConsumer<CoreExtension.Context, Collection<Object>> extensionAdder) {
    List<Object> extensions = ImmutableList.of(WestSideClass.class, EastSideClass.class, OtherSideClass.class, Latitude.class);
    CoreExtension coreExtension = newCoreExtension(context -> extensionAdder.accept(context, extensions));
    when(coreExtensionRepository.loadedCoreExtensions()).thenReturn(Stream.of(coreExtension));
    ExtensionContainer container = mock(ExtensionContainer.class);

    underTest.install(container, noExtensionFilter(), t -> t != Latitude.class);

    verify(container).addExtension(coreExtension.getName(), WestSideClass.class);
    verify(container).declareExtension(coreExtension.getName(), OtherSideClass.class);
    verify(container).declareExtension(coreExtension.getName(), EastSideClass.class);
    verify(container).declareExtension(coreExtension.getName(), Latitude.class);
    verifyNoMoreInteractions(container);
  }

  @Test
  @UseDataProvider("allMethodsToAddExtension")
  public void install_adds_PropertyDefinition_with_extension_name_as_default_category(BiConsumer<CoreExtension.Context, Collection<Object>> extensionAdder) {
    PropertyDefinition propertyDefinitionNoCategory = PropertyDefinition.builder("fooKey").build();
    PropertyDefinition propertyDefinitionWithCategory = PropertyDefinition.builder("barKey").category("donut").build();
    List<Object> extensions = ImmutableList.of(propertyDefinitionNoCategory, propertyDefinitionWithCategory);
    CoreExtension coreExtension = newCoreExtension(context -> extensionAdder.accept(context, extensions));
    when(coreExtensionRepository.loadedCoreExtensions()).thenReturn(Stream.of(coreExtension));
    ExtensionContainer container = mock(ExtensionContainer.class);

    underTest.install(container, noExtensionFilter(), noAdditionalSideFilter());

    verify(container).declareExtension(coreExtension.getName(), propertyDefinitionNoCategory);
    verify(container).declareExtension(coreExtension.getName(), propertyDefinitionWithCategory);
    verifyNoMoreInteractions(container);
  }

  @DataProvider
  public static Object[][] allMethodsToAddExtension() {
    BiConsumer<CoreExtension.Context, Collection<Object>> addExtension = (context, objects) -> objects.forEach(context::addExtension);
    BiConsumer<CoreExtension.Context, Collection<Object>> addExtensionsVarArg = (context, objects) -> {
      if (objects.isEmpty()) {
        return;
      }
      if (objects.size() == 1) {
        context.addExtensions(objects.iterator().next());
      }
      context.addExtensions(objects.iterator().next(), objects.stream().skip(1).toArray(Object[]::new));
    };
    BiConsumer<CoreExtension.Context, Collection<Object>> addExtensions = CoreExtension.Context::addExtensions;
    return new Object[][] {
      {addExtension},
      {addExtensions},
      {addExtensionsVarArg}
    };
  }

  private static void assertAddedExtensions(ListContainer container, int addedExtensions) {
    List<Object> adapters = container.getAddedObjects();
    assertThat(adapters)
      .hasSize(addedExtensions);
  }

  private static CoreExtension newCoreExtension() {
    return newCoreExtension(t -> {
    });
  }

  private static CoreExtension newCoreExtension(Consumer<CoreExtension.Context> loadImplementation) {
    CoreExtension res = mock(CoreExtension.class);
    when(res.getName()).thenReturn("name_" + name_counter);
    name_counter++;
    doAnswer(invocation -> {
      CoreExtension.Context context = invocation.getArgument(0);
      loadImplementation.accept(context);
      return null;
    }).when(res).load(any());
    return res;
  }

  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface WestSide {
  }

  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface EastSide {
  }

  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface OtherSide {
  }

  @WestSide
  public static class WestSideClass {

  }

  @EastSide
  public static class EastSideClass {

  }

  @OtherSide
  public static class OtherSideClass {

  }

  @WestSide
  @EastSide
  public static class Latitude {

  }

  @Property(key = "eastKey", name = "eastName")
  @EastSide
  public static class EastSidePropertyDefinition {

  }

  @Property(key = "otherKey", name = "otherName")
  @OtherSide
  public static class OtherSidePropertyDefinition {

  }

  @Property(key = "blankKey", name = "blankName")
  public static class BlankPropertyDefinition {

  }

}
