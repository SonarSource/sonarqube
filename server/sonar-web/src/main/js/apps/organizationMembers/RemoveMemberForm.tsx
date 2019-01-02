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
import Modal from '../../components/controls/Modal';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { SubmitButton, ResetButtonLink } from '../../components/ui/buttons';

interface Props {
  onClose: () => void;
  member: T.OrganizationMember;
  organization: T.Organization;
  removeMember: (member: T.OrganizationMember) => void;
}

export default class RemoveMemberForm extends React.PureComponent<Props> {
  handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.props.removeMember(this.props.member);
    this.props.onClose();
  };

  render() {
    const header = translate('users.remove');
    return (
      <Modal contentLabel={header} key="remove-member-modal" onRequestClose={this.props.onClose}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>
        <form onSubmit={this.handleSubmit}>
          <div className="modal-body">
            {translateWithParameters(
              'organization.members.remove_x',
              this.props.member.name,
              this.props.organization.name
            )}
          </div>
          <footer className="modal-foot">
            <div>
              <SubmitButton autoFocus={true} className="button-red">
                {translate('remove')}
              </SubmitButton>
              <ResetButtonLink onClick={this.props.onClose}>{translate('cancel')}</ResetButtonLink>
            </div>
          </footer>
        </form>
      </Modal>
    );
  }
}
