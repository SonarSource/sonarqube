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
import org.picocontainer.ComponentAdapter;
import org.sonar.api.ExtensionProvider;
import org.sonar.api.Property;
import org.sonar.api.SonarRuntime;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.core.platform.ComponentContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.core.extension.CoreExtensionsInstaller.noAdditionalSideFilter;
import static org.sonar.core.extension.CoreExtensionsInstaller.noExtensionFilter;
import static org.sonar.core.platform.ComponentContainer.COMPONENTS_IN_EMPTY_COMPONENT_CONTAINER;

@RunWith(DataProviderRunner.class)
public class CoreExtensionsInstallerTest {
  private SonarRuntime sonarRuntime = mock(SonarRuntime.class);
  private CoreExtensionRepository coreExtensionRepository = mock(CoreExtensionRepository.class);
  private CoreExtensionsInstaller underTest = new CoreExtensionsInstaller(sonarRuntime, coreExtensionRepository, WestSide.class) {

  };

  private ArgumentCaptor<CoreExtension.Context> contextCaptor = ArgumentCaptor.forClass(CoreExtension.Context.class);
  private static int name_counter = 0;

  @Test
  public void install_has_no_effect_if_CoreExtensionRepository_has_no_loaded_CoreExtension() {
    ComponentContainer container = new ComponentContainer();

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
    ComponentContainer container = new ComponentContainer();

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
    ComponentContainer container = new ComponentContainer();

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
    ComponentContainer container = new ComponentContainer();

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
    ComponentContainer container = new ComponentContainer();
    container.add(configuration);

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
    ComponentContainer container = new ComponentContainer();

    underTest.install(container, noExtensionFilter(), noAdditionalSideFilter());

    assertAddedExtensions(container, WestSideClass.class, Latitude.class);
    assertPropertyDefinitions(container);
  }

  @Test
  @UseDataProvider("allMethodsToAddExtension")
  public void install_does_not_install_extensions_annotated_with_expected_annotation_but_filtered_out(BiConsumer<CoreExtension.Context, Collection<Object>> extensionAdder) {
    List<Object> extensions = ImmutableList.of(WestSideClass.class, EastSideClass.class, OtherSideClass.class, Latitude.class);
    CoreExtension coreExtension = newCoreExtension(context -> extensionAdder.accept(context, extensions));
    when(coreExtensionRepository.loadedCoreExtensions()).thenReturn(Stream.of(coreExtension));
    ComponentContainer container = new ComponentContainer();

    underTest.install(container, noExtensionFilter(), t -> t != Latitude.class);

    assertAddedExtensions(container, WestSideClass.class);
    assertPropertyDefinitions(container);
  }

  @Test
  @UseDataProvider("allMethodsToAddExtension")
  public void install_adds_PropertyDefinition_from_annotation_no_matter_annotations(BiConsumer<CoreExtension.Context, Collection<Object>> extensionAdder) {
    List<Object> extensions = ImmutableList.of(WestSidePropertyDefinition.class, EastSidePropertyDefinition.class,
      OtherSidePropertyDefinition.class, LatitudePropertyDefinition.class, BlankPropertyDefinition.class);
    CoreExtension coreExtension = newCoreExtension(context -> extensionAdder.accept(context, extensions));
    when(coreExtensionRepository.loadedCoreExtensions()).thenReturn(Stream.of(coreExtension));
    ComponentContainer container = new ComponentContainer();

    underTest.install(container, noExtensionFilter(), noAdditionalSideFilter());

    assertAddedExtensions(container, WestSidePropertyDefinition.class, LatitudePropertyDefinition.class);
    assertPropertyDefinitions(container, "westKey", "eastKey", "otherKey", "latitudeKey", "blankKey");
  }

  @Test
  @UseDataProvider("allMethodsToAddExtension")
  public void install_adds_PropertyDefinition_from_annotation_no_matter_annotations_even_if_filtered_out(BiConsumer<CoreExtension.Context, Collection<Object>> extensionAdder) {
    List<Object> extensions = ImmutableList.of(WestSidePropertyDefinition.class, EastSidePropertyDefinition.class,
      OtherSidePropertyDefinition.class, LatitudePropertyDefinition.class, BlankPropertyDefinition.class);
    CoreExtension coreExtension = newCoreExtension(context -> extensionAdder.accept(context, extensions));
    when(coreExtensionRepository.loadedCoreExtensions()).thenReturn(Stream.of(coreExtension));
    ComponentContainer container = new ComponentContainer();

    underTest.install(container, noExtensionFilter(), t -> false);

    assertAddedExtensions(container, 0);
    assertPropertyDefinitions(container, "westKey", "eastKey", "otherKey", "latitudeKey", "blankKey");
  }

  @Test
  @UseDataProvider("allMethodsToAddExtension")
  public void install_adds_PropertyDefinition_with_extension_name_as_default_category(BiConsumer<CoreExtension.Context, Collection<Object>> extensionAdder) {
    PropertyDefinition propertyDefinitionNoCategory = PropertyDefinition.builder("fooKey").build();
    PropertyDefinition propertyDefinitionWithCategory = PropertyDefinition.builder("barKey").category("donut").build();
    List<Object> extensions = ImmutableList.of(propertyDefinitionNoCategory, propertyDefinitionWithCategory);
    CoreExtension coreExtension = newCoreExtension(context -> extensionAdder.accept(context, extensions));
    when(coreExtensionRepository.loadedCoreExtensions()).thenReturn(Stream.of(coreExtension));
    ComponentContainer container = new ComponentContainer();

    underTest.install(container, noExtensionFilter(), noAdditionalSideFilter());

    assertAddedExtensions(container, 0);
    assertPropertyDefinitions(container, coreExtension, propertyDefinitionNoCategory, propertyDefinitionWithCategory);
  }

  @Test
  @UseDataProvider("allMethodsToAddExtension")
  public void install_adds_providers_to_container_and_install_extensions_they_provide_when_annotated_with_expected_annotation(BiConsumer<CoreExtension.Context, Collection<Object>> extensionAdder) {
    List<Object> extensions = ImmutableList.of(WestSideProvider.class, PartiallyWestSideProvider.class, EastSideProvider.class);
    CoreExtension coreExtension = newCoreExtension(context -> extensionAdder.accept(context, extensions));
    when(coreExtensionRepository.loadedCoreExtensions()).thenReturn(Stream.of(coreExtension));
    ComponentContainer container = new ComponentContainer();

    underTest.install(container, noExtensionFilter(), noAdditionalSideFilter());

    assertAddedExtensions(container, WestSideProvider.class, WestSideProvided.class, PartiallyWestSideProvider.class);
    assertPropertyDefinitions(container);
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

  private static void assertAddedExtensions(ComponentContainer container, int addedExtensions) {
    Collection<ComponentAdapter<?>> adapters = container.getPicoContainer().getComponentAdapters();
    assertThat(adapters)
      .hasSize(COMPONENTS_IN_EMPTY_COMPONENT_CONTAINER + addedExtensions);
  }

  private static void assertAddedExtensions(ComponentContainer container, Class... classes) {
    Collection<ComponentAdapter<?>> adapters = container.getPicoContainer().getComponentAdapters();
    assertThat(adapters)
      .hasSize(COMPONENTS_IN_EMPTY_COMPONENT_CONTAINER + classes.length);

    Stream<Class> installedExtensions = adapters.stream()
      .map(t -> (Class) t.getComponentImplementation())
      .filter(t -> !PropertyDefinitions.class.isAssignableFrom(t) && t != ComponentContainer.class);
    assertThat(installedExtensions)
      .contains(classes)
      .hasSize(classes.length);
  }

  private void assertPropertyDefinitions(ComponentContainer container, String... keys) {
    PropertyDefinitions propertyDefinitions = container.getComponentByType(PropertyDefinitions.class);
    if (keys.length == 0) {
      assertThat(propertyDefinitions.getAll()).isEmpty();
    } else {
      for (String key : keys) {
        assertThat(propertyDefinitions.get(key)).isNotNull();
      }
    }
  }

  private void assertPropertyDefinitions(ComponentContainer container, CoreExtension coreExtension, PropertyDefinition... definitions) {
    PropertyDefinitions propertyDefinitions = container.getComponentByType(PropertyDefinitions.class);
    if (definitions.length == 0) {
      assertThat(propertyDefinitions.getAll()).isEmpty();
    } else {
      for (PropertyDefinition definition : definitions) {
        PropertyDefinition actual = propertyDefinitions.get(definition.key());
        assertThat(actual.category()).isEqualTo(definition.category() == null ? coreExtension.getName() : definition.category());
      }
    }
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

  @WestSide
  public static class WestSideProvider extends ExtensionProvider {

    @Override
    public Object provide() {
      return WestSideProvided.class;
    }
  }

  @WestSide
  public static class WestSideProvided {

  }

  @WestSide
  public static class PartiallyWestSideProvider extends ExtensionProvider {

    @Override
    public Object provide() {
      return NotWestSideProvided.class;
    }
  }

  public static class NotWestSideProvided {

  }

  @EastSide
  public static class EastSideProvider extends ExtensionProvider {

    @Override
    public Object provide() {
      throw new IllegalStateException("EastSideProvider#provide should not be called");
    }
  }

  @Property(key = "westKey", name = "westName")
  @WestSide
  public static class WestSidePropertyDefinition {

  }

  @Property(key = "eastKey", name = "eastName")
  @EastSide
  public static class EastSidePropertyDefinition {

  }

  @Property(key = "otherKey", name = "otherName")
  @OtherSide
  public static class OtherSidePropertyDefinition {

  }

  @Property(key = "latitudeKey", name = "latitudeName")
  @WestSide
  @EastSide
  public static class LatitudePropertyDefinition {

  }

  @Property(key = "blankKey", name = "blankName")
  public static class BlankPropertyDefinition {

  }

}
