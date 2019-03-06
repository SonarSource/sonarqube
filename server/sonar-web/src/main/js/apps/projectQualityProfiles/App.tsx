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
import Helmet from 'react-helmet';
import Header from './Header';
import Table from './Table';
import {
  associateProject,
  dissociateProject,
  searchQualityProfiles,
  Profile
} from '../../api/quality-profiles';
import A11ySkipTarget from '../../app/components/a11y/A11ySkipTarget';
import Suggestions from '../../app/components/embed-docs-modal/Suggestions';
import addGlobalSuccessMessage from '../../app/utils/addGlobalSuccessMessage';
import handleRequiredAuthorization from '../../app/utils/handleRequiredAuthorization';
import { translate, translateWithParameters } from '../../helpers/l10n';

interface Props {
  component: T.Component;
}

interface State {
  allProfiles?: Profile[];
  loading: boolean;
  profiles?: Profile[];
}

export default class QualityProfiles extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    if (this.checkPermissions()) {
      this.fetchProfiles();
    } else {
      handleRequiredAuthorization();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  checkPermissions() {
    const { configuration } = this.props.component;
    const hasPermission = configuration && configuration.showQualityProfiles;
    return !!hasPermission;
  }

  fetchProfiles() {
    const { key, organization } = this.props.component;
    Promise.all([
      searchQualityProfiles({ organization }).then(r => r.profiles),
      searchQualityProfiles({ organization, project: key }).then(r => r.profiles)
    ]).then(
      ([allProfiles, profiles]) => {
        if (this.mounted) {
          this.setState({ loading: false, allProfiles, profiles });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  }

  handleChangeProfile = (oldKey: string, newKey: string) => {
    const { component } = this.props;
    const { allProfiles, profiles } = this.state;
    const newProfile = allProfiles && allProfiles.find(profile => profile.key === newKey);
    const request =
      newProfile && newProfile.isDefault
        ? dissociateProject(oldKey, component.key)
        : associateProject(newKey, component.key);

    return request.then(() => {
      if (this.mounted && profiles && newProfile) {
        // remove old profile, add new one
        const nextProfiles = [...profiles.filter(profile => profile.key !== oldKey), newProfile];
        this.setState({ profiles: nextProfiles });

        addGlobalSuccessMessage(
          translateWithParameters(
            'project_quality_profile.successfully_updated',
            newProfile.languageName
          )
        );
      }
    });
  };

  render() {
    if (!this.checkPermissions()) {
      return null;
    }

    const { allProfiles, loading, profiles } = this.state;

    return (
      <div className="page page-limited">
        <Suggestions suggestions="project_quality_profiles" />
        <Helmet title={translate('project_quality_profiles.page')} />

        <A11ySkipTarget anchor="profiles_main" />

        <Header />

        {loading ? (
          <i className="spinner" />
        ) : (
          allProfiles &&
          profiles && (
            <Table
              allProfiles={allProfiles}
              onChangeProfile={this.handleChangeProfile}
              profiles={profiles}
            />
          )
        )}
      </div>
    );
  }
}
