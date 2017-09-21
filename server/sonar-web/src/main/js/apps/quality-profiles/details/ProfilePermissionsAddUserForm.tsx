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
import Modal from 'react-modal';
import UsersSelectSearch from '../../users/components/UsersSelectSearch';
import { searchUsers, addUser } from '../../../api/quality-profiles';
import { translate } from '../../../helpers/l10n';
import { User } from './ProfilePermissions';

interface Props {
  onAdd: (user: User) => void;
  onClose: () => void;
  organization?: string;
  profile: { language: string; name: string };
}

interface State {
  selected?: User;
  submitting: boolean;
}

export default class ProfilePermissionsAddUserForm extends React.PureComponent<Props, State> {
  state: State = { submitting: false };

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    const { selected } = this.state;
    if (selected) {
      this.setState({ submitting: true });
      const { organization, profile } = this.props;
      addUser({
        language: profile.language,
        organization,
        profile: profile.name,
        user: selected.login
      }).then(() => this.props.onAdd(selected), () => {});
    }
  };

  handleSearch = (q: string) => {
    const { organization, profile } = this.props;
    return searchUsers({
      language: profile.language,
      organization,
      profile: profile.name,
      q,
      selected: false
    });
  };

  handleValueChange = (selected: any) => {
    this.setState({ selected });
  };

  render() {
    const header = translate('quality_profiles.grant_permissions_to_user_or_group');
    const submitDisabled = !this.state.selected || this.state.submitting;
    return (
      <Modal
        isOpen={true}
        contentLabel={header}
        className="modal"
        overlayClassName="modal-overlay"
        onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>
        <form onSubmit={this.handleFormSubmit}>
          <div className="modal-body">
            <div className="modal-large-field">
              <label>{translate('quality_profiles.search_description')}</label>
              <UsersSelectSearch
                autoFocus={true}
                selectedUser={this.state.selected}
                searchUsers={this.handleSearch}
                handleValueChange={this.handleValueChange}
              />
            </div>
          </div>
          <footer className="modal-foot">
            {this.state.submitting && <i className="spinner spacer-right" />}
            <button disabled={submitDisabled} type="submit">
              {translate('add_verb')}
            </button>
            <button className="button-link" onClick={this.props.onClose} type="reset">
              {translate('cancel')}
            </button>
          </footer>
        </form>
      </Modal>
    );
  }
}
