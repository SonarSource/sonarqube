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
import { getQualityProfiles, getExporters } from '../../../api/quality-profiles';
import { getCurrentUser } from '../../../api/users';
import '../styles.css';
import { sortProfiles } from '../utils';

export default class App extends React.Component {
  state = { loading: true };

  componentWillMount () {
    document.querySelector('html').classList.add('dashboard-page');
    this.updateProfiles = this.updateProfiles.bind(this);
  }

  componentDidMount () {
    this.mounted = true;
    this.loadData();
  }

  componentWillUnmount () {
    this.mounted = false;
    document.querySelector('html').classList.remove('dashboard-page');
  }

  loadData () {
    this.setState({ loading: true });
    Promise.all([
      getCurrentUser(),
      getExporters(),
      getQualityProfiles()
    ]).then(responses => {
      if (this.mounted) {
        const [user, exporters, profiles] = responses;
        const canAdmin = user.permissions.global.includes('profileadmin');
        this.setState({
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
    const areLanguagesLoading = Object.keys(this.props.languages).length === 0;
    if (this.state.loading || areLanguagesLoading) {
      return <i className="spinner"/>;
    }

    const finalLanguages = Object.values(this.props.languages);

    return React.cloneElement(this.props.children, {
      profiles: this.state.profiles,
      languages: finalLanguages,
      exporters: this.state.exporters,
      canAdmin: this.state.canAdmin,
      updateProfiles: this.updateProfiles
    });
  }

  render () {
    return (
        <div className="page page-limited">
          {this.renderChild()}
        </div>
    );
  }
}
