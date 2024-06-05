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
import { InputSelect, LabelValueSelectOption } from 'design-system';
import * as React from 'react';
import { OptionProps, SingleValueProps, components } from 'react-select';
import SeverityHelper from '../../../components/shared/SeverityHelper';
import { SEVERITIES } from '../../../helpers/constants';
import { translate } from '../../../helpers/l10n';
import { IssueSeverity } from '../../../types/issues';

export interface SeveritySelectProps {
  isDisabled: boolean;
  onChange: (value: LabelValueSelectOption<IssueSeverity>) => void;
  severity: string;
}

function Option(props: Readonly<OptionProps<LabelValueSelectOption<IssueSeverity>, false>>) {
  return (
    <components.Option {...props}>
      <SeverityHelper className="sw-flex sw-items-center" severity={props.data.value} />
    </components.Option>
  );
}

function SingleValue(
  props: Readonly<SingleValueProps<LabelValueSelectOption<IssueSeverity>, false>>,
) {
  return (
    <components.SingleValue {...props}>
      <SeverityHelper className="sw-flex sw-items-center" severity={props.data.value} />
    </components.SingleValue>
  );
}

export function SeveritySelect(props: SeveritySelectProps) {
  const { isDisabled, severity } = props;
  const serverityOption = SEVERITIES.map((severity) => ({
    label: translate('severity', severity),
    value: severity,
  }));

  return (
    <InputSelect
      aria-label={translate('severity')}
      inputId="coding-rules-severity-select"
      isDisabled={isDisabled}
      onChange={props.onChange}
      components={{ Option, SingleValue }}
      options={serverityOption}
      isSearchable={false}
      value={serverityOption.find((s) => s.value === severity)}
    />
  );
}
