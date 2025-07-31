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
import { addGlobalSuccessMessage, InputField, Modal } from '~design-system';
import * as React from 'react';
import { deleteOrganization } from '../../../api/organizations';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  organization: ArchivedOrganization;
  onClose?: () => void;
}

interface State {
  verify: string;
  isOpen?: boolean;
}

export class OrganizationDelete extends React.PureComponent<Props, State> {

  state: State = { verify: '', isOpen: true };

  handleInput = (event: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ verify: event.currentTarget.value });
  };

  isVerified = () => {
    return this.state.verify.toLowerCase() === this.props.organization.name.toLowerCase();
  };

  closeDeletePopup = () => {
    this.setState({ isOpen: false });
    this.props.onClose();
  };

  onDelete = () => {
    const { organization } = this.props;
    return deleteOrganization(organization.kee)
      .then(() => {
        addGlobalSuccessMessage(translate('organization.deleted'));
        window.location.reload();
    });
  };

  render() {
    return (
      <Modal
        isOpen={this.state.isOpen}
        onClose={this.closeDeletePopup}
        body={
          <div>
            <p>{translate('organization.delete.question')}</p><br/>
            <p>{translate('organization.delete.warning')}</p>
            <div className="spacer-top sw-my-4">
              <label htmlFor="downgrade-organization-name">
                {translate('organization.delete.type_to_proceed')}
              </label>
              <div className="little-spacer-top sw-my-4">
                <InputField
                  autoFocus={true}
                  className="input-super-large"
                  id="downgrade-organization-name"
                  onChange={this.handleInput}
                  type="text"
                  value={this.state.verify}
                />
              </div>
            </div>
          </div>
        }
        headerTitle={translateWithParameters(
          'organization.delete_x',
          this.props.organization.name,
        )}
        primaryButton={
          <Button
            variety={ButtonVariety.Danger}
            isDisabled={!this.isVerified()}
            onClick={this.onDelete}
          >
            {translate('delete')}
          </Button>
        }
        secondaryButtonLabel={translate('close')}
      ></Modal>
    );
  }
}
