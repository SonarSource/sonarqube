/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

function PrimitiveInput(
  props: DefaultSpecializedInputProps,
  ref: React.ForwardedRef<HTMLInputElement>,
) {
  const { ariaDescribedBy, setting, name, isDefault, ...other } = props;
  const { definition } = setting;
  const typeMapping: {
    [type in SettingType]?: React.ComponentType<
      React.PropsWithChildren<DefaultSpecializedInputProps & { options?: string[] }>
    >;
  } = React.useMemo(
    () => ({
      STRING: InputForString,
      TEXT: InputForText,
      JSON: InputForJSON,
      PASSWORD: InputForPassword,
      BOOLEAN: InputForBoolean,
      INTEGER: InputForNumber,
      LONG: InputForNumber,
      FLOAT: InputForNumber,
      SINGLE_SELECT_LIST: InputForSingleSelectList,
      FORMATTED_TEXT: InputForFormattedText,
    }),
    [definition.options],
  );

  const InputComponent = (definition.type && typeMapping[definition.type]) || InputForString;
  let id = `input-${name}`;
  if (typeof props.index === 'number') {
    id = `${id}-${props.index}`;
  }

  return (
    <InputComponent
      ariaDescribedBy={ariaDescribedBy}
      id={id}
      isDefault={isDefault}
      name={name}
      options={definition.type === SettingType.SINGLE_SELECT_LIST ? definition.options : undefined}
      setting={setting}
      ref={ref}
      {...other}
    />
  );
}

export default React.forwardRef(PrimitiveInput);
