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
import * as React from 'react';
import { searchQualityProfiles, getExporters } from '../../../api/quality-profiles';
import { sortProfiles } from '../utils';
import { translate } from '../../../helpers/l10n';
import OrganizationHelmet from '../../../components/common/OrganizationHelmet';
import '../styles.css';
import { Exporter, Profile } from '../types';

interface Props {
  children: React.ReactElement<any>;
  currentUser: { permissions: { global: Array<string> } };
  languages: Array<{}>;
  onRequestFail: (reasong: any) => void;
  organization: { name: string; canAdmin?: boolean; key: string } | null;
}

interface State {
  loading: boolean;
  exporters?: Exporter[];
  profiles?: Profile[];
}

export default class App extends React.PureComponent<Props, State> {
  mounted: boolean;
  state: State = { loading: true };

  componentWillMount() {
    const html = document.querySelector('html');
    if (html) {
      html.classList.add('dashboard-page');
    }
  }

  componentDidMount() {
    this.mounted = true;
    this.loadData();
  }

  componentWillUnmount() {
    this.mounted = false;
    const html = document.querySelector('html');
    if (html) {
      html.classList.remove('dashboard-page');
    }
  }

  fetchProfiles() {
    const { organization } = this.props;
    const data = organization ? { organization: organization.key } : {};
    return searchQualityProfiles(data);
  }

  loadData() {
    this.setState({ loading: true });
    Promise.all([getExporters(), this.fetchProfiles()]).then(responses => {
      if (this.mounted) {
        const [exporters, profiles] = responses;
        this.setState({
          exporters,
          profiles: sortProfiles(profiles),
          loading: false
        });
      }
    });
  }

  updateProfiles = () => {
    return this.fetchProfiles().then((profiles: any) => {
      if (this.mounted) {
        this.setState({ profiles: sortProfiles(profiles) });
      }
    });
  };

  renderChild() {
    if (this.state.loading) {
      return <i className="spinner" />;
    }
    const { organization } = this.props;
    const finalLanguages = Object.values(this.props.languages);

    const canAdmin = organization
      ? organization.canAdmin
      : this.props.currentUser.permissions.global.includes('profileadmin');

    return React.cloneElement(this.props.children, {
      profiles: this.state.profiles,
      languages: finalLanguages,
      exporters: this.state.exporters,
      updateProfiles: this.updateProfiles,
      onRequestFail: this.props.onRequestFail,
      organization: organization ? organization.key : null,
      canAdmin
    });
  }

  render() {
    return (
      <div className="page page-limited">
        <OrganizationHelmet
          title={translate('quality_profiles.page')}
          organization={this.props.organization}
        />

        {this.renderChild()}
      </div>
    );
  }
}
