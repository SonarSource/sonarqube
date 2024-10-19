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
import { LargeCenteredLayout, Spinner } from 'design-system';
import * as React from 'react';
import { Helmet } from 'react-helmet-async';
import { Outlet } from 'react-router-dom';
import { Actions, getExporters, searchQualityProfiles } from '../../../api/quality-profiles';
import withLanguagesContext from '../../../app/components/languages/withLanguagesContext';
import Suggestions from '../../../components/embed-docs-modal/Suggestions';
import { DocLink } from '../../../helpers/doc-links';
import { translate } from '../../../helpers/l10n';
import { Languages } from '../../../types/languages';
import { QualityProfilesContextProps } from '../qualityProfilesContext';
import { Exporter, Profile } from '../types';
import { sortProfiles } from '../utils';
import { withOrganizationContext } from "../../organizations/OrganizationContext";
import { Organization } from "../../../types/types";

interface Props {
  organization: Organization;
  languages: Languages;
}

interface State {
  actions?: Actions;
  exporters?: Exporter[];
  loading: boolean;
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
    const { organization } = this.props;
    const data = organization ? { organization: organization.kee } : {};
    return searchQualityProfiles(data);
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
            loading: false,
          });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      },
    );
  }

  updateProfiles = () => {
    return this.fetchProfiles().then((r) => {
      if (this.mounted) {
        this.setState({ profiles: sortProfiles(r.profiles) });
      }
    });
  };

  renderChild() {
    const { actions, loading, profiles, exporters } = this.state;

    if (loading) {
      return <Spinner />;
    }
    const finalLanguages = Object.values(this.props.languages);

    const context: QualityProfilesContextProps = {
      actions: actions ?? {},
      profiles: profiles ?? [],
      languages: finalLanguages,
      exporters: exporters ?? [],
      updateProfiles: this.updateProfiles,
      organization: this.props.organization.kee,
    };

    return <Outlet context={context} />;
  }

  render() {
    return (
      <LargeCenteredLayout className="sw-my-8">
        <Suggestions suggestion={DocLink.InstanceAdminQualityProfiles} />
        <Helmet defer={false} title={translate('quality_profiles.page')} />

        {this.renderChild()}
      </LargeCenteredLayout>
    );
  }
}

export default withLanguagesContext(withOrganizationContext(QualityProfilesApp));
