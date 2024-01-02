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
import { addGroup, addUser } from '../../../api/quality-profiles';
import { ResetButtonLink, SubmitButton } from '../../../components/controls/buttons';
import Modal from '../../../components/controls/Modal';
import { translate } from '../../../helpers/l10n';
import { UserSelected } from '../../../types/types';
import { Group } from './ProfilePermissions';
import ProfilePermissionsFormSelect from './ProfilePermissionsFormSelect';

interface Props {
  onClose: () => void;
  onGroupAdd: (group: Group) => void;
  onUserAdd: (user: UserSelected) => void;
  profile: { language: string; name: string };
}

interface State {
  selected?: UserSelected | Group;
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

  handleUserAdd = (user: UserSelected) => {
    const {
      profile: { language, name },
    } = this.props;
    addUser({
      language,
      login: user.login,
      qualityProfile: name,
    }).then(() => this.props.onUserAdd(user), this.stopSubmitting);
  };

  handleGroupAdd = (group: Group) => {
    const {
      profile: { language, name },
    } = this.props;
    addGroup({
      group: group.name,
      language,
      qualityProfile: name,
    }).then(() => this.props.onGroupAdd(group), this.stopSubmitting);
  };

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { selected } = this.state;
    if (selected) {
      this.setState({ submitting: true });
      if ((selected as UserSelected).login !== undefined) {
        this.handleUserAdd(selected as UserSelected);
      } else {
        this.handleGroupAdd(selected as Group);
      }
    }
  };

  handleValueChange = (selected: UserSelected | Group) => {
    this.setState({ selected });
  };

  render() {
    const { profile } = this.props;
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
              <label htmlFor="change-profile-permission-input">
                {translate('quality_profiles.search_description')}
              </label>
              <ProfilePermissionsFormSelect onChange={this.handleValueChange} profile={profile} />
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
