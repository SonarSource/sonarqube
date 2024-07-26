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
import { ToggleButton } from 'design-system';
import * as React from 'react';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { Provider } from '../../types/types';

interface ManagedFilterProps {
  loading: boolean;
  manageProvider: Provider | undefined;
  managed: boolean | undefined;
  setManaged: (managed: boolean | undefined) => void;
}

export function ManagedFilter(props: Readonly<ManagedFilterProps>) {
  const { manageProvider, loading, managed } = props;

  if (manageProvider === undefined) {
    return null;
  }

  return (
    <div className="sw-mr-4">
      <ToggleButton
        value={managed ?? 'all'}
        disabled={loading}
        options={[
          { label: translate('all'), value: 'all' },
          {
            label: translateWithParameters('managed', translate(`managed.${manageProvider}`)),
            value: true,
          },
          { label: translate('local'), value: false },
        ]}
        onChange={(filterOption) => {
          if (filterOption === 'all') {
            props.setManaged(undefined);
          } else {
            props.setManaged(filterOption);
          }
        }}
      />
    </div>
  );
}
