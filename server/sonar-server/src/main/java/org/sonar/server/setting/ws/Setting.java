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
package org.sonar.server.setting.ws;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableTable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.db.property.PropertyDto;

public class Setting {

  private static final Splitter DOT_SPLITTER = Splitter.on(".").omitEmptyStrings();

  private final String key;
  private final Long componentId;
  private final String value;
  private final PropertyDefinition definition;
  private final List<Map<String, String>> propertySets;
  private final boolean isDefault;

  private Setting(PropertyDto propertyDto, List<PropertyDto> propertyDtoSetValues, @Nullable PropertyDefinition definition) {
    this.key = propertyDto.getKey();
    this.value = propertyDto.getValue();
    this.componentId = propertyDto.getResourceId();
    this.definition = definition;
    this.propertySets = buildPropertySetValuesAsMap(key, propertyDtoSetValues);
    this.isDefault = false;
  }

  private Setting(PropertyDefinition definition) {
    this.key = definition.key();
    this.value = definition.defaultValue();
    this.componentId = null;
    this.definition = definition;
    this.propertySets = Collections.emptyList();
    this.isDefault = true;
  }

  public static Setting createFromDto(PropertyDto propertyDto, List<PropertyDto> propertyDtoSetValues, @Nullable PropertyDefinition definition){
    return new Setting(propertyDto, propertyDtoSetValues, definition);
  }

  public static Setting createFromDefinition(PropertyDefinition definition){
    return new Setting(definition);
  }

  public String getKey() {
    return key;
  }

  @CheckForNull
  public Long getComponentId() {
    return componentId;
  }

  @CheckForNull
  public PropertyDefinition getDefinition() {
    return definition;
  }

  public String getValue() {
    return value;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public List<Map<String, String>> getPropertySets() {
    return propertySets;
  }

  private static List<Map<String, String>> buildPropertySetValuesAsMap(String propertyKey, List<PropertyDto> propertySets) {
    if (propertySets.isEmpty()) {
      return Collections.emptyList();
    }
    ImmutableTable.Builder<String, String, String> tableBuilder = new ImmutableTable.Builder<>();
    propertySets.forEach(property -> {
      String keyWithoutSettingKey = property.getKey().replace(propertyKey + ".", "");
      List<String> setIdWithFieldKey = DOT_SPLITTER.splitToList(keyWithoutSettingKey);
      String setId = setIdWithFieldKey.get(0);
      String fieldKey = keyWithoutSettingKey.replaceFirst(setId + ".", "");
      tableBuilder.put(setId, fieldKey, property.getValue());
    });
    ImmutableTable<String, String, String> table = tableBuilder.build();
    return table.rowKeySet().stream()
      .map(table::row)
      .collect(Collectors.toList());
  }

}
