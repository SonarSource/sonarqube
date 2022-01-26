/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import withLanguagesContext from '../../../app/components/languages/withLanguagesContext';
import { translate } from '../../../helpers/l10n';
import { Languages } from '../../../types/languages';
import '../styles.css';
import { Exporter, Profile } from '../types';
import { sortProfiles } from '../utils';

interface Props {
  children: React.ReactElement;
  languages: Languages;
}

interface State {
  actions?: Actions;
  loading: boolean;
  exporters?: Exporter[];
  profiles?: Profile[];
}

export class QualityProfilesApp extends React.PureComponent<Props, State> {
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
      ([exporters, profilesResponse]) => {
        if (this.mounted) {
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

export default withLanguagesContext(QualityProfilesApp);
