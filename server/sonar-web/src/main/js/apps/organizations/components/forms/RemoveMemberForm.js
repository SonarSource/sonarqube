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
import { translate, translateWithParameters } from '../../../../helpers/l10n';
import type { Member } from '../../../../store/organizationsMembers/actions';
import type { Organization } from '../../../../store/organizations/duck';

type Props = {
  member: Member,
  organization: Organization,
  removeMember: (member: Member) => void
};

type State = {
  open: boolean
};

export default class RemoveMemberForm extends React.PureComponent {
  props: Props;

  state: State = {
    open: false
  };

  openForm = (evt: MouseEvent) => {
    evt.preventDefault();
    this.setState({ open: true });
  };

  closeForm = () => {
    this.setState({ open: false });
  };

  handleSubmit = (e: Object) => {
    e.preventDefault();
    this.props.removeMember(this.props.member);
    this.closeForm();
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
          <h2>{translate('users.remove')}</h2>
        </header>
        <form onSubmit={this.handleSubmit}>
          <div className="modal-body markdown">
            <p>
              {translateWithParameters(
                'organization.members.remove_x',
                this.props.member.name,
                this.props.organization.name
              )}
            </p>
          </div>
          <footer className="modal-foot">
            <div>
              <button type="submit" className="button-red" autoFocus={true}>
                {translate('remove')}
              </button>
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
      <a onClick={this.openForm} href="#">
        {translate('organization.members.remove')}
        {this.state.open && this.renderModal()}
      </a>
    );
  }
}
