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
import { translate, translateWithParameters } from "../../helpers/l10n";
import Avatar from "../../components/ui/Avatar";
import { components, OptionProps, SingleValueProps } from 'react-select';
import { isUserActive, UserActive } from "../../types/users";
import { searchMembers } from "../../api/organizations";
import { Organization } from "../../types/types";
import { SearchSelect } from "design-system";

export const MIN_QUERY_LENGTH = 2;

interface UserOption extends BasicSelectOption {
  login: string;
  name: string;
  avatar?: string;
}

interface UsersSelectSearchProps {
  organization: Organization;
  autoFocus?: boolean;
  excludedUsers: string[];
  handleValueChange: (option: UserOption) => void;
  selectedUser?: UserActive;
}

const LIST_SIZE = 10;

export default function UsersSelectSearch(props: UsersSelectSearchProps) {

  const handleUserSearch = (query: string, resolve: (options: UserOption[]) => void) => {
    if (query.length < MIN_QUERY_LENGTH) {
      resolve([]);
      return;
    }

    const data = { organization: props.organization.kee, selected: 'deselected' };

    searchMembers({ ...data, q: query })
        .then(({ users }) => users
            .filter(user => !props.excludedUsers.includes(user.login)).slice(0, LIST_SIZE)
            .map((r) => {
              const userInfo = r.name || r.login;

              return {
                avatar: r.avatar,
                name: isUserActive(r) ? userInfo : translateWithParameters('user.x_deleted', userInfo),
                login: r.login,
              } as UserOption;
            })
        )
        .then(resolve)
        .catch(() => resolve([]));
  };

  const renderUser = (option: UserOption) => (
      <div className="display-flex-center" title={option ? option.name : ''}>
        {option.avatar !== undefined && (
            <Avatar className="spacer-right" hash={option.avatar} name={option.label} size={16}/>
        )}
        {option.login}
      </div>
  );

  const renderOption = (props: OptionProps<UserOption, false>) =>
      <components.Option {...props}>{renderUser(props.data)}</components.Option>

  const renderSingleValue = (props: SingleValueProps<UserOption>) =>
      <components.SingleValue {...props}>{renderUser(props.data)}</components.SingleValue>

  return (
      <SearchSelect
          className="input-super-large"
          autoFocus={props.autoFocus}
          isClearable={false}
          onChange={props.handleValueChange}
          // onInputChange={this.handleInputChange}
          // options={this.state.searchResult}
          loadOptions={handleUserSearch}
          placeholder=""
          components={{
            Option: renderOption,
            SingleValue: renderSingleValue,
          }}
          noOptionsMessage={({ inputValue }) =>
              inputValue.length < MIN_QUERY_LENGTH
                  ? translateWithParameters('select2.tooShort', MIN_QUERY_LENGTH)
                  : translate('select2.noMatches')
          }
      />
  );
}
