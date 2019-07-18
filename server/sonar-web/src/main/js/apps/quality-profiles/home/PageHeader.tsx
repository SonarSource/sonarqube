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
import { Link } from 'react-router';
import { Button } from 'sonar-ui-common/components/controls/buttons';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { Actions } from '../../../api/quality-profiles';
import { Router, withRouter } from '../../../components/hoc/withRouter';
import { Profile } from '../types';
import { getProfilePath } from '../utils';
import CreateProfileForm from './CreateProfileForm';
import RestoreProfileForm from './RestoreProfileForm';

interface Props {
  actions: Actions;
  languages: Array<{ key: string; name: string }>;
  organization: string | null;
  profiles: Profile[];
  router: Pick<Router, 'push'>;
  updateProfiles: () => Promise<void>;
}

interface State {
  createFormOpen: boolean;
  restoreFormOpen: boolean;
}

export class PageHeader extends React.PureComponent<Props, State> {
  state: State = {
    createFormOpen: false,
    restoreFormOpen: false
  };

  handleCreateClick = () => {
    this.setState({ createFormOpen: true });
  };

  handleCreate = (profile: Profile) => {
    this.props.updateProfiles().then(
      () => {
        this.props.router.push(
          getProfilePath(profile.name, profile.language, this.props.organization)
        );
      },
      () => {}
    );
  };

  closeCreateForm = () => {
    this.setState({ createFormOpen: false });
  };

  handleRestoreClick = () => {
    this.setState({ restoreFormOpen: true });
  };

  closeRestoreForm = () => {
    this.setState({ restoreFormOpen: false });
  };

  render() {
    const { actions, languages, organization, profiles } = this.props;
    return (
      <header className="page-header">
        <h1 className="page-title">{translate('quality_profiles.page')}</h1>

        {actions.create && (
          <div className="page-actions">
            <Button
              disabled={languages.length === 0}
              id="quality-profiles-create"
              onClick={this.handleCreateClick}>
              {translate('create')}
            </Button>
            <Button
              className="little-spacer-left"
              id="quality-profiles-restore"
              onClick={this.handleRestoreClick}>
              {translate('restore')}
            </Button>
            {languages.length === 0 && (
              <Alert className="spacer-top" variant="warning">
                {translate('quality_profiles.no_languages_available')}
              </Alert>
            )}
          </div>
        )}

        <div className="page-description markdown">
          {translate('quality_profiles.intro1')}
          <br />
          {translate('quality_profiles.intro2')}
          <Link
            className="spacer-left"
            target="_blank"
            to={{
              pathname: '/documentation/instance-administration/quality-profiles/'
            }}>
            {translate('learn_more')}
          </Link>
        </div>

        {this.state.restoreFormOpen && (
          <RestoreProfileForm
            onClose={this.closeRestoreForm}
            onRestore={this.props.updateProfiles}
            organization={organization}
          />
        )}

        {this.state.createFormOpen && (
          <CreateProfileForm
            languages={languages}
            onClose={this.closeCreateForm}
            onCreate={this.handleCreate}
            organization={organization}
            profiles={profiles}
          />
        )}
      </header>
    );
  }
}

export default withRouter(PageHeader);
