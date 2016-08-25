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
import React from 'react';
import InputForString from './InputForString';
import InputForText from './InputForText';
import InputForPassword from './InputForPassword';
import InputForBoolean from './InputForBoolean';
import InputForNumber from './InputForNumber';
import InputForSingleSelectList from './InputForSingleSelectList';
import {
    TYPE_BOOLEAN,
    TYPE_INTEGER,
    TYPE_LONG,
    TYPE_TEXT,
    TYPE_PASSWORD,
    TYPE_FLOAT,
    TYPE_SINGLE_SELECT_LIST
} from '../../constants';
import { getSettingValue, getUniqueName } from '../../utils';

const typeMapping = {
  [TYPE_TEXT]: InputForText,
  [TYPE_PASSWORD]: InputForPassword,
  [TYPE_BOOLEAN]: InputForBoolean,
  [TYPE_INTEGER]: InputForNumber,
  [TYPE_LONG]: InputForNumber,
  [TYPE_FLOAT]: InputForNumber
};

const renderInput = (setting, onChange) => {
  const { definition } = setting;
  const name = getUniqueName(definition);
  const value = getSettingValue(setting);

  if (definition.type === TYPE_SINGLE_SELECT_LIST) {
    return <InputForSingleSelectList name={name} value={value} options={definition.options} onChange={onChange}/>
  }

  const InputComponent = typeMapping[definition.type] || InputForString;
  return <InputComponent name={name} value={value} onChange={onChange}/>;
};

export default renderInput;
