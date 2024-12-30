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

import { differenceBy } from 'lodash';
import * as React from 'react';
import { addGlobalSuccessMessage } from '~design-system';
import {
  associateProject,
  dissociateProject,
  getProfileProjects,
  Profile,
  searchQualityProfiles,
} from '../../api/quality-profiles';
import withComponentContext from '../../app/components/componentContext/withComponentContext';
import handleRequiredAuthorization from '../../app/utils/handleRequiredAuthorization';
import { translateWithParameters } from '../../helpers/l10n';
import { isDefined } from '../../helpers/types';
import { Component, Organization } from '../../types/types';
import ProjectQualityProfilesAppRenderer from './ProjectQualityProfilesAppRenderer';
import { ProjectProfile } from './types';
import { withOrganizationContext } from "../organizations/OrganizationContext";

interface Props {
  organization: Organization;
  component: Component;
}

interface State {
  allProfiles?: Profile[];
  loading: boolean;
  projectProfiles?: ProjectProfile[];
  showAddLanguageModal?: boolean;
  showProjectProfileInModal?: ProjectProfile;
}

export class ProjectQualityProfilesApp extends React.PureComponent<Props, State> {
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
    const hasPermission = configuration?.showQualityProfiles;
    return !!hasPermission;
  }

  fetchProfiles = async () => {
    const { component, organization } = this.props;

    const allProfiles = await searchQualityProfiles({ organization: organization.kee })
      .then(({ profiles }) => profiles)
      .catch(() => [] as Profile[]);

    // We need to know if a profile was explicitly assigned to a project,
    // even if it's the system default. For this, we need to fetch the info
    // for each existing profile. We only keep those that were effectively
    // selected, and discard the rest.
    const projectProfiles = await Promise.all(
      allProfiles.map((profile) =>
        getProfileProjects({
          key: profile.key,
          q: component.name,
          selected: 'selected',
        })
          .then(({ results }) => ({
            selected: Boolean(results.find((p) => p.key === component.key)?.selected),
            profile,
          }))
          .catch(() => ({ selected: false, profile })),
      ),
    );

    const selectedProjectProfiles = projectProfiles
      .filter(({ selected }) => selected)
      .map(({ profile }) => ({
        profile,
        selected: true,
      }));

    // Finally, the project uses some profiles implicitly, either inheriting
    // from the system defaults, OR because the project wasn't re-analyzed
    // yet (in which case the info is outdated). We also need this information.
    const componentProfiles = differenceBy(
      component.qualityProfiles,
      selectedProjectProfiles.map((p) => p.profile),
      'key',
    )
      // Discard languages we already have up-to-date info for.
      .filter(
        ({ language }) => !selectedProjectProfiles.some((p) => p.profile.language === language),
      )
      .map(({ key }) => {
        const profile = allProfiles.find((p) => p.key === key);
        if (profile) {
          // If the profile is the default profile, all is good.
          if (profile.isDefault) {
            return { profile, selected: false };
          }

          // If it is neither the default, nor explicitly selected, it
          // means this is outdated information. This can only mean the
          // user wants to use the default profile, but it will only
          // be taken into account after a new analysis. Fetch the
          // default profile.
          const defaultProfile = allProfiles.find(
            (p) => p.isDefault && p.language === profile.language,
          );

          return (
            defaultProfile && {
              profile: defaultProfile,
              selected: false,
            }
          );
        }

        return undefined;
      })
      .filter(isDefined);

    if (this.mounted) {
      this.setState({
        allProfiles,
        projectProfiles: [...selectedProjectProfiles, ...componentProfiles],
        loading: false,
      });
    }
  };

  handleOpenSetProfileModal = (showProjectProfileInModal: ProjectProfile) => {
    this.setState({ showProjectProfileInModal });
  };

  handleOpenAddLanguageModal = () => {
    this.setState({ showAddLanguageModal: true });
  };

  handleCloseModal = () => {
    this.setState({ showAddLanguageModal: false, showProjectProfileInModal: undefined });
  };

  handleAddLanguage = async (key: string) => {
    const { component } = this.props;
    const { allProfiles = [] } = this.state;
    const newProfile = allProfiles.find((p) => p.key === key);

    if (newProfile) {
      try {
        await associateProject(newProfile, component.key);

        if (this.mounted) {
          this.setState(({ projectProfiles = [] }) => {
            const newProjectProfiles = [
              ...projectProfiles,
              {
                profile: newProfile,
                selected: true,
              },
            ];

            return { projectProfiles: newProjectProfiles, showAddLanguageModal: false };
          });

          addGlobalSuccessMessage(
            translateWithParameters(
              'project_quality_profile.successfully_updated',
              newProfile.languageName,
            ),
          );
        }
      } catch (e) {
        if (this.mounted) {
          this.setState({ showAddLanguageModal: false });
        }
      }
    }
  };

  handleSetProfile = async (newKey: string | undefined, oldKey: string) => {
    const { component } = this.props;
    const { allProfiles = [], projectProfiles = [] } = this.state;

    const newProfile = newKey !== undefined && allProfiles.find((p) => p.key === newKey);
    const oldProjectProfile = projectProfiles.find((p) => p.profile.key === oldKey);
    const defaultProfile = allProfiles.find(
      (p) => p.isDefault && p.language === oldProjectProfile?.profile.language,
    );

    if (defaultProfile === undefined || oldProjectProfile === undefined) {
      // Isn't possible. We're in a messed up state.
      return;
    }

    let replaceProfile: Profile | undefined;
    if (newProfile) {
      replaceProfile = newProfile;

      // Associate with the new profile.
      try {
        await associateProject(newProfile, component.key);
      } catch (e) {
        // Something went wrong. Keep the old profile in the UI.
        replaceProfile = oldProjectProfile.profile;
      }
    } else if (newKey === undefined) {
      replaceProfile = defaultProfile;

      // We want to use the system default. Explicitly dissociate the project
      // profile, if it was explicitly selected.
      if (oldProjectProfile.selected) {
        try {
          await dissociateProject(oldProjectProfile.profile, component.key);
        } catch (e) {
          // Something went wrong. Keep the old profile in the UI.
          replaceProfile = oldProjectProfile.profile;
        }
      }
    }

    if (this.mounted) {
      const newProjectProfiles = [
        // Remove the old profile.
        ...projectProfiles.filter((p) => p.profile.key !== oldKey),
        // Replace with the "new" profile.
        replaceProfile && {
          profile: replaceProfile,
          selected: newKey !== undefined,
        },
      ].filter(isDefined);

      this.setState({ projectProfiles: newProjectProfiles, showProjectProfileInModal: undefined });

      addGlobalSuccessMessage(
        translateWithParameters(
          'project_quality_profile.successfully_updated',
          defaultProfile.languageName,
        ),
      );
    }
  };

  render() {
    if (!this.checkPermissions()) {
      return null;
    }

    const {
      allProfiles,
      loading,
      showProjectProfileInModal,
      projectProfiles,
      showAddLanguageModal,
    } = this.state;

    return (
      <ProjectQualityProfilesAppRenderer
        allProfiles={allProfiles}
        component={this.props.component}
        loading={loading}
        onAddLanguage={this.handleAddLanguage}
        onCloseModal={this.handleCloseModal}
        onOpenAddLanguageModal={this.handleOpenAddLanguageModal}
        onOpenSetProfileModal={this.handleOpenSetProfileModal}
        onSetProfile={this.handleSetProfile}
        projectProfiles={projectProfiles}
        showAddLanguageModal={showAddLanguageModal}
        showProjectProfileInModal={showProjectProfileInModal}
      />
    );
  }
}

export default withComponentContext(withOrganizationContext(ProjectQualityProfilesApp));
