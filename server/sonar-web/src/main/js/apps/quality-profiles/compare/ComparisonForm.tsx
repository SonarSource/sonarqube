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
import Select from '../../../components/controls/Select';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';
import { Profile } from '../types';

interface Props {
  profile: Profile;
  profiles: Profile[];
  onCompare: (rule: string) => void;
  withKey?: string;
}

interface Option {
  value: string;
  label: string;
  isDefault: boolean | undefined;
}
export default class ComparisonForm extends React.PureComponent<Props> {
  handleChange = (option: { value: string }) => {
    this.props.onCompare(option.value);
  };

  optionRenderer(
    options: Option[],
    props: OptionProps<Omit<Option, 'label' | 'isDefault'>, false>
  ) {
    const { data } = props;
    return <components.Option {...props}>{renderValue(data, options)}</components.Option>;
  }

  singleValueRenderer = (
    options: Option[],
    props: SingleValueProps<Omit<typeof options[0], 'label' | 'isDefault'>>
  ) => (
    <components.SingleValue {...props}>{renderValue(props.data, options)}</components.SingleValue>
  );

  render() {
    const { profile, profiles, withKey } = this.props;
    const options = profiles
      .filter((p) => p.language === profile.language && p !== profile)
      .map((p) => ({ value: p.key, label: p.name, isDefault: p.isDefault }));

    return (
      <div className="display-inline-block">
        <label htmlFor="quality-profiles-comparision-input" className="spacer-right">
          {translate('quality_profiles.compare_with')}
        </label>
        <Select
          className="input-large"
          autoFocus={true}
          isClearable={false}
          id="quality-profiles-comparision"
          inputId="quality-profiles-comparision-input"
          onChange={this.handleChange}
          options={options}
          isSearchable={true}
          components={{
            Option: this.optionRenderer.bind(this, options),
            SingleValue: this.singleValueRenderer.bind(null, options),
          }}
          value={options.filter((o) => o.value === withKey)}
        />
      </div>
    );
  }
}

function renderValue(p: Omit<Option, 'label' | 'isDefault'>, options: Option[]) {
  const selectedOption = options.find((o) => o.value === p.value);
  if (selectedOption !== undefined) {
    return (
      <div>
        <span>{selectedOption.label}</span>
        {selectedOption.isDefault && (
          <Tooltip overlay={translate('quality_profiles.list.default.help')}>
            <span className="spacer-left badge">{translate('default')}</span>
          </Tooltip>
        )}
      </div>
    );
  }
}
