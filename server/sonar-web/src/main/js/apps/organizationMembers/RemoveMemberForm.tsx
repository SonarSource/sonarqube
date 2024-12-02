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
import { Button, ButtonVariety } from '@sonarsource/echoes-react';
import { Modal } from 'design-system';
import * as React from 'react';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { Organization, OrganizationMember } from '../../types/types';

interface Props {
  onClose: () => void;
  member: OrganizationMember;
  organization: Organization;
  removeMember: (member: OrganizationMember) => void;
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
      <Modal
        headerTitle={header}
        onClose={this.props.onClose}
        body={
          <form id="remove-member-form" onSubmit={this.handleSubmit}>
            {translateWithParameters(
              'organization.members.remove_x',
              this.props.member.name,
              this.props.organization.name,
            )}
          </form>
        }
        primaryButton={
          <Button variety={ButtonVariety.Danger} type="submit" form="remove-member-form">
            {translate('remove')}
          </Button>
        }
        secondaryButtonLabel={translate('cancel')}
      />
    );
  }
}
