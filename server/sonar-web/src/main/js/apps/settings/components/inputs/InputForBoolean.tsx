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
import { Note, Switch } from 'design-system';
import * as React from 'react';
import { translate } from '../../../../helpers/l10n';
import { DefaultSpecializedInputProps, getPropertyName } from '../../utils';

interface Props extends DefaultSpecializedInputProps {
  value: string | boolean | undefined;
}

export default function InputForBoolean({ onChange, name, value, setting }: Props) {
  const toggleValue = getToggleValue(value != null ? value : false);

  const propertyName = getPropertyName(setting.definition);

  return (
    <div className="sw-flex sw-items-center">
      <Switch
        name={name}
        onChange={onChange}
        value={toggleValue}
        labels={{
          on: propertyName,
          off: propertyName,
        }}
      />
      {value == null && <Note className="sw-ml-2">{translate('settings.not_set')}</Note>}
    </div>
  );
}

function getToggleValue(value: string | boolean) {
  return typeof value === 'string' ? value === 'true' : value;
}
