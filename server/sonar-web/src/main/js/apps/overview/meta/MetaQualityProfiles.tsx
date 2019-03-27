/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { getLanguages, Store } from '../../../store/rootReducer';

interface StateProps {
  languages: T.Languages;
}

interface OwnProps {
  headerClassName?: string;
  organization?: string;
  profiles: T.ComponentQualityProfile[];
}

interface State {
  deprecatedByKey: T.Dict<number>;
}

export class MetaQualityProfiles extends React.PureComponent<StateProps & OwnProps, State> {
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
    const existingProfiles = this.props.profiles.filter(p => !p.deleted);
    const requests = existingProfiles.map(profile =>
      this.loadDeprecatedRulesForProfile(profile.key)
    );
    Promise.all(requests).then(
      responses => {
        if (this.mounted) {
          const deprecatedByKey: T.Dict<number> = {};
          responses.forEach((count, i) => {
            const profileKey = existingProfiles[i].key;
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

  renderProfile(profile: T.ComponentQualityProfile) {
    const languageFromStore = this.props.languages[profile.language];
    const languageName = languageFromStore ? languageFromStore.name : profile.language;

    const inner = (
      <div className="text-ellipsis">
        <span className="note spacer-right">{'(' + languageName + ')'}</span>
        {profile.deleted ? (
          profile.name
        ) : (
          <Link to={getQualityProfileUrl(profile.name, profile.language, this.props.organization)}>
            {profile.name}
          </Link>
        )}
      </div>
    );

    if (profile.deleted) {
      const tooltip = translateWithParameters('overview.deleted_profile', profile.name);
      return (
        <Tooltip key={profile.key} overlay={tooltip}>
          <li className="overview-deleted-profile">{inner}</li>
        </Tooltip>
      );
    }

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

const mapStateToProps = (state: Store) => ({
  languages: getLanguages(state)
});

export default connect(mapStateToProps)(MetaQualityProfiles);
