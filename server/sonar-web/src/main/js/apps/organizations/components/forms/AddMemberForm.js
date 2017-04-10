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
import Modal from 'react-modal';
import UsersSelectSearch from '../../../users/components/UsersSelectSearch';
import { searchUsers } from '../../../../api/users';
import { translate } from '../../../../helpers/l10n';
import type { Member } from '../../../../store/organizationsMembers/actions';

type Props = {
  memberLogins: Array<string>,
  addMember: (member: Member) => void
};

type State = {
  open: boolean,
  selectedMember?: Member
};

export default class AddMemberForm extends React.PureComponent {
  props: Props;

  state: State = {
    open: false
  };

  openForm = () => {
    this.setState({ open: true });
  };

  closeForm = () => {
    this.setState({ open: false, selectedMember: undefined });
  };

  handleSubmit = (e: Object) => {
    e.preventDefault();
    if (this.state.selectedMember) {
      this.props.addMember(this.state.selectedMember);
      this.closeForm();
    }
  };

  selectedMemberChange = (member: Member) => {
    this.setState({ selectedMember: member });
  };

  renderModal() {
    return (
      <Modal
        isOpen={true}
        contentLabel="modal form"
        className="modal"
        overlayClassName="modal-overlay"
        onRequestClose={this.closeForm}>
        <header className="modal-head">
          <h2>{translate('users.add')}</h2>
        </header>
        <form onSubmit={this.handleSubmit}>
          <div className="modal-body">
            <div className="modal-large-field">
              <label>{translate('users.search_description')}</label>
              <UsersSelectSearch
                selectedUser={this.state.selectedMember}
                excludedUsers={this.props.memberLogins}
                searchUsers={searchUsers}
                handleValueChange={this.selectedMemberChange}
              />
            </div>
          </div>
          <footer className="modal-foot">
            <div>
              <button type="submit">{translate('organization.members.add_to_members')}</button>
              <button type="reset" className="button-link" onClick={this.closeForm}>
                {translate('cancel')}
              </button>
            </div>
          </footer>
        </form>
      </Modal>
    );
  }

  render() {
    return (
      <button onClick={this.openForm}>
        {translate('organization.members.add')}
        {this.state.open && this.renderModal()}
      </button>
    );
  }
}
