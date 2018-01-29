/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { map } from 'lodash';
import { connect } from 'react-redux';
import * as PropTypes from 'prop-types';
import Avatar from '../../../components/ui/Avatar';
import BubblePopup from '../../../components/common/BubblePopup';
import SelectList from '../../../components/common/SelectList';
import SelectListItem from '../../../components/common/SelectListItem';
import SearchBox from '../../../components/controls/SearchBox';
import { searchMembers } from '../../../api/organizations';
import { searchUsers } from '../../../api/users';
import { translate } from '../../../helpers/l10n';
import { getCurrentUser } from '../../../store/rootReducer';
/*:: import type { Issue } from '../types'; */

/*::
type User = {
  avatar?: string,
  email?: string,
  login: string,
  name: string
};
*/

/*::
type Props = {
  currentUser: User,
  issue: Issue,
  onFail: Error => void,
  onSelect: string => void,
  popupPosition?: {}
};
*/

/*::
type State = {
  query: string,
  users: Array<User>,
  currentUser: string
};
*/

const LIST_SIZE = 10;

class SetAssigneePopup extends React.PureComponent {
  /*:: defaultUsersArray: Array<User>; */
  /*:: props: Props; */
  /*:: state: State; */

  static contextTypes = {
    organizationsEnabled: PropTypes.bool
  };

  constructor(props /*: Props */) {
    super(props);
    this.defaultUsersArray = [{ login: '', name: translate('unassigned') }];

    if (props.currentUser.isLoggedIn) {
      this.defaultUsersArray = [props.currentUser, ...this.defaultUsersArray];
    }

    this.state = {
      query: '',
      users: this.defaultUsersArray,
      currentUser: this.defaultUsersArray.length > 0 ? this.defaultUsersArray[0].login : ''
    };
  }

  searchMembers = (query /*: string */) => {
    searchMembers({
      organization: this.props.issue.projectOrganization,
      q: query,
      ps: LIST_SIZE
    }).then(this.handleSearchResult, this.props.onFail);
  };

  searchUsers = (query /*: string */) =>
    searchUsers({ q: query, ps: LIST_SIZE }).then(this.handleSearchResult, this.props.onFail);

  handleSearchResult = (data /*: Object */) => {
    this.setState({
      users: data.users,
      currentUser: data.users.length > 0 ? data.users[0].login : ''
    });
  };

  handleSearchChange = (query /*: string */) => {
    if (query.length === 0) {
      this.setState({
        query,
        users: this.defaultUsersArray,
        currentUser: this.defaultUsersArray[0].login
      });
    } else {
      this.setState({ query });
      if (this.context.organizationsEnabled) {
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
          <div className="menu-search">
            <SearchBox
              autoFocus={true}
              minLength={2}
              onChange={this.handleSearchChange}
              placeholder={translate('search.search_for_users')}
              value={this.state.query}
            />
          </div>
          <SelectList
            items={map(this.state.users, 'login')}
            currentItem={this.state.currentUser}
            onSelect={this.props.onSelect}>
            {this.state.users.map(user => (
              <SelectListItem key={user.login} item={user.login}>
                {!!user.login && (
                  <Avatar className="spacer-right" hash={user.avatar} name={user.name} size={16} />
                )}
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

const mapStateToProps = state => ({
  currentUser: getCurrentUser(state)
});

export default connect(mapStateToProps)(SetAssigneePopup);
