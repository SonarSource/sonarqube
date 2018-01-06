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
import React from 'react';
import PropTypes from 'prop-types';
import InputForString from './InputForString';
import InputForText from './InputForText';
import InputForPassword from './InputForPassword';
import InputForBoolean from './InputForBoolean';
import InputForNumber from './InputForNumber';
import InputForSingleSelectList from './InputForSingleSelectList';
import { getUniqueName, isDefaultOrInherited } from '../../utils';
import * as types from '../../constants';

const typeMapping = {
  [types.TYPE_STRING]: InputForString,
  [types.TYPE_TEXT]: InputForText,
  [types.TYPE_PASSWORD]: InputForPassword,
  [types.TYPE_BOOLEAN]: InputForBoolean,
  [types.TYPE_INTEGER]: InputForNumber,
  [types.TYPE_LONG]: InputForNumber,
  [types.TYPE_FLOAT]: InputForNumber
};

export default class PrimitiveInput extends React.PureComponent {
  static propTypes = {
    setting: PropTypes.object.isRequired,
    value: PropTypes.any,
    onChange: PropTypes.func.isRequired
  };

  render() {
    const { setting, value, onChange, ...other } = this.props;
    const { definition } = setting;

    const name = getUniqueName(definition);

    if (definition.type === types.TYPE_SINGLE_SELECT_LIST) {
      return (
        <InputForSingleSelectList
          name={name}
          value={value}
          isDefault={isDefaultOrInherited(setting)}
          options={definition.options}
          onChange={onChange}
          {...other}
        />
      );
    }

    const InputComponent = typeMapping[definition.type] || InputForString;
    return (
      <InputComponent
        name={name}
        value={value}
        isDefault={isDefaultOrInherited(setting)}
        onChange={onChange}
        {...other}
      />
    );
  }
}
