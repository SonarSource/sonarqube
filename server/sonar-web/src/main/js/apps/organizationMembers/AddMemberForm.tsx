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
import { Button } from '@sonarsource/echoes-react';
import { Modal } from '~design-system';
import * as React from 'react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import withAppStateContext from '../../app/components/app-state/withAppStateContext';
import { translate } from '../../helpers/l10n';
import { AppState } from '../../types/appstate';
import { Organization, OrganizationMember } from '../../types/types';
import './AddMemberForm.css';
import CustomSearchInput from './SearchUser';

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
  const navigate = useNavigate();

  const goToInviteUsers = () => {
    navigate('/organizations/' + props.organization.kee + '/extension/developer/invite_users');
  };

  const closeForm = () => {
    setOpen(false);
    setSelectedMember(undefined);
  };

  const handleSubmit: any = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (selectedMember) {
      props.addMember(selectedMember);
      closeForm();
      window.location.reload();
    }
  };

  const selectedMemberChange = (member: OrganizationMember) => {
    setSelectedMember(member);
  };

  const handleUserSelect = (user: any) => {
    console.log('Selected user:', user); // Handle user select event
  };

  const renderModal = () => {
    const header = translate('users.add');
    return (
      <Modal
        headerTitle={header}
        key="add-member-form"
        onClose={closeForm}
        body={
          <div className="add-member-form-height">
            <label>{translate('users.search_description')}</label>
            <CustomSearchInput
              excludedUsers={props.memberLogins} // Pass the excluded users list
              handleValueChange={selectedMemberChange} // Pass the callback to handle value changes
              selectedUser={selectedMember} // Pass the current selected user
              organization={props.organization} // Pass the organization info
              onUserSelect={handleUserSelect} // Pass the callback for user selection
            />
          </div>
        }
        primaryButton={
          <Button type="submit" onClick={handleSubmit} isDisabled={!selectedMember}>
            {translate('organization.members.add_to_members')}
          </Button>
        }
        secondaryButtonLabel={translate('cancel')}
      />
    );
  };

  return (
    <>
      {(canAdmin || canCustomerAdmin) && (
        <Button key="add-member-button" onClick={openForm}>
          {translate('organization.members.add')}
        </Button>
      )}
      <Button onClick={goToInviteUsers} className="button sw-ml-2">
        Invite Member
      </Button>
      {open && renderModal()}
    </>
  );
}

export default withAppStateContext(AddMemberForm);
