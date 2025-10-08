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
import { addGlobalSuccessMessage, Modal } from '~design-system';
import * as React from 'react';
import { restoreArchivedOrganization } from '../../../api/organizations';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  organization: ArchivedOrganization;
  onClose?: () => void;
}

interface State {
  isOpen?: boolean;
}

export class OrganizationRestore extends React.PureComponent<Props, State> {

  state: State = { isOpen: true };

  closeRestorePopup = () => {
    this.setState({ isOpen: false });
    this.props.onClose();
  };

  onRestore = () => {
    const { organization } = this.props;
    return restoreArchivedOrganization(organization.kee)
      .then(() => {
        addGlobalSuccessMessage(translate('organization.restored'));
        window.location.reload();
    });
  };

  render() {
    return (
      <Modal
        isOpen={this.state.isOpen}
        onClose={this.closeRestorePopup}
        body={
          <div>
            <p>{translate('organization.restore.question')}</p>
          </div>
        }
        headerTitle={translateWithParameters(
          'organization.restore_x',
          this.props.organization.name,
        )}
        primaryButton={
          <Button
            variety={ButtonVariety.Primary}
            onClick={this.onRestore}
          >
            {translate('restore')}
          </Button>
        }
        secondaryButtonLabel={translate('close')}
      ></Modal>
    );
  }
}
