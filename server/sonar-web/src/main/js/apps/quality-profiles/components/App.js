/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import { getLanguages } from '../../../api/languages';
import {
    getQualityProfiles,
    getExporters
} from '../../../api/quality-profiles';
import { getCurrentUser } from '../../../api/users';
import '../styles.css';
import { sortProfiles } from '../utils';

export default class App extends React.Component {
  state = { loading: true };

  componentWillMount () {
    this.updateProfiles = this.updateProfiles.bind(this);
  }

  componentDidMount () {
    this.mounted = true;
    this.loadData();
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  loadData () {
    this.setState({ loading: true });
    Promise.all([
      getCurrentUser(),
      getLanguages(),
      getExporters(),
      getQualityProfiles()
    ]).then(responses => {
      if (this.mounted) {
        const [user, languages, exporters, profiles] = responses;
        const canAdmin = user.permissions.global.includes('profileadmin');
        this.setState({
          languages,
          exporters,
          canAdmin,
          profiles: sortProfiles(profiles),
          loading: false
        });
      }
    });
  }

  updateProfiles () {
    return getQualityProfiles().then(profiles => {
      if (this.mounted) {
        this.setState({ profiles: sortProfiles(profiles) });
      }
    });
  }

  renderChild () {
    if (this.state.loading) {
      return <i className="spinner"/>;
    }

    return React.cloneElement(this.props.children, {
      profiles: this.state.profiles,
      languages: this.state.languages,
      exporters: this.state.exporters,
      canAdmin: this.state.canAdmin,
      updateProfiles: this.updateProfiles
    });
  }

  render () {
    return (
        <div className="page page-limited-small">
          {this.renderChild()}
        </div>
    );
  }
}
