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
import { debounce, omit } from 'lodash';
import * as React from 'react';
import { components, ControlProps, OptionProps, SingleValueProps } from 'react-select';
import {
  searchGroups,
  searchUsers,
  SearchUsersGroupsParameters,
} from '../../../api/quality-profiles';
import { SearchSelect } from '../../../components/controls/Select';
import GroupIcon from '../../../components/icons/GroupIcon';
import Avatar from '../../../components/ui/Avatar';
import { translate } from '../../../helpers/l10n';
import { UserSelected } from '../../../types/types';
import { Group } from './ProfilePermissions';

type Option = UserSelected | Group;
type OptionWithValue = Option & { value: string };

interface Props {
  onChange: (option: OptionWithValue) => void;
  profile: { language: string; name: string };
}

const DEBOUNCE_DELAY = 250;

export default class ProfilePermissionsFormSelect extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
    this.handleSearch = debounce(this.handleSearch, DEBOUNCE_DELAY);
  }

  optionRenderer(props: OptionProps<OptionWithValue, false>) {
    const { data } = props;
    return <components.Option {...props}>{customOptions(data)}</components.Option>;
  }

  singleValueRenderer = (props: SingleValueProps<OptionWithValue>) => (
    <components.SingleValue {...props}>{customOptions(props.data)}</components.SingleValue>
  );

  controlRenderer = (props: ControlProps<OptionWithValue, false>) => (
    <components.Control {...omit(props, ['children'])} className="abs-height-100">
      {props.children}
    </components.Control>
  );

  handleSearch = (q: string, resolve: (options: OptionWithValue[]) => void) => {
    const { profile } = this.props;
    const parameters: SearchUsersGroupsParameters = {
      language: profile.language,
      q,
      qualityProfile: profile.name,
      selected: 'deselected',
    };
    Promise.all([searchUsers(parameters), searchGroups(parameters)])
      .then(([usersResponse, groupsResponse]) => [...usersResponse.users, ...groupsResponse.groups])
      .then((options: Option[]) => options.map((opt) => ({ ...opt, value: getStringValue(opt) })))
      .then(resolve)
      .catch(() => resolve([]));
  };

  render() {
    const noResultsText = translate('no_results');

    return (
      <SearchSelect
        className="width-100"
        autoFocus={true}
        isClearable={false}
        id="change-profile-permission"
        inputId="change-profile-permission-input"
        onChange={this.props.onChange}
        defaultOptions={true}
        loadOptions={this.handleSearch}
        placeholder=""
        noOptionsMessage={() => noResultsText}
        large={true}
        components={{
          Option: this.optionRenderer,
          SingleValue: this.singleValueRenderer,
          Control: this.controlRenderer,
        }}
      />
    );
  }
}

function isUser(option: Option): option is UserSelected {
  return (option as UserSelected).login !== undefined;
}

function getStringValue(option: Option) {
  return isUser(option) ? `user:${option.login}` : `group:${option.name}`;
}

function customOptions(option: OptionWithValue) {
  return isUser(option) ? (
    <span className="display-flex-center">
      <Avatar hash={option.avatar} name={option.name} size={16} />
      <strong className="spacer-left">{option.name}</strong>
      <span className="note little-spacer-left">{option.login}</span>
    </span>
  ) : (
    <span className="display-flex-center">
      <GroupIcon size={16} />
      <strong className="spacer-left">{option.name}</strong>
    </span>
  );
}
