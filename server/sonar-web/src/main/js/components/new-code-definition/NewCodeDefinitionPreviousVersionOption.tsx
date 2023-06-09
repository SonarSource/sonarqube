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
import { translate } from '../../helpers/l10n';
import { NewCodePeriodSettingType } from '../../types/types';
import RadioCard from '../controls/RadioCard';

interface Props {
  disabled?: boolean;
  isDefault?: boolean;
  onSelect: (selection: NewCodePeriodSettingType) => void;
  selected: boolean;
}

export default function NewCodeDefinitionPreviousVersionOption({
  disabled,
  isDefault,
  onSelect,
  selected,
}: Props) {
  return (
    <RadioCard
      disabled={disabled}
      onClick={() => onSelect(NewCodePeriodSettingType.PREVIOUS_VERSION)}
      selected={selected}
      title={
        translate('new_code_definition.previous_version') +
        (isDefault ? ` (${translate('default')})` : '')
      }
    >
      <div>
        <p>{translate('new_code_definition.previous_version.description')}</p>
        <p className="sw-mt-3">{translate('new_code_definition.previous_version.usecase')}</p>
      </div>
    </RadioCard>
  );
}
