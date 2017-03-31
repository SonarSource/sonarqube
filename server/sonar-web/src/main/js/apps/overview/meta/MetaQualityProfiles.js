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
import { connect } from 'react-redux';
import { Link } from 'react-router';
import { TooltipsContainer } from '../../../components/mixins/tooltips-mixin';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getQualityProfileUrl } from '../../../helpers/urls';
import { searchRules } from '../../../api/rules';
import { getLanguages } from '../../../store/rootReducer';

class MetaQualityProfiles extends React.Component {
  state = {
    deprecatedByKey: {}
  };

  componentDidMount() {
    this.mounted = true;
    this.loadDeprecatedRules();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadDeprecatedRules() {
    const requests = this.props.profiles.map(profile =>
      this.loadDeprecatedRulesForProfile(profile.key));
    Promise.all(requests).then(responses => {
      if (this.mounted) {
        const deprecatedByKey = {};
        responses.forEach((count, i) => {
          const profileKey = this.props.profiles[i].key;
          deprecatedByKey[profileKey] = count;
        });
        this.setState({ deprecatedByKey });
      }
    });
  }

  loadDeprecatedRulesForProfile(profileKey) {
    const data = {
      qprofile: profileKey,
      activation: 'true',
      statuses: 'DEPRECATED',
      ps: 1
    };
    return searchRules(data).then(r => r.total);
  }

  getDeprecatedRulesCount(profile) {
    const count = this.state.deprecatedByKey[profile.key];
    return count || 0;
  }

  renderProfile(profile) {
    const languageFromStore = this.props.languages[profile.language];
    const languageName = languageFromStore ? languageFromStore.name : profile.language;

    const inner = (
      <div className="text-ellipsis">
        <span className="note spacer-right">
          {'(' + languageName + ')'}
        </span>
        <Link to={getQualityProfileUrl(profile.key)}>
          {profile.name}
        </Link>
      </div>
    );

    const count = this.getDeprecatedRulesCount(profile);

    if (count > 0) {
      const tooltip = translateWithParameters('overview.deprecated_profile', count);
      return (
        <li
          key={profile.key}
          className="overview-deprecated-rules"
          title={tooltip}
          data-toggle="tooltip">
          {inner}
        </li>
      );
    }

    return (
      <li key={profile.key}>
        {inner}
      </li>
    );
  }

  render() {
    const { profiles } = this.props;

    return (
      <TooltipsContainer>
        <div className="overview-meta-card">
          <h4 className="overview-meta-header">
            {translate('overview.quality_profiles')}
          </h4>

          <ul className="overview-meta-list">
            {profiles.map(profile => this.renderProfile(profile))}
          </ul>
        </div>
      </TooltipsContainer>
    );
  }
}

const mapStateToProps = state => ({
  languages: getLanguages(state)
});

export default connect(mapStateToProps)(MetaQualityProfiles);
