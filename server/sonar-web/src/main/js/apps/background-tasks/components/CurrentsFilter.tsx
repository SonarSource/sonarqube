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
import { Switch } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { CURRENTS } from '../constants';

interface CurrentsFilterProps {
  value?: string;
  onChange: (value: string) => void;
}

export default function CurrentsFilter(props: Readonly<CurrentsFilterProps>) {
  const { value, onChange } = props;
  const checked = value === CURRENTS.ONLY_CURRENTS;

  const handleChange = React.useCallback(
    (value: boolean) => {
      const newValue = value ? CURRENTS.ONLY_CURRENTS : CURRENTS.ALL;
      onChange(newValue);
    },
    [onChange],
  );

  return (
    <Switch
      value={checked}
      onChange={handleChange}
      labels={{
        on: translate('background_tasks.currents_filter.ONLY_CURRENTS'),
        off: translate('background_tasks.currents_filter.ALL'),
      }}
    />
  );
}
