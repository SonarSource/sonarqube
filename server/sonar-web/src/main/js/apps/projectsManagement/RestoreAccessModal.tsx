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
import { FormattedMessage } from 'react-intl';
import { grantPermissionToUser } from '../../api/permissions';
import { Project } from '../../api/project-management';
import { translate } from '../../helpers/l10n';
import { LoggedInUser } from '../../types/users';

interface Props {
  currentUser: Pick<LoggedInUser, 'login'>;
  onClose: () => void;
  onRestoreAccess: () => void;
  project: Project;
}

interface State {
  loading: boolean;
}

const FORM_ID = 'restore-access-form';

export default class RestoreAccessModal extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleFormSubmit = (event: React.SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    this.setState({ loading: true });
    Promise.all([this.grantPermission('user'), this.grantPermission('admin')]).then(
      this.props.onRestoreAccess,
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      },
    );
  };

  grantPermission = (permission: string) =>
    grantPermissionToUser({
      projectKey: this.props.project.key,
      login: this.props.currentUser.login,
      permission,
    });

  render() {
    const { loading } = this.state;
    const header = translate('global_permissions.restore_access');

    return (
      <Modal
        headerTitle={header}
        onClose={this.props.onClose}
        loading={loading}
        body={
          <form id={FORM_ID} onSubmit={this.handleFormSubmit}>
            <FormattedMessage
              defaultMessage={translate('global_permissions.restore_access.message')}
              id="global_permissions.restore_access.message"
              values={{
                browse: <strong>{translate('projects_role.user')}</strong>,
                administer: <strong>{translate('projects_role.admin')}</strong>,
              }}
            />
          </form>
        }
        primaryButton={
          <Button
            hasAutoFocus
            isDisabled={loading}
            form={FORM_ID}
            type="submit"
            variety={ButtonVariety.Primary}
          >
            {translate('restore')}
          </Button>
        }
        secondaryButtonLabel={translate('cancel')}
      />
    );
  }
}
