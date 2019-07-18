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
import ActionsDropdown, {
  ActionsDropdownItem
} from 'sonar-ui-common/components/controls/ActionsDropdown';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { Project } from '../../api/components';
import { getComponentNavigation } from '../../api/nav';
import { getComponentPermissionsUrl } from '../../helpers/urls';
import ApplyTemplate from '../permissions/project/components/ApplyTemplate';
import RestoreAccessModal from './RestoreAccessModal';

export interface Props {
  currentUser: Pick<T.LoggedInUser, 'login'>;
  organization: string | undefined;
  project: Project;
}

interface State {
  applyTemplateModal: boolean;
  hasAccess?: boolean;
  loading: boolean;
  restoreAccessModal: boolean;
}

export default class ProjectRowActions extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { applyTemplateModal: false, loading: false, restoreAccessModal: false };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchPermissions = () => {
    this.setState({ loading: true });
    getComponentNavigation({ component: this.props.project.key }).then(
      ({ configuration }) => {
        if (this.mounted) {
          const hasAccess = Boolean(configuration && configuration.showPermissions);
          this.setState({ hasAccess, loading: false });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  handleDropdownOpen = () => {
    if (this.state.hasAccess === undefined && !this.state.loading) {
      this.fetchPermissions();
    }
  };

  handleApplyTemplateClick = () => {
    this.setState({ applyTemplateModal: true });
  };

  handleApplyTemplateClose = () => {
    if (this.mounted) {
      this.setState({ applyTemplateModal: false });
    }
  };

  handleRestoreAccessClick = () => {
    this.setState({ restoreAccessModal: true });
  };

  handleRestoreAccessClose = () => this.setState({ restoreAccessModal: false });

  handleRestoreAccessDone = () => {
    this.setState({ hasAccess: true, restoreAccessModal: false });
  };

  render() {
    const { hasAccess, loading } = this.state;

    return (
      <>
        <ActionsDropdown onOpen={this.handleDropdownOpen}>
          {loading ? (
            <ActionsDropdownItem>
              <DeferredSpinner />
            </ActionsDropdownItem>
          ) : (
            <>
              {hasAccess === true && (
                <ActionsDropdownItem
                  className="js-edit-permissions"
                  to={getComponentPermissionsUrl(this.props.project.key)}>
                  {translate('edit_permissions')}
                </ActionsDropdownItem>
              )}

              {hasAccess === false && (
                <ActionsDropdownItem
                  className="js-restore-access"
                  onClick={this.handleRestoreAccessClick}>
                  {translate('global_permissions.restore_access')}
                </ActionsDropdownItem>
              )}
            </>
          )}

          <ActionsDropdownItem
            className="js-apply-template"
            onClick={this.handleApplyTemplateClick}>
            {translate('projects_role.apply_template')}
          </ActionsDropdownItem>
        </ActionsDropdown>

        {this.state.restoreAccessModal && (
          <RestoreAccessModal
            currentUser={this.props.currentUser}
            onClose={this.handleRestoreAccessClose}
            onRestoreAccess={this.handleRestoreAccessDone}
            project={this.props.project}
          />
        )}

        {this.state.applyTemplateModal && (
          <ApplyTemplate
            onClose={this.handleApplyTemplateClose}
            organization={this.props.organization}
            project={this.props.project}
          />
        )}
      </>
    );
  }
}
