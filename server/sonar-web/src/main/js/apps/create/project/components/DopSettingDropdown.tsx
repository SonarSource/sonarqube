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
import classNames from 'classnames';
import { DarkLabel, InputSelect, LabelValueSelectOption, Note } from 'design-system';
import * as React from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { OptionProps, SingleValueProps, components } from 'react-select';
import { translate } from '../../../../helpers/l10n';
import { AlmKeys } from '../../../../types/alm-settings';
import { DopSetting } from '../../../../types/dop-translation';

export interface DopSettingDropdownProps {
  almKey: AlmKeys;
  className?: string;
  dopSettings?: DopSetting[];
  onChangeSetting: (setting: DopSetting) => void;
  selectedDopSetting?: DopSetting;
}

const MIN_SIZE_INSTANCES = 2;

function optionRenderer(props: OptionProps<LabelValueSelectOption<DopSetting>, false>) {
  return <components.Option {...props}>{customOptions(props.data.value)}</components.Option>;
}

function singleValueRenderer(props: SingleValueProps<LabelValueSelectOption<DopSetting>, false>) {
  return (
    <components.SingleValue {...props}>{customOptions(props.data.value)}</components.SingleValue>
  );
}

function customOptions(setting: DopSetting) {
  return setting.url ? (
    <>
      <span>{setting.key} — </span>
      <Note>{setting.url}</Note>
    </>
  ) : (
    <span>{setting.key}</span>
  );
}

function orgToOption(alm: DopSetting) {
  return { value: alm, label: alm.key };
}

export default function DopSettingDropdown(props: Readonly<DopSettingDropdownProps>) {
  const { formatMessage } = useIntl();

  const { almKey, className, dopSettings, onChangeSetting, selectedDopSetting } = props;
  if (!dopSettings || dopSettings.length < MIN_SIZE_INSTANCES) {
    return null;
  }

  return (
    <div className={classNames('sw-flex sw-flex-col', className)}>
      <DarkLabel htmlFor="dop-setting-dropdown" className="sw-mb-2">
        <FormattedMessage
          id="onboarding.create_project.monorepo.choose_dop_setting"
          values={{ almKey: formatMessage({ id: `alm.${almKey}` }) }}
        />
      </DarkLabel>

      <InputSelect
        inputId="dop-setting-dropdown"
        className={className}
        isClearable={false}
        isSearchable={false}
        options={dopSettings.map(orgToOption)}
        onChange={(data: LabelValueSelectOption<DopSetting>) => {
          onChangeSetting(data.value);
        }}
        components={{
          Option: optionRenderer,
          SingleValue: singleValueRenderer,
        }}
        placeholder={translate('alm.configuration.selector.placeholder')}
        getOptionValue={(opt: LabelValueSelectOption<DopSetting>) => opt.value.key}
        value={
          dopSettings.map(orgToOption).find((opt) => opt.value.key === selectedDopSetting?.key) ??
          null
        }
        size="full"
      />
    </div>
  );
}
