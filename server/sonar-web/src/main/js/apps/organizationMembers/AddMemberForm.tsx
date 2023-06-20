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
import { useState } from 'react';
import { translate } from "../../helpers/l10n";
import Modal from "../../components/controls/Modal";
import { Button, ResetButtonLink, SubmitButton } from "../../components/controls/buttons";
import Link from "../../components/common/Link";
import { Organization, OrganizationMember } from "../../types/types";
import withAppStateContext from "../../app/components/app-state/withAppStateContext";
import { AppState } from "../../types/appstate";
import UsersSelectSearch from "./UsersSelectSearch";

interface AddMemberFormProps {
  appState: AppState;
  addMember: (member: OrganizationMember) => void;
  organization: Organization;
  memberLogins: string[];
}

function AddMemberForm(props: AddMemberFormProps) {

  const { canAdmin, canCustomerAdmin } = props.appState;
  const [open, setOpen] = useState<boolean>();
  const [selectedMember, setSelectedMember] = useState<OrganizationMember>();

  const openForm = () => {
    setOpen(true);
  };

  const closeForm = () => {
    setOpen(false);
    setSelectedMember(undefined);
  };

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (selectedMember) {
      props.addMember(selectedMember);
      closeForm();
    }
  };

  const selectedMemberChange = (member: OrganizationMember) => {
    setSelectedMember(member);
  };

  const renderModal = () => {
    const header = translate('users.add');
    return (
        <Modal contentLabel={header} key="add-member-modal" onRequestClose={closeForm}>
          <header className="modal-head">
            <h2>{header}</h2>
          </header>
          <form onSubmit={handleSubmit}>
            <div className="modal-body">
              <div className="modal-field">
                <label>{translate('users.search_description')}</label>
                <UsersSelectSearch
                    autoFocus={true}
                    excludedUsers={props.memberLogins}
                    handleValueChange={selectedMemberChange}
                    selectedUser={selectedMember}
                    organization={props.organization}
                />
              </div>
            </div>
            <footer className="modal-foot">
              <div>
                <SubmitButton disabled={!selectedMember}>
                  {translate('organization.members.add_to_members')}
                </SubmitButton>
                <ResetButtonLink onClick={closeForm}>{translate('cancel')}</ResetButtonLink>
              </div>
            </footer>
          </form>
        </Modal>
    );
  }

  return (
      <>
        {(canAdmin || canCustomerAdmin) && (
            <Button key="add-member-button" onClick={openForm}>
              {translate('organization.members.add')}
            </Button>
        )}
        <Link to={"/organizations/" + props.organization.kee + "/extension/developer/invite_users"}
              className="button little-spacer-left">
          Invite Member
        </Link>
        {open && renderModal()}
      </>
  );
}

export default withAppStateContext(AddMemberForm);
