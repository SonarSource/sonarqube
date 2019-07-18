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
import Toggle from 'sonar-ui-common/components/controls/Toggle';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { DefaultSpecializedInputProps } from '../../utils';

interface Props extends DefaultSpecializedInputProps {
  value: string | boolean | undefined;
}

export default function InputForBoolean({ onChange, name, value }: Props) {
  const displayedValue = value != null ? value : false;
  return (
    <div className="display-inline-block text-top">
      <Toggle name={name} onChange={onChange} value={displayedValue} />
      {value == null && <span className="spacer-left note">{translate('settings.not_set')}</span>}
    </div>
  );
}
