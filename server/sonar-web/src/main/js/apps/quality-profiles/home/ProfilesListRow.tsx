/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { Link } from 'react-router';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import { translate } from 'sonar-ui-common/helpers/l10n';
import DocTooltip from '../../../components/docs/DocTooltip';
import { getRulesUrl } from '../../../helpers/urls';
import BuiltInQualityProfileBadge from '../components/BuiltInQualityProfileBadge';
import ProfileActions from '../components/ProfileActions';
import ProfileDate from '../components/ProfileDate';
import ProfileLink from '../components/ProfileLink';
import { Profile } from '../types';

export interface ProfilesListRowProps {
  organization: string | null;
  profile: Profile;
  updateProfiles: () => Promise<void>;
}

export function ProfilesListRow(props: ProfilesListRowProps) {
  const { organization, profile } = props;

  const offset = 25 * (profile.depth - 1);
  const activeRulesUrl = getRulesUrl(
    {
      qprofile: profile.key,
      activation: 'true'
    },
    organization
  );
  const deprecatedRulesUrl = getRulesUrl(
    {
      qprofile: profile.key,
      activation: 'true',
      statuses: 'DEPRECATED'
    },
    organization
  );

  return (
    <tr
      className="quality-profiles-table-row text-middle"
      data-key={profile.key}
      data-name={profile.name}>
      <td className="quality-profiles-table-name text-middle">
        <div className="display-flex-center" style={{ paddingLeft: offset }}>
          <div>
            <ProfileLink
              language={profile.language}
              name={profile.name}
              organization={organization}>
              {profile.name}
            </ProfileLink>
          </div>
          {profile.isBuiltIn && <BuiltInQualityProfileBadge className="spacer-left" />}
        </div>
      </td>

      <td className="quality-profiles-table-projects thin nowrap text-middle text-right">
        {profile.isDefault ? (
          <DocTooltip
            doc={import(
              /* webpackMode: "eager" */ 'Docs/tooltips/quality-profiles/default-quality-profile.md'
            )}>
            <span className="badge">{translate('default')}</span>
          </DocTooltip>
        ) : (
          <span>{profile.projectCount}</span>
        )}
      </td>

      <td className="quality-profiles-table-rules thin nowrap text-middle text-right">
        <div>
          {profile.activeDeprecatedRuleCount > 0 && (
            <span className="spacer-right">
              <Tooltip overlay={translate('quality_profiles.deprecated_rules')}>
                <Link className="badge badge-error" to={deprecatedRulesUrl}>
                  {profile.activeDeprecatedRuleCount}
                </Link>
              </Tooltip>
            </span>
          )}

          <Link to={activeRulesUrl}>{profile.activeRuleCount}</Link>
        </div>
      </td>

      <td className="quality-profiles-table-date thin nowrap text-middle text-right">
        <ProfileDate date={profile.rulesUpdatedAt} />
      </td>

      <td className="quality-profiles-table-date thin nowrap text-middle text-right">
        <ProfileDate date={profile.lastUsed} />
      </td>

      <td className="quality-profiles-table-actions thin nowrap text-middle text-right">
        <ProfileActions
          fromList={true}
          organization={organization}
          profile={profile}
          updateProfiles={props.updateProfiles}
        />
      </td>
    </tr>
  );
}

export default React.memo(ProfilesListRow);
