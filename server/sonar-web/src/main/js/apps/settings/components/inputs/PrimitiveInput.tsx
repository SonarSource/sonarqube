/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { SettingType } from '../../../../types/settings';
import { DefaultSpecializedInputProps } from '../../utils';
import InputForBoolean from './InputForBoolean';
import InputForFormattedText from './InputForFormattedText';
import InputForJSON from './InputForJSON';
import InputForNumber from './InputForNumber';
import InputForPassword from './InputForPassword';
import InputForSingleSelectList from './InputForSingleSelectList';
import InputForString from './InputForString';
import InputForText from './InputForText';

function withOptions(
  options: string[],
): React.ComponentType<React.PropsWithChildren<DefaultSpecializedInputProps>> {
  return function Wrapped(props: DefaultSpecializedInputProps) {
    return <InputForSingleSelectList options={options} {...props} />;
  };
}

export default function PrimitiveInput(props: DefaultSpecializedInputProps) {
  const { setting, name, isDefault, ...other } = props;
  const { definition } = setting;
  const typeMapping: {
    [type in SettingType]?: React.ComponentType<
      React.PropsWithChildren<DefaultSpecializedInputProps>
    >;
  } = {
    STRING: InputForString,
    TEXT: InputForText,
    JSON: InputForJSON,
    PASSWORD: InputForPassword,
    BOOLEAN: InputForBoolean,
    INTEGER: InputForNumber,
    LONG: InputForNumber,
    FLOAT: InputForNumber,
    SINGLE_SELECT_LIST: withOptions(definition.options),
    FORMATTED_TEXT: InputForFormattedText,
  };

  const InputComponent = (definition.type && typeMapping[definition.type]) || InputForString;

  return <InputComponent isDefault={isDefault} name={name} setting={setting} {...other} />;
}
