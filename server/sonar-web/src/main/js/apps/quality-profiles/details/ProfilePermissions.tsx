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
import * as React from 'react';
import { sortBy, uniqBy } from 'lodash';
import ProfilePermissionsUser from './ProfilePermissionsUser';
import ProfilePermissionsGroup from './ProfilePermissionsGroup';
import ProfilePermissionsForm from './ProfilePermissionsForm';
import {
  searchUsers,
  searchGroups,
  SearchUsersGroupsParameters
} from '../../../api/quality-profiles';
import { translate } from '../../../helpers/l10n';

export interface User {
  avatar?: string;
  login: string;
  name: string;
}

export interface Group {
  name: string;
}

interface Props {
  organization?: string;
  profile: { language: string; name: string };
}

interface State {
  addUserForm: boolean;
  groups?: Group[];
  loading: boolean;
  users?: User[];
}

export default class ProfilePermissions extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { addUserForm: false, loading: true };

  componentDidMount() {
    this.mounted = true;
    this.fetchUsersAndGroups();
  }

  componentDidUpdate(prevProps: Props) {
    if (
      prevProps.organization !== this.props.organization ||
      prevProps.profile !== this.props.profile
    ) {
      this.fetchUsersAndGroups();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchUsersAndGroups() {
    this.setState({ loading: true });
    const { organization, profile } = this.props;
    const parameters: SearchUsersGroupsParameters = {
      language: profile.language,
      organization,
      qualityProfile: profile.name,
      selected: 'selected'
    };
    Promise.all([searchUsers(parameters), searchGroups(parameters)]).then(
      ([usersResponse, groupsResponse]) => {
        if (this.mounted) {
          this.setState({
            groups: groupsResponse.groups,
            loading: false,
            users: usersResponse.users
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  }

  handleAddUserButtonClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState({ addUserForm: true });
  };

  handleAddUserFormClose = () => {
    if (this.mounted) {
      this.setState({ addUserForm: false });
    }
  };

  handleUserAdd = (addedUser: User) => {
    if (this.mounted) {
      this.setState((state: State) => ({
        addUserForm: false,
        users: state.users && uniqBy([...state.users, addedUser], user => user.login)
      }));
    }
  };

  handleUserDelete = (removedUser: User) => {
    if (this.mounted) {
      this.setState((state: State) => ({
        users: state.users && state.users.filter(user => user !== removedUser)
      }));
    }
  };

  handleGroupAdd = (addedGroup: Group) => {
    if (this.mounted) {
      this.setState((state: State) => ({
        addUserForm: false,
        groups: state.groups && uniqBy([...state.groups, addedGroup], group => group.name)
      }));
    }
  };

  handleGroupDelete = (removedGroup: Group) => {
    if (this.mounted) {
      this.setState((state: State) => ({
        groups: state.groups && state.groups.filter(group => group !== removedGroup)
      }));
    }
  };

  render() {
    return (
      <div className="boxed-group">
        <h2>{translate('permissions.page')}</h2>
        <div className="boxed-group-inner">
          <p className="note">{translate('quality_profiles.default_permissions')}</p>

          {this.state.loading ? (
            <div className="big-spacer-top">
              <i className="spinner" />
            </div>
          ) : (
            <div className="big-spacer-top">
              {this.state.users &&
                sortBy(this.state.users, 'name').map(user => (
                  <ProfilePermissionsUser
                    key={user.login}
                    onDelete={this.handleUserDelete}
                    organization={this.props.organization}
                    profile={this.props.profile}
                    user={user}
                  />
                ))}
              {this.state.groups &&
                sortBy(this.state.groups, 'name').map(group => (
                  <ProfilePermissionsGroup
                    group={group}
                    key={group.name}
                    onDelete={this.handleGroupDelete}
                    organization={this.props.organization}
                    profile={this.props.profile}
                  />
                ))}
              <div className="text-right">
                <button onClick={this.handleAddUserButtonClick}>
                  {translate('quality_profiles.grant_permissions_to_more_users')}
                </button>
              </div>
            </div>
          )}
        </div>

        {this.state.addUserForm && (
          <ProfilePermissionsForm
            onClose={this.handleAddUserFormClose}
            onGroupAdd={this.handleGroupAdd}
            onUserAdd={this.handleUserAdd}
            profile={this.props.profile}
            organization={this.props.organization}
          />
        )}
      </div>
    );
  }
}
