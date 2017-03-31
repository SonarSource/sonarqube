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
import React from 'react';
import CreateProfileView from '../views/CreateProfileView';
import RestoreProfileView from '../views/RestoreProfileView';
import RestoreBuiltInProfilesView from '../views/RestoreBuiltInProfilesView';
import { translate } from '../../../helpers/l10n';
import { getImporters } from '../../../api/quality-profiles';

export default class PageHeader extends React.Component {
  static propTypes = {
    canAdmin: React.PropTypes.bool.isRequired
  };

  static contextTypes = {
    router: React.PropTypes.object
  };

  state = {};

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

  handleCreateClick(e) {
    e.preventDefault();
    e.target.blur();
    this.retrieveImporters().then(importers => {
      new CreateProfileView({
        languages: this.props.languages,
        importers
      })
        .on('done', profile => {
          this.props.updateProfiles().then(() => {
            this.context.router.push({
              pathname: '/profiles/show',
              query: { key: profile.key }
            });
          });
        })
        .render();
    });
  }

  handleRestoreClick(e) {
    e.preventDefault();
    new RestoreProfileView().on('done', this.props.updateProfiles).render();
  }

  handleRestoreBuiltIn(e) {
    e.preventDefault();
    new RestoreBuiltInProfilesView({ languages: this.props.languages })
      .on('done', this.props.updateProfiles)
      .render();
  }

  render() {
    return (
      <header className="page-header">
        <h1 className="page-title">
          {translate('quality_profiles.page')}
        </h1>

        {this.props.canAdmin &&
          <div className="page-actions button-group dropdown">
            <button id="quality-profiles-create" onClick={this.handleCreateClick.bind(this)}>
              {translate('create')}
            </button>
            <button className="dropdown-toggle js-more-admin-actions" data-toggle="dropdown">
              <i className="icon-dropdown" />
            </button>
            <ul className="dropdown-menu dropdown-menu-right">
              <li>
                <a
                  href="#"
                  id="quality-profiles-restore"
                  onClick={this.handleRestoreClick.bind(this)}>
                  {translate('quality_profiles.restore_profile')}
                </a>
              </li>

              <li>
                <a
                  href="#"
                  id="quality-profiles-restore-built-in"
                  onClick={this.handleRestoreBuiltIn.bind(this)}>
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
      </header>
    );
  }
}
