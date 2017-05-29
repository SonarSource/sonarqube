/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
// @flow
import React from 'react';
import CreateProfileView from '../views/CreateProfileView';
import RestoreProfileForm from './RestoreProfileForm';
import RestoreBuiltInProfilesView from '../views/RestoreBuiltInProfilesView';
import { getProfilePath } from '../utils';
import { translate } from '../../../helpers/l10n';
import { getImporters } from '../../../api/quality-profiles';

type Props = {
  canAdmin: boolean,
  languages: Array<{ key: string, name: string }>,
  onRequestFail: Object => void,
  organization: ?string,
  updateProfiles: () => Promise<*>
};

type State = {
  importers?: Array<{}>,
  restoreFormOpen: boolean
};

export default class PageHeader extends React.PureComponent {
  mounted: boolean;
  props: Props;

  static contextTypes = {
    router: React.PropTypes.object
  };

  state: State = {
    restoreFormOpen: false
  };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  retrieveImporters() {
    if (this.state.importers) {
      return Promise.resolve(this.state.importers);
    } else {
      return getImporters().then(importers => {
        this.setState({ importers });
        return importers;
      });
    }
  }

  handleCreateClick = (e: SyntheticInputEvent) => {
    e.preventDefault();
    e.target.blur();
    this.retrieveImporters().then(importers => {
      new CreateProfileView({
        languages: this.props.languages,
        organization: this.props.organization,
        importers
      })
        .on('done', profile => {
          this.props.updateProfiles().then(() => {
            this.context.router.push(
              getProfilePath(profile.name, profile.language, this.props.organization)
            );
          });
        })
        .render();
    });
  };

  handleRestoreClick = (event: Event) => {
    event.preventDefault();
    this.setState({ restoreFormOpen: true });
  };

  closeRestoreForm = () => {
    this.setState({ restoreFormOpen: false });
  };

  handleRestoreBuiltIn = (e: SyntheticInputEvent) => {
    e.preventDefault();
    new RestoreBuiltInProfilesView({
      languages: this.props.languages,
      organization: this.props.organization
    })
      .on('done', this.props.updateProfiles)
      .render();
  };

  render() {
    return (
      <header className="page-header">
        <h1 className="page-title">
          {translate('quality_profiles.page')}
        </h1>

        {this.props.canAdmin &&
          <div className="page-actions button-group dropdown">
            <button id="quality-profiles-create" onClick={this.handleCreateClick}>
              {translate('create')}
            </button>
            <button className="dropdown-toggle js-more-admin-actions" data-toggle="dropdown">
              <i className="icon-dropdown" />
            </button>
            <ul className="dropdown-menu dropdown-menu-right">
              <li>
                <a href="#" id="quality-profiles-restore" onClick={this.handleRestoreClick}>
                  {translate('quality_profiles.restore_profile')}
                </a>
              </li>

              <li>
                <a
                  href="#"
                  id="quality-profiles-restore-built-in"
                  onClick={this.handleRestoreBuiltIn}>
                  {translate('quality_profiles.restore_built_in_profiles')}
                </a>
              </li>
            </ul>
          </div>}

        <div className="page-description markdown">
          {translate('quality_profiles.intro1')}
          <br />
          {translate('quality_profiles.intro2')}
        </div>

        {this.state.restoreFormOpen &&
          <RestoreProfileForm
            onClose={this.closeRestoreForm}
            onRequestFail={this.props.onRequestFail}
            onRestore={this.props.updateProfiles}
            organization={this.props.organization}
          />}
      </header>
    );
  }
}
