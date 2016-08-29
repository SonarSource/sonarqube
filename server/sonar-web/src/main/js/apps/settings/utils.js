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
import { translate, hasMessage } from '../../helpers/l10n';
import { TYPE_PROPERTY_SET, TYPE_BOOLEAN, TYPE_SINGLE_SELECT_LIST } from './constants';

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

export function getUniqueName (definition, index = null) {
  const indexSuffix = index != null ? `[${index}]` : '';
  return `settings[${definition.key}]${indexSuffix}`;
}

export function getSettingValue (setting) {
  if (setting.definition.multiValues) {
    return setting.values;
  } else if (setting.definition.type === TYPE_PROPERTY_SET) {
    return setting.fieldsValues;
  } else {
    return setting.value;
  }
}

export function isEmptyValue (definition, value) {
  if (value == null) {
    return true;
  } else if (definition.type === TYPE_BOOLEAN) {
    return false;
  } else {
    return value.length === 0;
  }
}

export function getEmptyValue (definition) {
  if (definition.type === TYPE_BOOLEAN || definition.type === TYPE_SINGLE_SELECT_LIST) {
    return null;
  } else {
    return '';
  }
}

export function isDefaultOrInherited (setting) {
  return !!setting.default || !!setting.inherited;
}

/**
 * Check if provided definition is a complex one
 * Complex definitions are property sets or multi values
 * @param definition
 * @returns {boolean}
 */
export function isComplexDefinition (definition) {
  return definition.type === TYPE_PROPERTY_SET || definition.multiValues === true;
}

/**
 * Get and format the default value
 * @param definition
 * @returns {string}
 */
export function getDefaultValue (definition) {
  if (definition.defaultValue == null) {
    return '<no value>';
  } else if (definition.type === TYPE_BOOLEAN) {
    return definition.defaultValue ? translate('settings.boolean.true') : translate('settings.boolean.false');
  } else {
    return definition.defaultValue;
  }
}
