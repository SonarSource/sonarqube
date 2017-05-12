/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import { debounce, map } from 'lodash';
import Avatar from '../../../components/ui/Avatar';
import BubblePopup from '../../../components/common/BubblePopup';
import SelectList from '../../../components/common/SelectList';
import SelectListItem from '../../../components/common/SelectListItem';
import getCurrentUserFromStore from '../../../app/utils/getCurrentUserFromStore';
import { areThereCustomOrganizations } from '../../../store/organizations/utils';
import { searchMembers } from '../../../api/organizations';
import { searchUsers } from '../../../api/users';
import { translate } from '../../../helpers/l10n';
import type { Issue } from '../types';

type User = {
  avatar?: string,
  email?: string,
  login: string,
  name: string
};

type Props = {
  issue: Issue,
  onFail: Error => void,
  onSelect: string => void,
  popupPosition?: {}
};

type State = {
  query: string,
  users: Array<User>,
  currentUser: string
};

const LIST_SIZE = 10;

export default class SetAssigneePopup extends React.PureComponent {
  defaultUsersArray: Array<User>;
  organizationEnabled: boolean;
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.organizationEnabled = areThereCustomOrganizations();
    this.searchUsers = debounce(this.searchUsers, 250);
    this.searchMembers = debounce(this.searchMembers, 250);
    this.defaultUsersArray = [{ login: '', name: translate('unassigned') }];

    const currentUser = getCurrentUserFromStore();
    if (currentUser != null) {
      this.defaultUsersArray = [currentUser, ...this.defaultUsersArray];
    }

    this.state = {
      query: '',
      users: this.defaultUsersArray,
      currentUser: currentUser.login
    };
  }

  searchMembers = (query: string) => {
    searchMembers({
      organization: this.props.issue.projectOrganization,
      q: query,
      ps: LIST_SIZE
    }).then(this.handleSearchResult, this.props.onFail);
  };

  searchUsers = (query: string) => {
    searchUsers(query, LIST_SIZE).then(this.handleSearchResult, this.props.onFail);
  };

  handleSearchResult = (data: Object) => {
    this.setState({
      users: data.users,
      currentUser: data.users.length > 0 ? data.users[0].login : ''
    });
  };

  handleSearchChange = (evt: SyntheticInputEvent) => {
    const query = evt.target.value;
    if (query.length < 2) {
      this.setState({
        query,
        users: this.defaultUsersArray,
        currentUser: this.defaultUsersArray[0].login
      });
    } else {
      this.setState({ query });
      if (this.organizationEnabled) {
        this.searchMembers(query);
      } else {
        this.searchUsers(query);
      }
    }
  };

  render() {
    return (
      <BubblePopup
        position={this.props.popupPosition}
        customClass="bubble-popup-menu bubble-popup-bottom">
        <div className="multi-select">
          <div className="search-box menu-search">
            <button className="search-box-submit button-clean">
              <i className="icon-search-new" />
            </button>
            <input
              type="search"
              value={this.state.query}
              className="search-box-input"
              placeholder={translate('search_verb')}
              onChange={this.handleSearchChange}
              autoComplete="off"
              autoFocus={true}
            />
          </div>
          <SelectList
            items={map(this.state.users, 'login')}
            currentItem={this.state.currentUser}
            onSelect={this.props.onSelect}>
            {this.state.users.map(user => (
              <SelectListItem key={user.login} item={user.login}>
                {!!user.login &&
                  <Avatar
                    className="spacer-right"
                    email={user.email}
                    hash={user.avatar}
                    name={user.name}
                    size={16}
                  />}
                <span
                  className="vertical-middle"
                  style={{ marginLeft: !user.login ? 24 : undefined }}>
                  {user.name}
                </span>
              </SelectListItem>
            ))}
          </SelectList>
        </div>
      </BubblePopup>
    );
  }
}
