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
import { useIntl } from 'react-intl';
import { ActionCell, Badge, BaseLink, ContentCell, Link, Note, TableRow } from '~design-system';
import Tooltip from '../../../components/controls/Tooltip';
import DateFromNow from '../../../components/intl/DateFromNow';
import { getRulesUrl } from '../../../helpers/urls';
import BuiltInQualityProfileBadge from '../components/BuiltInQualityProfileBadge';
import ProfileActions from '../components/ProfileActions';
import ProfileLink from '../components/ProfileLink';
import { Profile } from '../types';

export interface ProfilesListRowProps {
  organization: string;
  isComparable: boolean;
  profile: Profile;
  updateProfiles: () => Promise<void>;
}

export function ProfilesListRow(props: Readonly<ProfilesListRowProps>) {
  const { organization, profile, isComparable } = props;
  const intl = useIntl();

  const offset = 24 * (profile.depth - 1);
  const activeRulesUrl = getRulesUrl({
    qprofile: profile.key,
    activation: 'true',
  }, organization);
  const deprecatedRulesUrl = getRulesUrl({
    qprofile: profile.key,
    activation: 'true',
    statuses: 'DEPRECATED',
  }, organization);

  return (
    <TableRow
      className="quality-profiles-table-row"
      data-key={profile.key}
      data-name={profile.name}
    >
      <ContentCell>
        <div className="sw-flex sw-items-center" style={{ paddingLeft: offset }}>
          <ProfileLink organization={organization} language={profile.language} name={profile.name}>
            {profile.name}
          </ProfileLink>
          {profile.isBuiltIn && <BuiltInQualityProfileBadge className="sw-ml-2" />}
        </div>
      </ContentCell>

      <ContentCell>
        {profile.isDefault ? (
          <Tooltip content={intl.formatMessage({ id: 'quality_profiles.list.default.help' })}>
            <Badge>{intl.formatMessage({ id: 'default' })}</Badge>
          </Tooltip>
        ) : (
          <Note>{profile.projectCount}</Note>
        )}
      </ContentCell>

      <ContentCell>
        <div>
          <Link to={activeRulesUrl}>{profile.activeRuleCount}</Link>

          {profile.activeDeprecatedRuleCount > 0 && (
            <span className="sw-ml-2">
              <Tooltip content={intl.formatMessage({ id: 'quality_profiles.deprecated_rules' })}>
                <BaseLink to={deprecatedRulesUrl} className="sw-border-0">
                  <Badge variant="deleted">{profile.activeDeprecatedRuleCount}</Badge>
                </BaseLink>
              </Tooltip>
            </span>
          )}
        </div>
      </ContentCell>

      <ContentCell>
        <Note>
          <DateFromNow date={profile.rulesUpdatedAt} />
        </Note>
      </ContentCell>

      <ContentCell>
        <Note>
          <DateFromNow date={profile.lastUsed} />
        </Note>
      </ContentCell>

      <ActionCell>
        <ProfileActions
          organization={organization}
          isComparable={isComparable}
          profile={profile}
          updateProfiles={props.updateProfiles}
        />
      </ActionCell>
    </TableRow>
  );
}

export default React.memo(ProfilesListRow);
