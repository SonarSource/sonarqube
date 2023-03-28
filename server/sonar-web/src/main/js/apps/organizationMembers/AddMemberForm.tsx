/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import {translate} from "../../helpers/l10n";
import Modal from "../../components/controls/Modal";
import {ResetButtonLink, SubmitButton} from "../../components/controls/buttons";
import Link from "../../components/common/Link";
import { Organization, OrganizationMember } from "../../types/types";
import { searchMembers } from "../../api/organizations";

interface Props {
  addMember: (member: OrganizationMember) => void;
  organization: Organization;
  memberLogins: string[];
}

interface State {
  open: boolean;
  selectedMember?: OrganizationMember;
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
    const data = { organization: this.props.organization.kee, ps, selected: 'deselected' };
    if (query && query.length >=2) {
      return searchMembers({ ...data, q: query });
    } else {
      return Promise.resolve({ paging: { pageIndex: 1, pageSize: 50, total: 0 }, users: [] });
    }
  };

  handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (this.state.selectedMember) {
      this.props.addMember(this.state.selectedMember);
      this.closeForm();
    }
  };

  selectedMemberChange = (member: OrganizationMember) => {
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
        <Link to={"/organizations/" + this.props.organization.kee + "/extension/developer/invite_users"} className="button little-spacer-left">
          Invite Member
        </Link>
        {this.state.open && this.renderModal()}
      </>
    );
  }
}
