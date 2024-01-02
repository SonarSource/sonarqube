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
import { components, OptionProps, SingleValueProps } from 'react-select';
import { translate } from '../../helpers/l10n';
import { AlmSettingsInstance } from '../../types/alm-settings';
import Select from '../controls/Select';

function optionRenderer(props: OptionProps<AlmSettingsInstance, false>) {
  return <components.Option {...props}>{customOptions(props.data)}</components.Option>;
}

function singleValueRenderer(props: SingleValueProps<AlmSettingsInstance>) {
  return <components.SingleValue {...props}>{customOptions(props.data)}</components.SingleValue>;
}

function customOptions(instance: AlmSettingsInstance) {
  return instance.url ? (
    <>
      <span>{instance.key} — </span>
      <span className="text-muted">{instance.url}</span>
    </>
  ) : (
    <span>{instance.key}</span>
  );
}

interface Props {
  instances: AlmSettingsInstance[];
  initialValue?: string;
  onChange: (instance: AlmSettingsInstance) => void;
  classNames: string;
  inputId: string;
}

export default function AlmSettingsInstanceSelector(props: Props) {
  const { instances, initialValue, classNames, inputId } = props;

  return (
    <Select
      inputId={inputId}
      className={classNames}
      isClearable={false}
      isSearchable={false}
      options={instances}
      onChange={(inst) => {
        if (inst) {
          props.onChange(inst);
        }
      }}
      components={{
        Option: optionRenderer,
        SingleValue: singleValueRenderer,
      }}
      placeholder={translate('alm.configuration.selector.placeholder')}
      getOptionValue={(opt) => opt.key}
      value={instances.find((inst) => inst.key === initialValue)}
    />
  );
}
