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
import * as React from 'react';
import { User, Group } from './ProfilePermissions';
import ProfilePermissionsFormSelect from './ProfilePermissionsFormSelect';
import {
  searchUsers,
  searchGroups,
  addUser,
  addGroup,
  SearchUsersGroupsParameters
} from '../../../api/quality-profiles';
import Modal from '../../../components/controls/Modal';
import { SubmitButton, ResetButtonLink } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';

interface Props {
  onClose: () => void;
  onGroupAdd: (group: Group) => void;
  onUserAdd: (user: User) => void;
  organization?: string;
  profile: { language: string; name: string };
}

interface State {
  selected?: User | Group;
  submitting: boolean;
}

export default class ProfilePermissionsForm extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { submitting: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  stopSubmitting = () => {
    if (this.mounted) {
      this.setState({ submitting: false });
    }
  };

  handleUserAdd = (user: User) =>
    addUser({
      language: this.props.profile.language,
      login: user.login,
      organization: this.props.organization,
      qualityProfile: this.props.profile.name
    }).then(() => this.props.onUserAdd(user), this.stopSubmitting);

  handleGroupAdd = (group: Group) =>
    addGroup({
      group: group.name,
      language: this.props.profile.language,
      organization: this.props.organization,
      qualityProfile: this.props.profile.name
    }).then(() => this.props.onGroupAdd(group), this.stopSubmitting);

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { selected } = this.state;
    if (selected) {
      this.setState({ submitting: true });
      if ((selected as User).login !== undefined) {
        this.handleUserAdd(selected as User);
      } else {
        this.handleGroupAdd(selected as Group);
      }
    }
  };

  handleSearch = (q: string) => {
    const { organization, profile } = this.props;
    const parameters: SearchUsersGroupsParameters = {
      language: profile.language,
      organization,
      q,
      qualityProfile: profile.name,
      selected: 'deselected'
    };
    return Promise.all([searchUsers(parameters), searchGroups(parameters)]).then(
      ([usersResponse, groupsResponse]) => [...usersResponse.users, ...groupsResponse.groups]
    );
  };

  handleValueChange = (selected: User | Group) => {
    this.setState({ selected });
  };

  render() {
    const header = translate('quality_profiles.grant_permissions_to_user_or_group');
    const submitDisabled = !this.state.selected || this.state.submitting;
    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>
        <form onSubmit={this.handleFormSubmit}>
          <div className="modal-body">
            <div className="modal-field">
              <label>{translate('quality_profiles.search_description')}</label>
              <ProfilePermissionsFormSelect
                onChange={this.handleValueChange}
                onSearch={this.handleSearch}
                selected={this.state.selected}
              />
            </div>
          </div>
          <footer className="modal-foot">
            {this.state.submitting && <i className="spinner spacer-right" />}
            <SubmitButton disabled={submitDisabled}>{translate('add_verb')}</SubmitButton>
            <ResetButtonLink onClick={this.props.onClose}>{translate('cancel')}</ResetButtonLink>
          </footer>
        </form>
      </Modal>
    );
  }
}
