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
import { Badge, SearchSelectDropdown } from 'design-system';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { OptionProps, Options, components } from 'react-select';
import Tooltip from '../../../components/controls/Tooltip';
import { Profile } from '../types';

interface Props {
  onCompare: (rule: string) => void;
  profile: Profile;
  profiles: Profile[];
  withKey?: string;
}

interface Option {
  isDefault: boolean | undefined;
  label: string;
  value: string;
}

export default function ComparisonForm(props: Readonly<Props>) {
  const { profile, profiles, withKey } = props;
  const intl = useIntl();

  const options = profiles
    .filter((p) => p.language === profile.language && p !== profile)
    .map((p) => ({ value: p.key, label: p.name, isDefault: p.isDefault }));

  const handleProfilesSearch = React.useCallback(
    (query: string, cb: (options: Options<Option>) => void) => {
      cb(options.filter((option) => option.label.toLowerCase().includes(query.toLowerCase())));
    },
    [options],
  );

  return (
    <>
      <span className="sw-mr-2">{intl.formatMessage({ id: 'quality_profiles.compare_with' })}</span>
      <SearchSelectDropdown
        controlPlaceholder={intl.formatMessage({ id: 'select_verb' })}
        controlAriaLabel={intl.formatMessage({ id: 'quality_profiles.compare_with' })}
        options={options}
        onChange={(option: Option) => props.onCompare(option.value)}
        defaultOptions={options}
        loadOptions={handleProfilesSearch}
        components={{
          Option: OptionRenderer,
        }}
        autoFocus
        controlSize="medium"
        value={options.find((o) => o.value === withKey)}
      />
    </>
  );
}

function OptionRenderer(props: Readonly<OptionProps<Option, false>>) {
  const { isDefault, label } = props.data;
  const intl = useIntl();

  // For tests and a11y
  props.innerProps.role = 'option';
  props.innerProps['aria-selected'] = props.isSelected;

  return (
    <components.Option {...props}>
      <span>{label}</span>
      {isDefault && (
        <Tooltip content={intl.formatMessage({ id: 'quality_profiles.list.default.help' })}>
          <span>
            <Badge className="sw-ml-1">{intl.formatMessage({ id: 'default' })}</Badge>
          </span>
        </Tooltip>
      )}
    </components.Option>
  );
}
