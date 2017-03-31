/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import { Link } from 'react-router';
import shallowCompare from 'react-addons-shallow-compare';
import ProfileLink from '../components/ProfileLink';
import ProfileDate from '../components/ProfileDate';
import ProfileActions from '../components/ProfileActions';
import { ProfileType } from '../propTypes';
import { translate } from '../../../helpers/l10n';
import { getRulesUrl } from '../../../helpers/urls';
import { isStagnant } from '../utils';

export default class ProfilesListRow extends React.Component {
  static propTypes = {
    profile: ProfileType.isRequired,
    canAdmin: React.PropTypes.bool.isRequired,
    updateProfiles: React.PropTypes.func.isRequired
  };

  shouldComponentUpdate(nextProps, nextState) {
    return shallowCompare(this, nextProps, nextState);
  }

  renderName() {
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

  renderProjects() {
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

  renderRules() {
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
        {profile.activeDeprecatedRuleCount > 0 &&
          <span className="spacer-right">
            <Link
              to={deprecatedRulesUrl}
              className="badge badge-normal-size badge-danger-light"
              title={translate('quality_profiles.deprecated_rules')}
              data-toggle="tooltip">
              {profile.activeDeprecatedRuleCount}
            </Link>
          </span>}

        <Link to={activeRulesUrl}>
          {profile.activeRuleCount}
        </Link>
      </div>
    );
  }

  renderUpdateDate() {
    const date = <ProfileDate date={this.props.profile.userUpdatedAt} />;
    if (isStagnant(this.props.profile)) {
      return <span className="badge badge-normal-size badge-focus">{date}</span>;
    } else {
      return date;
    }
  }

  renderUsageDate() {
    const { lastUsed } = this.props.profile;
    const date = <ProfileDate date={lastUsed} />;
    if (!lastUsed) {
      return <span className="badge badge-normal-size badge-focus">{date}</span>;
    } else {
      return date;
    }
  }

  render() {
    return (
      <tr
        className="quality-profiles-table-row"
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
        {this.props.canAdmin &&
          <td className="quality-profiles-table-actions thin nowrap text-right">
            <div className="dropdown">
              <button className="dropdown-toggle" data-toggle="dropdown">
                <i className="icon-dropdown" />
              </button>
              <ProfileActions
                profile={this.props.profile}
                canAdmin={this.props.canAdmin}
                updateProfiles={this.props.updateProfiles}
              />
            </div>
          </td>}
      </tr>
    );
  }
}
