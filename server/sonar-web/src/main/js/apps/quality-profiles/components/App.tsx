/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import { Helmet } from 'react-helmet-async';
import { Actions, getExporters, searchQualityProfiles } from '../../../api/quality-profiles';
import Suggestions from '../../../app/components/embed-docs-modal/Suggestions';
import { translate } from '../../../sonar-ui-common/helpers/l10n';
import '../styles.css';
import { Exporter, Profile } from '../types';
import { sortProfiles } from '../utils';

interface Props {
  children: React.ReactElement<any>;
  languages: T.Languages;
}

interface State {
  actions?: Actions;
  loading: boolean;
  exporters?: Exporter[];
  profiles?: Profile[];
}

export default class App extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.loadData();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchProfiles() {
    return searchQualityProfiles();
  }

  loadData() {
    this.setState({ loading: true });
    Promise.all([getExporters(), this.fetchProfiles()]).then(
      responses => {
        if (this.mounted) {
          const [exporters, profilesResponse] = responses;
          this.setState({
            actions: profilesResponse.actions,
            exporters,
            profiles: sortProfiles(profilesResponse.profiles),
            loading: false
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  }

  updateProfiles = () => {
    return this.fetchProfiles().then(r => {
      if (this.mounted) {
        this.setState({ profiles: sortProfiles(r.profiles) });
      }
    });
  };

  renderChild() {
    if (this.state.loading) {
      return <i className="spinner" />;
    }
    const finalLanguages = Object.values(this.props.languages);

    return React.cloneElement(this.props.children, {
      actions: this.state.actions || {},
      profiles: this.state.profiles || [],
      languages: finalLanguages,
      exporters: this.state.exporters,
      updateProfiles: this.updateProfiles
    });
  }

  render() {
    return (
      <div className="page page-limited">
        <Suggestions suggestions="quality_profiles" />
        <Helmet defer={false} title={translate('quality_profiles.page')} />

        {this.renderChild()}
      </div>
    );
  }
}
