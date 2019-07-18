/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { map } from 'lodash';
import * as React from 'react';
import { DropdownOverlay } from 'sonar-ui-common/components/controls/Dropdown';
import SearchBox from 'sonar-ui-common/components/controls/SearchBox';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { searchMembers } from '../../../api/organizations';
import { searchUsers } from '../../../api/users';
import { isSonarCloud } from '../../../helpers/system';
import { isLoggedIn, isUserActive } from '../../../helpers/users';
import SelectList from '../../common/SelectList';
import SelectListItem from '../../common/SelectListItem';
import { withCurrentUser } from '../../hoc/withCurrentUser';
import Avatar from '../../ui/Avatar';

interface Props {
  currentUser: T.CurrentUser;
  issue: Pick<T.Issue, 'projectOrganization'>;
  onSelect: (login: string) => void;
}

interface State {
  currentUser: string;
  query: string;
  users: T.UserActive[];
}

const LIST_SIZE = 10;

export class SetAssigneePopup extends React.PureComponent<Props, State> {
  defaultUsersArray: T.UserActive[];

  constructor(props: Props) {
    super(props);
    this.defaultUsersArray = [{ login: '', name: translate('unassigned') }];

    if (isLoggedIn(props.currentUser)) {
      this.defaultUsersArray = [props.currentUser, ...this.defaultUsersArray];
    }

    this.state = {
      query: '',
      users: this.defaultUsersArray,
      currentUser: this.defaultUsersArray.length > 0 ? this.defaultUsersArray[0].login : ''
    };
  }

  searchMembers = (query: string) => {
    searchMembers({
      organization: this.props.issue.projectOrganization,
      q: query,
      ps: LIST_SIZE
    }).then(this.handleSearchResult, () => {});
  };

  searchUsers = (query: string) => {
    searchUsers({ q: query, ps: LIST_SIZE }).then(this.handleSearchResult, () => {});
  };

  handleSearchResult = ({ users }: { users: T.UserBase[] }) => {
    const activeUsers = users.filter(isUserActive);
    this.setState({
      users: activeUsers,
      currentUser: activeUsers.length > 0 ? activeUsers[0].login : ''
    });
  };

  handleSearchChange = (query: string) => {
    if (query.length === 0) {
      this.setState({
        query,
        users: this.defaultUsersArray,
        currentUser: this.defaultUsersArray[0].login
      });
    } else {
      this.setState({ query });
      if (isSonarCloud()) {
        this.searchMembers(query);
      } else {
        this.searchUsers(query);
      }
    }
  };

  render() {
    return (
      <DropdownOverlay noPadding={true}>
        <div className="multi-select">
          <div className="menu-search">
            <SearchBox
              autoFocus={true}
              className="little-spacer-top"
              minLength={2}
              onChange={this.handleSearchChange}
              placeholder={translate('search.search_for_users')}
              value={this.state.query}
            />
          </div>
          <SelectList
            currentItem={this.state.currentUser}
            items={map(this.state.users, 'login')}
            onSelect={this.props.onSelect}>
            {this.state.users.map(user => (
              <SelectListItem item={user.login} key={user.login}>
                {!!user.login && (
                  <Avatar className="spacer-right" hash={user.avatar} name={user.name} size={16} />
                )}
                <span className="text-middle" style={{ marginLeft: user.login ? 24 : undefined }}>
                  {user.name}
                </span>
              </SelectListItem>
            ))}
          </SelectList>
        </div>
      </DropdownOverlay>
    );
  }
}

export default withCurrentUser(SetAssigneePopup);
