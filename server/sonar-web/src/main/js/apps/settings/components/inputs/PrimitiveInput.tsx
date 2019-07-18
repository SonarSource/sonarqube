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
import * as React from 'react';
import {
  DefaultInputProps,
  DefaultSpecializedInputProps,
  getUniqueName,
  isDefaultOrInherited
} from '../../utils';
import InputForBoolean from './InputForBoolean';
import InputForNumber from './InputForNumber';
import InputForPassword from './InputForPassword';
import InputForSingleSelectList from './InputForSingleSelectList';
import InputForString from './InputForString';
import InputForText from './InputForText';

const typeMapping: {
  [type in T.SettingType]?: React.ComponentType<DefaultSpecializedInputProps>
} = {
  STRING: InputForString,
  TEXT: InputForText,
  PASSWORD: InputForPassword,
  BOOLEAN: InputForBoolean,
  INTEGER: InputForNumber,
  LONG: InputForNumber,
  FLOAT: InputForNumber
};

interface Props extends DefaultInputProps {
  name?: string;
}

export default class PrimitiveInput extends React.PureComponent<Props> {
  render() {
    const { setting, ...other } = this.props;
    const { definition } = setting;

    const name = this.props.name || getUniqueName(definition);

    if (definition.type === 'SINGLE_SELECT_LIST') {
      return (
        <InputForSingleSelectList
          isDefault={isDefaultOrInherited(setting)}
          name={name}
          options={definition.options}
          {...other}
        />
      );
    }

    const InputComponent = (definition.type && typeMapping[definition.type]) || InputForString;
    return <InputComponent isDefault={isDefaultOrInherited(setting)} name={name} {...other} />;
  }
}
