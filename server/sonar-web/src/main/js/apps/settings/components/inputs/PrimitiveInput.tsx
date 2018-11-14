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
import * as React from 'react';
import InputForString from './InputForString';
import InputForText from './InputForText';
import InputForPassword from './InputForPassword';
import InputForBoolean from './InputForBoolean';
import InputForNumber from './InputForNumber';
import InputForSingleSelectList from './InputForSingleSelectList';
import {
  getUniqueName,
  isDefaultOrInherited,
  DefaultInputProps,
  DefaultSpecializedInputProps
} from '../../utils';
import { SettingType } from '../../../../app/types';

const typeMapping: {
  [type in SettingType]?:
    | React.ComponentClass<DefaultSpecializedInputProps>
    | React.StatelessComponent<DefaultSpecializedInputProps>
} = {
  [SettingType.String]: InputForString,
  [SettingType.Text]: InputForText,
  [SettingType.Password]: InputForPassword,
  [SettingType.Boolean]: InputForBoolean,
  [SettingType.Integer]: InputForNumber,
  [SettingType.Long]: InputForNumber,
  [SettingType.Float]: InputForNumber
};

interface Props extends DefaultInputProps {
  name?: string;
}

export default class PrimitiveInput extends React.PureComponent<Props> {
  render() {
    const { setting, ...other } = this.props;
    const { definition } = setting;

    const name = this.props.name || getUniqueName(definition);

    if (definition.type === SettingType.SingleSelectList) {
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
