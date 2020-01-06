/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import RadioCard from 'sonar-ui-common/components/controls/RadioCard';
import { translate } from 'sonar-ui-common/helpers/l10n';

export interface Props {
  disabled?: boolean;
  onSelect: (selection: T.NewCodePeriodSettingType) => void;
  selected: boolean;
}

export default function BaselineSettingAnalysis({ disabled, onSelect, selected }: Props) {
  return (
    <RadioCard
      disabled={disabled}
      onClick={() => onSelect('SPECIFIC_ANALYSIS')}
      selected={selected}
      title={translate('baseline.specific_analysis')}>
      <p className="big-spacer-bottom">{translate('baseline.specific_analysis.description')}</p>
    </RadioCard>
  );
}
