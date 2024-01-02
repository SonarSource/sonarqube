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
import { components, OptionProps, OptionTypeBase, SingleValueProps } from 'react-select';
import Select from '../../../components/controls/Select';
import SeverityHelper from '../../../components/shared/SeverityHelper';
import { SEVERITIES } from '../../../helpers/constants';
import { translate } from '../../../helpers/l10n';

export interface SeveritySelectProps {
  isDisabled: boolean;
  severity: string;
  ariaLabelledby: string;
  onChange: (value: OptionTypeBase) => void;
}

export function SeveritySelect(props: SeveritySelectProps) {
  const { isDisabled, severity, ariaLabelledby } = props;
  const serverityOption = SEVERITIES.map((severity) => ({
    label: translate('severity', severity),
    value: severity,
  }));

  function Option(props: OptionProps<OptionTypeBase, false>) {
    return (
      <components.Option {...props}>
        <SeverityHelper className="display-flex-center" severity={props.data.value} />
      </components.Option>
    );
  }

  function SingleValue(props: SingleValueProps<OptionTypeBase>) {
    return (
      <components.SingleValue {...props}>
        <SeverityHelper className="display-flex-center" severity={props.data.value} />
      </components.SingleValue>
    );
  }

  return (
    <Select
      aria-labelledby={ariaLabelledby}
      isDisabled={isDisabled}
      onChange={props.onChange}
      components={{ Option, SingleValue }}
      options={serverityOption}
      isSearchable={false}
      value={serverityOption.find((s) => s.value === severity)}
    />
  );
}
