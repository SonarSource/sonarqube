/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import { uniqBy } from 'lodash';
import ProfilePermissionsUser from './ProfilePermissionsUser';
import ProfilePermissionsAddUserForm from './ProfilePermissionsAddUserForm';
import { searchUsers } from '../../../api/quality-profiles';
import { translate } from '../../../helpers/l10n';

export interface User {
  avatar?: string;
  login: string;
  name: string;
}

interface Props {
  organization?: string;
  profile: { language: string; name: string };
}

interface State {
  addUserForm: boolean;
  loading: boolean;
  users?: User[];
}

export default class ProfilePermissions extends React.PureComponent<Props, State> {
  mounted: boolean;
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
    searchUsers({
      language: profile.language,
      organization,
      profile: profile.name,
      selected: true
    }).then(users => {
      if (this.mounted) {
        this.setState({ loading: false, users });
      }
    });
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
                this.state.users.map(user => (
                  <ProfilePermissionsUser
                    key={user.login}
                    onDelete={this.handleUserDelete}
                    organization={this.props.organization}
                    profile={this.props.profile}
                    user={user}
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
          <ProfilePermissionsAddUserForm
            onClose={this.handleAddUserFormClose}
            onAdd={this.handleUserAdd}
            profile={this.props.profile}
            organization={this.props.organization}
          />
        )}
      </div>
    );
  }
}
