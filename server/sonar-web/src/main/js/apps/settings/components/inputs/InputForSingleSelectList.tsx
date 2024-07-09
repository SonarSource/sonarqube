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
import { InputSize, Select } from '@sonarsource/echoes-react';
import * as React from 'react';
import { ExtendedSettingDefinition } from '../../../../types/settings';
import { DefaultSpecializedInputProps, getPropertyName } from '../../utils';

type Props = DefaultSpecializedInputProps & Pick<ExtendedSettingDefinition, 'options'>;

export default function InputForSingleSelectList(props: Readonly<Props>) {
  const { name, options: opts, value, setting } = props;

  const options = React.useMemo(
    () => opts.map((option) => ({ label: option, value: option })),
    [opts],
  );

  return (
    <Select
      ariaLabel={getPropertyName(setting.definition)}
      data={options}
      isNotClearable
      name={name}
      onChange={props.onChange}
      size={InputSize.Large}
      value={value}
    />
  );
}
