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
import * as React from 'react';
import { Alert } from '../../../components/ui/Alert';
import { translate } from '../../../helpers/l10n';
import { withQualityProfilesContext } from '../qualityProfilesContext';
import { Exporter, Profile } from '../types';
import ProfileExporters from './ProfileExporters';
import ProfileInheritance from './ProfileInheritance';
import ProfilePermissions from './ProfilePermissions';
import ProfileProjects from './ProfileProjects';
import ProfileRules from './ProfileRules';

export interface ProfileDetailsProps {
  exporters: Exporter[];
  profile: Profile;
  profiles: Profile[];
  updateProfiles: () => Promise<void>;
}

export function ProfileDetails(props: ProfileDetailsProps) {
  const { profile } = props;
  return (
    <div>
      <div className="quality-profile-grid">
        <div className="quality-profile-grid-left">
          <ProfileRules profile={profile} />
          <ProfileExporters exporters={props.exporters} profile={profile} />
          {profile.actions && profile.actions.edit && !profile.isBuiltIn && (
            <ProfilePermissions profile={profile} />
          )}
        </div>
        <div className="quality-profile-grid-right">
          {profile.activeRuleCount === 0 && (profile.projectCount || profile.isDefault) && (
            <Alert className="big-spacer-bottom" variant="warning">
              {profile.projectCount !== undefined &&
                profile.projectCount > 0 &&
                translate('quality_profiles.warning.used_by_projects_no_rules')}
              {!profile.projectCount &&
                profile.isDefault &&
                translate('quality_profiles.warning.is_default_no_rules')}
            </Alert>
          )}

          <ProfileInheritance
            profile={profile}
            profiles={props.profiles}
            updateProfiles={props.updateProfiles}
          />
          <ProfileProjects profile={profile} />
        </div>
      </div>
    </div>
  );
}

export default withQualityProfilesContext(ProfileDetails);
