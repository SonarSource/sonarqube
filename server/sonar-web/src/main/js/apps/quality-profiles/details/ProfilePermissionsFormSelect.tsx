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
import {
  Avatar,
  GenericAvatar,
  LabelValueSelectOption,
  SearchSelectDropdown,
  UserGroupIcon,
} from 'design-system';
import { omit } from 'lodash';
import React from 'react';
import { useIntl } from 'react-intl';
import {
  SearchUsersGroupsParameters,
  searchGroups,
  searchUsers,
} from '../../../api/quality-profiles';
import { UserSelected } from '../../../types/types';
import { Group } from './ProfilePermissions';

type Option = UserSelected | Group;

interface Props {
  organization: string;
  onChange: (option: Option) => void;
  profile: { language: string; name: string };
  selected?: Option;
}

export default function ProfilePermissionsFormSelect({
  organization,
  onChange,
  profile,
  selected,
}: Props) {
  const [defaultOptions, setDefaultOptions] = React.useState<LabelValueSelectOption<string>[]>([]);
  const intl = useIntl();

  const value = selected ? getOption(selected) : null;

  const controlLabel = selected ? (
    <>
      {isUser(selected) ? (
        <Avatar hash={selected.avatar} name={selected.name} size="xs" className="sw-mt-1" />
      ) : (
        <GenericAvatar Icon={UserGroupIcon} name={selected.name} size="xs" className="sw-mt-1" />
      )}{' '}
      {selected.name}
    </>
  ) : undefined;

  const loadOptions = React.useCallback(
    async (q = '') => {
      const parameters: SearchUsersGroupsParameters = {
        organization,
        language: profile.language,
        q,
        qualityProfile: profile.name,
        selected: 'deselected',
      };
      const [{ users }, { groups }] = await Promise.all([
        searchUsers(parameters),
        searchGroups(parameters),
      ]);

      return { users, groups };
    },
    [organization, profile.language, profile.name],
  );

  const loadInitial = React.useCallback(async () => {
    try {
      const { users, groups } = await loadOptions();
      setDefaultOptions([...users, ...groups].map(getOption));
    } catch {
      setDefaultOptions([]);
    }
  }, [loadOptions]);

  const handleSearch = React.useCallback(
    async (q: string, cb: (options: LabelValueSelectOption<string>[]) => void) => {
      try {
        const { users, groups } = await loadOptions(q);
        cb([...users, ...groups].map(getOption));
      } catch {
        cb([]);
      }
    },
    [loadOptions],
  );

  const handleChange = (option: LabelValueSelectOption<string>) => {
    onChange(omit(option, ['Icon', 'label', 'value']) as Option);
  };

  React.useEffect(() => {
    loadInitial();
  }, [loadInitial]);

  return (
    <SearchSelectDropdown
      id="change-profile-permission"
      inputId="change-profile-permission-input"
      controlAriaLabel={intl.formatMessage({ id: 'quality_profiles.search_description' })}
      size="full"
      controlLabel={controlLabel}
      onChange={handleChange}
      defaultOptions={defaultOptions}
      loadOptions={handleSearch}
      value={value}
    />
  );
}

const getOption = (option: Option): LabelValueSelectOption<string> => ({
  ...option,
  value: getStringValue(option),
  label: option.name,
  Icon: isUser(option) ? (
    <Avatar hash={option.avatar} name={option.name} size="xs" />
  ) : (
    <GenericAvatar Icon={UserGroupIcon} name={option.name} size="xs" />
  ),
});

function isUser(option: Option): option is UserSelected {
  return (option as UserSelected).login !== undefined;
}

function getStringValue(option: Option) {
  return isUser(option) ? `user:${option.login}` : `group:${option.name}`;
}
