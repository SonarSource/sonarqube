/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { connect } from 'react-redux';
import { Link } from 'react-router';
import * as classNames from 'classnames';
import Tooltip from '../../../components/controls/Tooltip';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getQualityProfileUrl } from '../../../helpers/urls';
import { searchRules } from '../../../api/rules';
import { getLanguages } from '../../../store/rootReducer';

interface StateProps {
  languages: { [key: string]: { name: string } };
}

interface OwnProps {
  headerClassName?: string;
  organization?: string;
  profiles: { key: string; language: string; name: string }[];
}

interface State {
  deprecatedByKey: { [key: string]: number };
}

class MetaQualityProfiles extends React.PureComponent<StateProps & OwnProps, State> {
  mounted = false;
  state: State = { deprecatedByKey: {} };

  componentDidMount() {
    this.mounted = true;
    this.loadDeprecatedRules();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadDeprecatedRules() {
    const requests = this.props.profiles.map(profile =>
      this.loadDeprecatedRulesForProfile(profile.key)
    );
    Promise.all(requests).then(
      responses => {
        if (this.mounted) {
          const deprecatedByKey: { [key: string]: number } = {};
          responses.forEach((count, i) => {
            const profileKey = this.props.profiles[i].key;
            deprecatedByKey[profileKey] = count;
          });
          this.setState({ deprecatedByKey });
        }
      },
      () => {}
    );
  }

  loadDeprecatedRulesForProfile(profileKey: string) {
    const data = {
      activation: 'true',
      organization: this.props.organization,
      ps: 1,
      qprofile: profileKey,
      statuses: 'DEPRECATED'
    };
    return searchRules(data).then(r => r.total);
  }

  getDeprecatedRulesCount(profile: { key: string }) {
    const count = this.state.deprecatedByKey[profile.key];
    return count || 0;
  }

  renderProfile(profile: { key: string; language: string; name: string }) {
    const languageFromStore = this.props.languages[profile.language];
    const languageName = languageFromStore ? languageFromStore.name : profile.language;

    const path = getQualityProfileUrl(profile.name, profile.language, this.props.organization);

    const inner = (
      <div className="text-ellipsis">
        <span className="note spacer-right">{'(' + languageName + ')'}</span>
        <Link to={path}>{profile.name}</Link>
      </div>
    );

    const count = this.getDeprecatedRulesCount(profile);

    if (count > 0) {
      const tooltip = translateWithParameters('overview.deprecated_profile', count);
      return (
        <Tooltip key={profile.key} overlay={tooltip}>
          <li className="overview-deprecated-rules">{inner}</li>
        </Tooltip>
      );
    }

    return <li key={profile.key}>{inner}</li>;
  }

  render() {
    const { headerClassName, profiles } = this.props;

    return (
      <>
        <h4 className={classNames('overview-meta-header', headerClassName)}>
          {translate('overview.quality_profiles')}
        </h4>

        <ul className="overview-meta-list">
          {profiles.map(profile => this.renderProfile(profile))}
        </ul>
      </>
    );
  }
}

const mapStateToProps = (state: any) => ({
  languages: getLanguages(state)
});

export default connect<StateProps, {}, OwnProps>(mapStateToProps)(MetaQualityProfiles);
