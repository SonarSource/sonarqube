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
import { InputSelect, LabelValueSelectOption, Note } from 'design-system';
import * as React from 'react';
import { OptionProps, SingleValueProps, components } from 'react-select';
import { translate } from '../../helpers/l10n';
import { AlmInstanceBase } from '../../types/alm-settings';

function optionRenderer(props: OptionProps<LabelValueSelectOption<AlmInstanceBase>, false>) {
  // For tests and a11y
  props.innerProps.role = 'option';
  props.innerProps['aria-selected'] = props.isSelected;

  return <components.Option {...props}>{customOptions(props.data.value)}</components.Option>;
}

function singleValueRenderer(
  props: SingleValueProps<LabelValueSelectOption<AlmInstanceBase>, false>,
) {
  return (
    <components.SingleValue {...props}>{customOptions(props.data.value)}</components.SingleValue>
  );
}

function customOptions(instance: AlmInstanceBase) {
  return instance.url ? (
    <>
      <span>{instance.key} — </span>
      <Note>{instance.url}</Note>
    </>
  ) : (
    <span>{instance.key}</span>
  );
}

function orgToOption(alm: AlmInstanceBase) {
  return { value: alm, label: alm.key };
}

interface Props {
  className: string;
  initialValue?: string;
  inputId: string;
  instances: AlmInstanceBase[];
  onChange: (instance: AlmInstanceBase) => void;
}

export default function AlmSettingsInstanceSelector(props: Props) {
  const { instances, initialValue, className, inputId } = props;

  return (
    <InputSelect
      inputId={inputId}
      className={className}
      isClearable={false}
      isSearchable={false}
      options={instances.map(orgToOption)}
      onChange={(data: LabelValueSelectOption<AlmInstanceBase>) => {
        props.onChange(data.value);
      }}
      components={{
        Option: optionRenderer,
        SingleValue: singleValueRenderer,
      }}
      placeholder={translate('alm.configuration.selector.placeholder')}
      getOptionValue={(opt: LabelValueSelectOption<AlmInstanceBase>) => opt.value.key}
      value={instances.map(orgToOption).find((opt) => opt.value.key === initialValue) ?? null}
      size="full"
    />
  );
}
