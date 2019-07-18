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
import { Button, ResetButtonLink, SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import Modal from 'sonar-ui-common/components/controls/Modal';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { searchMembers } from '../../api/organizations';
import UsersSelectSearch from '../users/components/UsersSelectSearch';

interface Props {
  addMember: (member: T.OrganizationMember) => void;
  organization: T.Organization;
  memberLogins: string[];
}

interface State {
  open: boolean;
  selectedMember?: T.OrganizationMember;
}

export default class AddMemberForm extends React.PureComponent<Props, State> {
  state: State = {
    open: false
  };

  openForm = () => {
    this.setState({ open: true });
  };

  closeForm = () => {
    this.setState({ open: false, selectedMember: undefined });
  };

  handleSearch = (query: string | undefined, ps: number) => {
    const data = { organization: this.props.organization.key, ps, selected: 'deselected' };
    if (!query) {
      return searchMembers(data);
    }
    return searchMembers({ ...data, q: query });
  };

  handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (this.state.selectedMember) {
      this.props.addMember(this.state.selectedMember);
      this.closeForm();
    }
  };

  selectedMemberChange = (member: T.OrganizationMember) => {
    this.setState({ selectedMember: member });
  };

  renderModal() {
    const header = translate('users.add');
    return (
      <Modal contentLabel={header} key="add-member-modal" onRequestClose={this.closeForm}>
        <header className="modal-head">
          <h2>{header}</h2>
        </header>
        <form onSubmit={this.handleSubmit}>
          <div className="modal-body">
            <div className="modal-field">
              <label>{translate('users.search_description')}</label>
              <UsersSelectSearch
                autoFocus={true}
                excludedUsers={this.props.memberLogins}
                handleValueChange={this.selectedMemberChange}
                searchUsers={this.handleSearch}
                selectedUser={this.state.selectedMember}
              />
            </div>
          </div>
          <footer className="modal-foot">
            <div>
              <SubmitButton disabled={!this.state.selectedMember}>
                {translate('organization.members.add_to_members')}
              </SubmitButton>
              <ResetButtonLink onClick={this.closeForm}>{translate('cancel')}</ResetButtonLink>
            </div>
          </footer>
        </form>
      </Modal>
    );
  }

  render() {
    return (
      <>
        <Button key="add-member-button" onClick={this.openForm}>
          {translate('organization.members.add')}
        </Button>
        {this.state.open && this.renderModal()}
      </>
    );
  }
}
