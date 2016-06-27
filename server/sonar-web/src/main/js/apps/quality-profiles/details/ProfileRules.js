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
import keyBy from 'lodash/keyBy';
import ProfileRulesRow from './ProfileRulesRow';
import { ProfileType } from '../propTypes';
import { TooltipsContainer } from '../../../components/mixins/tooltips-mixin';
import { searchRules, takeFacet } from '../../../api/rules';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';
import {
    getRulesUrl,
    getDeprecatedActiveRulesUrl
} from '../../../helpers/urls';

const TYPES = ['BUG', 'VULNERABILITY', 'CODE_SMELL'];

export default class ProfileRules extends React.Component {
  static propTypes = {
    profile: ProfileType.isRequired,
    canAdmin: React.PropTypes.bool.isRequired
  };

  state = {
    total: null,
    activatedTotal: null,
    allByType: keyBy(TYPES.map(t => ({ val: t, count: null })), 'val'),
    activatedByType: keyBy(TYPES.map(t => ({ val: t, count: null })), 'val')
  };

  componentDidMount () {
    this.mounted = true;
    this.loadRules();
  }

  componentDidUpdate (prevProps) {
    if (prevProps.profile !== this.props.profile) {
      this.loadRules();
    }
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  loadAllRules () {
    return searchRules({
      languages: this.props.profile.language,
      ps: 1,
      facets: 'types'
    });
  }

  loadActivatedRules () {
    return searchRules({
      qprofile: this.props.profile.key,
      activation: 'true',
      ps: 1,
      facets: 'types'
    });
  }

  loadRules () {
    Promise.all([
      this.loadAllRules(),
      this.loadActivatedRules()
    ]).then(responses => {
      if (this.mounted) {
        const [allRules, activatedRules] = responses;
        this.setState({
          total: allRules.total,
          activatedTotal: activatedRules.total,
          allByType: keyBy(takeFacet(allRules, 'types'), 'val'),
          activatedByType: keyBy(takeFacet(activatedRules, 'types'), 'val')
        });
      }
    });
  }

  getTooltip (count, total) {
    if (count == null || total == null) {
      return '';
    }

    return translateWithParameters(
        'quality_profiles.x_activated_out_of_y',
        formatMeasure(count, 'INT'),
        formatMeasure(total, 'INT'));
  }

  renderActiveTitle () {
    return (
        <strong>
          {translate('quality_profile.total_active_rules')}
        </strong>
    );
  }

  renderActiveCount () {
    const rulesUrl = getRulesUrl({
      qprofile: this.props.profile.key,
      activation: 'true'
    });

    if (this.state.activatedTotal == null) {
      return null;
    }

    return (
        <a href={rulesUrl}>
          <strong>
            {formatMeasure(this.state.activatedTotal, 'SHORT_INT')}
          </strong>
        </a>
    );
  }

  getTooltipForType (type) {
    const { count } = this.state.activatedByType[type];
    const total = this.state.allByType[type].count;
    return this.getTooltip(count, total);
  }

  renderTitleForType (type) {
    return <span>{translate('issue.type', type, 'plural')}</span>;
  }

  renderCountForType (type) {
    const rulesUrl = getRulesUrl({
      qprofile: this.props.profile.key,
      activation: 'true',
      types: type
    });

    const { count } = this.state.activatedByType[type];

    if (count == null) {
      return null;
    }

    return (
        <a href={rulesUrl}>
          {formatMeasure(count, 'SHORT_INT')}
        </a>
    );
  }

  renderDeprecated () {
    const { profile } = this.props;

    if (profile.activeDeprecatedRuleCount === 0) {
      return null;
    }

    const url = getDeprecatedActiveRulesUrl({ qprofile: profile.key });

    return (
        <div className="quality-profile-rules-deprecated clearfix">
          <div className="pull-left">
            {translate('quality_profiles.deprecated_rules')}
          </div>
          <div className="pull-right">
            <a href={url}>
              {profile.activeDeprecatedRuleCount}
            </a>
          </div>
        </div>
    );
  }

  render () {
    const { total, activatedTotal, allByType, activatedByType } = this.state;

    const activateMoreUrl = getRulesUrl({
      qprofile: this.props.profile.key,
      activation: 'false'
    });

    return (
        <div className="quality-profile-rules">
          <header className="clearfix">
            <h2 className="pull-left">{translate('rules')}</h2>

            {this.props.canAdmin && (
                <a href={activateMoreUrl}
                   className="button pull-right js-activate-rules">
                  {translate('quality_profiles.activate_more')}
                </a>
            )}
          </header>

          {this.renderDeprecated()}

          <TooltipsContainer options={{ delay: { show: 250, hide: 0 } }}>
            <ul className="quality-profile-rules-distribution">
              <li key="all" className="big-spacer-bottom">
                <ProfileRulesRow
                    count={activatedTotal}
                    total={total}
                    tooltip={this.getTooltip(activatedTotal, total)}
                    renderTitle={this.renderActiveTitle.bind(this)}
                    renderCount={this.renderActiveCount.bind(this)}/>
              </li>

              {TYPES.map(type => (
                  <li key={type} className="spacer-top">
                    <ProfileRulesRow
                        count={activatedByType[type].count}
                        total={allByType[type].count}
                        tooltip={this.getTooltipForType(type)}
                        renderTitle={this.renderTitleForType.bind(this, type)}
                        renderCount={this.renderCountForType.bind(this, type)}/>
                  </li>
              ))}
            </ul>
          </TooltipsContainer>
        </div>
    );
  }
}
