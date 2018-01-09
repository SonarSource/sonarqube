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
import * as React from 'react';
import ChangeVisibilityForm from './ChangeVisibilityForm';
import { Organization, Visibility } from '../../app/types';
import { translate } from '../../helpers/l10n';
import { EditButton } from '../../components/ui/buttons';

export interface Props {
  hasProvisionPermission?: boolean;
  onProjectCreate: () => void;
  onVisibilityChange: (visibility: Visibility) => void;
  organization: Organization;
}

interface State {
  visibilityForm: boolean;
}

export default class Header extends React.PureComponent<Props, State> {
  state: State = { visibilityForm: false };

  handleCreateProjectClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    this.props.onProjectCreate();
  };

  handleChangeVisibilityClick = () => {
    this.setState({ visibilityForm: true });
  };

  closeVisiblityForm = () => {
    this.setState({ visibilityForm: false });
  };

  render() {
    const { organization } = this.props;

    return (
      <header className="page-header">
        <h1 className="page-title">{translate('projects_management')}</h1>

        <div className="page-actions">
          <span className="big-spacer-right">
            <span className="text-middle">
              {translate('organization.default_visibility_of_new_projects')}{' '}
              <strong>{translate('visibility', organization.projectVisibility)}</strong>
            </span>
            <EditButton
              className="js-change-visibility spacer-left button-small"
              onClick={this.handleChangeVisibilityClick}
            />
          </span>
          {this.props.hasProvisionPermission && (
            <button id="create-project" onClick={this.handleCreateProjectClick}>
              {translate('qualifiers.create.TRK')}
            </button>
          )}
        </div>

        <p className="page-description">{translate('projects_management.page.description')}</p>

        {this.state.visibilityForm && (
          <ChangeVisibilityForm
            onClose={this.closeVisiblityForm}
            onConfirm={this.props.onVisibilityChange}
            organization={organization}
          />
        )}
      </header>
    );
  }
}
