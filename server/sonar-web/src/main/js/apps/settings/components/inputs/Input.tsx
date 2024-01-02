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
import {
  DefaultInputProps,
  DefaultSpecializedInputProps,
  getUniqueName,
  isCategoryDefinition,
  isDefaultOrInherited,
  isSecuredDefinition,
} from '../../utils';
import InputForSecured from './InputForSecured';
import MultiValueInput from './MultiValueInput';
import PrimitiveInput from './PrimitiveInput';
import PropertySetInput from './PropertySetInput';

export default function Input(props: Readonly<DefaultInputProps>) {
  const { setting } = props;
  const { definition } = setting;
  const name = getUniqueName(definition);

  let Input: React.ComponentType<React.PropsWithChildren<DefaultSpecializedInputProps>> =
    PrimitiveInput;

  if (isCategoryDefinition(definition) && definition.multiValues) {
    Input = MultiValueInput;
  }

  if (definition.type === SettingType.PROPERTY_SET) {
    Input = PropertySetInput;
  }

  if (isSecuredDefinition(definition)) {
    return <InputForSecured input={Input} {...props} />;
  }

  return <Input {...props} name={name} isDefault={isDefaultOrInherited(setting)} />;
}
