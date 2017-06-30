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
// @flow
import React from 'react';
import { Link } from 'react-router';
import { keyBy } from 'lodash';
import ProfileRulesRowOfType from './ProfileRulesRowOfType';
import ProfileRulesRowTotal from './ProfileRulesRowTotal';
import ProfileRulesDeprecatedWarning from './ProfileRulesDeprecatedWarning';
import ProfileRulesSonarWayComparison from './ProfileRulesSonarWayComparison';
import { searchRules, takeFacet } from '../../../api/rules';
import { getQualityProfiles } from '../../../api/quality-profiles';
import { getRulesUrl } from '../../../helpers/urls';
import { translate } from '../../../helpers/l10n';
import type { Profile } from '../propTypes';

const TYPES = ['BUG', 'VULNERABILITY', 'CODE_SMELL'];

type Props = {
  canAdmin: boolean,
  organization: ?string,
  profile: Profile
};

type State = {
  activatedTotal: ?number,
  activatedByType?: { [string]: ?{ val: string, count: ?number } },
  allByType?: { [string]: ?{ val: string, count: ?number } },
  compareToSonarWay: ?{ profile: string, profileName: string, missingRuleCount: number },
  loading: boolean,
  total: ?number
};

export default class ProfileRules extends React.PureComponent {
  mounted: boolean;
  props: Props;
  state: State = {
    activatedTotal: null,
    activatedByType: keyBy(TYPES.map(t => ({ val: t, count: null })), 'val'),
    allByType: keyBy(TYPES.map(t => ({ val: t, count: null })), 'val'),
    compareToSonarWay: null,
    loading: true,
    total: null
  };

  componentDidMount() {
    this.mounted = true;
    this.loadRules();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.profile !== this.props.profile) {
      this.loadRules();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadProfile() {
    if (this.props.profile.isBuiltIn) {
      return Promise.resolve(null);
    }
    return getQualityProfiles({
      compareToSonarWay: true,
      profile: this.props.profile.key
    });
  }

  loadAllRules() {
    return searchRules({
      languages: this.props.profile.language,
      ps: 1,
      facets: 'types'
    });
  }

  loadActivatedRules() {
    return searchRules({
      qprofile: this.props.profile.key,
      activation: 'true',
      ps: 1,
      facets: 'types'
    });
  }

  loadRules() {
    this.setState({ loading: true });
    Promise.all([
      this.loadAllRules(),
      this.loadActivatedRules(),
      this.loadProfile()
    ]).then(responses => {
      if (this.mounted) {
        const [allRules, activatedRules, showProfile] = responses;
        this.setState({
          activatedTotal: activatedRules.total,
          allByType: keyBy(takeFacet(allRules, 'types'), 'val'),
          activatedByType: keyBy(takeFacet(activatedRules, 'types'), 'val'),
          compareToSonarWay: showProfile && showProfile.compareToSonarWay,
          loading: false,
          total: allRules.total
        });
      }
    });
  }

  getRulesCountForType(type: string): ?number {
    return this.state.activatedByType && this.state.activatedByType[type]
      ? this.state.activatedByType[type].count
      : null;
  }

  getRulesTotalForType(type: string): ?number {
    return this.state.allByType && this.state.allByType[type]
      ? this.state.allByType[type].count
      : null;
  }

  render() {
    const { organization, profile } = this.props;
    const { compareToSonarWay } = this.state;
    const activateMoreUrl = getRulesUrl(
      { qprofile: profile.key, activation: 'false' },
      organization
    );

    return (
      <div className="quality-profile-rules">
        <div className="quality-profile-rules-distribution">
          <table className="data condensed">
            <thead>
              <tr>
                <th>
                  <h2>{translate('rules')}</h2>
                </th>
                <th>{translate('active')}</th>
                <th>{translate('inactive')}</th>
              </tr>
            </thead>
            <tbody>
              <ProfileRulesRowTotal
                key="all"
                count={this.state.activatedTotal}
                organization={organization}
                qprofile={profile.key}
                total={this.state.total}
              />
              {TYPES.map(type => (
                <ProfileRulesRowOfType
                  key={type}
                  count={this.getRulesCountForType(type)}
                  organization={organization}
                  qprofile={profile.key}
                  total={this.getRulesTotalForType(type)}
                  type={type}
                />
              ))}
            </tbody>
          </table>

          {this.props.canAdmin &&
            !profile.isBuiltIn &&
            <div className="text-right big-spacer-top">
              <Link to={activateMoreUrl} className="button js-activate-rules">
                {translate('quality_profiles.activate_more')}
              </Link>
            </div>}
        </div>
        {profile.activeDeprecatedRuleCount > 0 &&
          <ProfileRulesDeprecatedWarning
            activeDeprecatedRules={profile.activeDeprecatedRuleCount}
            organization={organization}
            profile={profile.key}
          />}
        {compareToSonarWay != null &&
          compareToSonarWay.missingRuleCount > 0 &&
          <ProfileRulesSonarWayComparison
            language={profile.language}
            organization={organization}
            profile={profile.key}
            sonarway={compareToSonarWay.profile}
            sonarWayMissingRules={compareToSonarWay.missingRuleCount}
          />}
      </div>
    );
  }
}
