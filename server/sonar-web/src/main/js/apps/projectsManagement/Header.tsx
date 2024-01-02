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
import * as React from 'react';
import { Button, EditButton } from '../../components/controls/buttons';
import { translate } from '../../helpers/l10n';
import { Visibility } from '../../types/types';
import ChangeDefaultVisibilityForm from './ChangeDefaultVisibilityForm';

export interface Props {
  defaultProjectVisibility?: Visibility;
  hasProvisionPermission?: boolean;
  onProjectCreate: () => void;
  onChangeDefaultProjectVisibility: (visibility: Visibility) => void;
}

interface State {
  visibilityForm: boolean;
}

export default class Header extends React.PureComponent<Props, State> {
  state: State = { visibilityForm: false };

  handleChangeVisibilityClick = () => {
    this.setState({ visibilityForm: true });
  };

  closeVisiblityForm = () => {
    this.setState({ visibilityForm: false });
  };

  render() {
    const { defaultProjectVisibility, hasProvisionPermission } = this.props;
    const { visibilityForm } = this.state;

    return (
      <header className="page-header">
        <h1 className="page-title">{translate('projects_management')}</h1>

        <div className="page-actions">
          <span className="big-spacer-right">
            <span className="text-middle">
              {translate('settings.projects.default_visibility_of_new_projects')}{' '}
              <strong>
                {defaultProjectVisibility ? translate('visibility', defaultProjectVisibility) : '—'}
              </strong>
            </span>
            <EditButton
              className="js-change-visibility spacer-left button-small"
              onClick={this.handleChangeVisibilityClick}
            />
          </span>

          {hasProvisionPermission && (
            <Button id="create-project" onClick={this.props.onProjectCreate}>
              {translate('qualifiers.create.TRK')}
            </Button>
          )}
        </div>

        <p className="page-description">{translate('projects_management.page.description')}</p>

        {visibilityForm && (
          <ChangeDefaultVisibilityForm
            defaultVisibility={defaultProjectVisibility || 'public'}
            onClose={this.closeVisiblityForm}
            onConfirm={this.props.onChangeDefaultProjectVisibility}
          />
        )}
      </header>
    );
  }
}
