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
// @flow
import React from 'react';
import UsersSelectSearch from '../../../users/components/UsersSelectSearch';
import { searchMembers } from '../../../../api/organizations';
import Modal from '../../../../components/controls/Modal';
import { translate } from '../../../../helpers/l10n';
import { SubmitButton, ResetButtonLink, Button } from '../../../../components/ui/buttons';
/*:: import type { Organization } from '../../../../store/organizations/duck'; */
/*:: import type { Member } from '../../../../store/organizationsMembers/actions'; */

/*::
type Props = {
  addMember: (member: Member) => void,
  organization: Organization,
  memberLogins: Array<string>
};
*/

/*::
type State = {
  open: boolean,
  selectedMember?: Member
};
*/

export default class AddMemberForm extends React.PureComponent {
  /*:: props: Props; */

  state /*: State */ = {
    open: false
  };

  openForm = () => {
    this.setState({ open: true });
  };

  closeForm = () => {
    this.setState({ open: false, selectedMember: undefined });
  };

  handleSearch = (query /*: ?string */, ps /*: number */) => {
    const data = { organization: this.props.organization.key, ps, selected: 'deselected' };
    if (!query) {
      return searchMembers(data);
    }
    return searchMembers({ ...data, q: query });
  };

  handleSubmit = (e /*: Object */) => {
    e.preventDefault();
    if (this.state.selectedMember) {
      this.props.addMember(this.state.selectedMember);
      this.closeForm();
    }
  };

  selectedMemberChange = (member /*: Member */) => {
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
            <div className="modal-large-field">
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
    const buttonComponent = (
      <Button key="add-member-button" onClick={this.openForm}>
        {translate('organization.members.add')}
      </Button>
    );
    if (this.state.open) {
      return [buttonComponent, this.renderModal()];
    }
    return buttonComponent;
  }
}
