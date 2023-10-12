/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import styled from '@emotion/styled';
import { FlagMessage, themeColor } from 'design-system';
import * as React from 'react';
import { translate } from '../../../helpers/l10n';
import { withQualityProfilesContext } from '../qualityProfilesContext';
import { Exporter, Profile } from '../types';
import ProfileExporters from './ProfileExporters';
import ProfileInheritance from './ProfileInheritance';
import ProfilePermissions from './ProfilePermissions';
import ProfileProjects from './ProfileProjects';
import ProfileRules from './ProfileRules';

interface ProfileDetailsProps {
  exporters: Exporter[];
  profile: Profile;
  profiles: Profile[];
  updateProfiles: () => Promise<void>;
}

function ProfileDetails(props: ProfileDetailsProps) {
  const { profile, profiles, exporters } = props;

  return (
    <ContentWrapper>
      <div className="sw-grid sw-grid-cols-3 sw-gap-12 sw-mt-12">
        <div className="sw-col-span-2 sw-flex sw-flex-col sw-gap-12">
          {profile.activeRuleCount === 0 && (profile.projectCount || profile.isDefault) && (
            <FlagMessage variant="warning">
              {profile.projectCount !== undefined &&
                profile.projectCount > 0 &&
                translate('quality_profiles.warning.used_by_projects_no_rules')}
              {!profile.projectCount &&
                profile.isDefault &&
                translate('quality_profiles.warning.is_default_no_rules')}
            </FlagMessage>
          )}

          <ProfileInheritance
            profile={profile}
            profiles={profiles}
            updateProfiles={props.updateProfiles}
          />
          <ProfileProjects profile={profile} />
          {profile.actions?.edit && !profile.isBuiltIn && <ProfilePermissions profile={profile} />}
        </div>
        <div className="sw-flex sw-flex-col sw-gap-12">
          <ProfileRules profile={profile} />
          <ProfileExporters exporters={exporters} profile={profile} />
        </div>
      </div>
    </ContentWrapper>
  );
}

const ContentWrapper = styled.div`
  color: ${themeColor('pageContent')};
`;

export default withQualityProfilesContext(ProfileDetails);
