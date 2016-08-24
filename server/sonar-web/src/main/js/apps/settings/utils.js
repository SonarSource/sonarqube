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
import uniqueId from 'lodash/uniqueId';
import { translate, hasMessage } from '../../helpers/l10n';

export const DEFAULT_CATEGORY = 'general';

export function getPropertyName (definition) {
  const key = `property.${definition.key}.name`;
  return hasMessage(key) ? translate(key) : definition.name;
}

export function getPropertyDescription (definition) {
  const key = `property.${definition.key}.description`;
  return hasMessage(key) ? translate(key) : definition.description;
}

export function getCategoryName (category) {
  const key = `property.category.${category}`;
  return hasMessage(key) ? translate(key) : category;
}

export function getSubCategoryName (category, subCategory) {
  const key = `property.category.${category}.${subCategory}`;
  return hasMessage(key) ? translate(key) : getCategoryName(subCategory);
}

export function getSubCategoryDescription (category, subCategory) {
  const key = `property.category.${category}.${subCategory}.description`;
  return hasMessage(key) ? translate(key) : null;
}

export function getUniqueName (definition) {
  return `settings_${definition.key}_${uniqueId('setting_')}`;
}

export function getSettingValue (setting) {
  if (setting.definition.multiValues) {
    return setting.values;
  }

  return setting.value;
}
