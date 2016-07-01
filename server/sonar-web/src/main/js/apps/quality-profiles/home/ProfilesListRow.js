/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';
import shallowCompare from 'react-addons-shallow-compare';
import ProfileLink from '../components/ProfileLink';
import ProfileDate from '../components/ProfileDate';
import { ProfileType } from '../propTypes';
import { translate } from '../../../helpers/l10n';
import { getRulesUrl } from '../../../helpers/urls';

export default class ProfilesListRow extends React.Component {
  static propTypes = {
    profile: ProfileType.isRequired
  };

  shouldComponentUpdate (nextProps, nextState) {
    return shallowCompare(this, nextProps, nextState);
  }

  renderName () {
    const { profile } = this.props;
    const offset = 25 * (profile.depth - 1);
    return (
        <div style={{ paddingLeft: offset }}>
          <ProfileLink profileKey={profile.key}>
            {profile.name}
          </ProfileLink>
        </div>
    );
  }

  renderProjects () {
    const { profile } = this.props;

    if (profile.isDefault) {
      return (
          <span className="badge">
            {translate('default')}
          </span>
      );
    }

    return (
        <span>
          {profile.projectCount}
        </span>
    );
  }

  renderRules () {
    const { profile } = this.props;

    const activeRulesUrl = getRulesUrl({
      qprofile: profile.key,
      activation: 'true'
    });

    const deprecatedRulesUrl = getRulesUrl({
      qprofile: profile.key,
      activation: 'true',
      statuses: 'DEPRECATED'
    });

    return (
        <div>
          {profile.activeDeprecatedRuleCount > 0 && (
              <span className="spacer-right">
                <a className="badge badge-warning"
                   href={deprecatedRulesUrl}
                   title={translate('quality_profiles.deprecated_rules')}
                   data-toggle="tooltip">
                  {profile.activeDeprecatedRuleCount}
                </a>
              </span>
          )}

          <a href={activeRulesUrl}>
            {profile.activeRuleCount}
          </a>
        </div>
    );
  }

  renderUpdateDate () {
    return <ProfileDate date={this.props.profile.userUpdatedAt}/>;
  }

  renderUsageDate () {
    return <ProfileDate date={this.props.profile.lastUsed}/>;
  }

  render () {
    return (
        <tr className="quality-profiles-table-row"
            data-key={this.props.profile.key}
            data-name={this.props.profile.name}>
          <td className="quality-profiles-table-name">
            {this.renderName()}
          </td>
          <td className="quality-profiles-table-projects thin nowrap text-right">
            {this.renderProjects()}
          </td>
          <td className="quality-profiles-table-rules thin nowrap text-right">
            {this.renderRules()}
          </td>
          <td className="quality-profiles-table-date thin nowrap text-right">
            {this.renderUpdateDate()}
          </td>
          <td className="quality-profiles-table-date thin nowrap text-right">
            {this.renderUsageDate()}
          </td>
        </tr>
    );
  }
}
