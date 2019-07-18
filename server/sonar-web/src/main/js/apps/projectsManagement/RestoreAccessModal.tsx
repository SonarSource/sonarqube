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
import { FormattedMessage } from 'react-intl';
import { ResetButtonLink, SubmitButton } from 'sonar-ui-common/components/controls/buttons';
import Modal from 'sonar-ui-common/components/controls/Modal';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { Project } from '../../api/components';
import { grantPermissionToUser } from '../../api/permissions';

interface Props {
  currentUser: Pick<T.LoggedInUser, 'login'>;
  onClose: () => void;
  onRestoreAccess: () => void;
  project: Project;
}

interface State {
  loading: boolean;
}

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
      }
    );
  };

  grantPermission = (permission: string) =>
    grantPermissionToUser({
      projectKey: this.props.project.key,
      login: this.props.currentUser.login,
      permission,
      organization: this.props.project.organization
    });

  render() {
    const header = translate('global_permissions.restore_access');

    return (
      <Modal contentLabel={header} onRequestClose={this.props.onClose}>
        <form onSubmit={this.handleFormSubmit}>
          <header className="modal-head">
            <h2>{header}</h2>
          </header>

          <div className="modal-body">
            <FormattedMessage
              defaultMessage={translate('global_permissions.restore_access.message')}
              id="global_permissions.restore_access.message"
              values={{
                browse: <strong>{translate('projects_role.user')}</strong>,
                administer: <strong>{translate('projects_role.admin')}</strong>
              }}
            />
          </div>

          <footer className="modal-foot">
            {this.state.loading && <i className="spinner spacer-right" />}
            <SubmitButton disabled={this.state.loading}>{translate('restore')}</SubmitButton>
            <ResetButtonLink onClick={this.props.onClose}>{translate('cancel')}</ResetButtonLink>
          </footer>
        </form>
      </Modal>
    );
  }
}
