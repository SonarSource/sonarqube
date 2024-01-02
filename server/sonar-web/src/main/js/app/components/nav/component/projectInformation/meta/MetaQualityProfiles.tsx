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
import { searchRules } from '../../../../../../api/rules';
import Link from '../../../../../../components/common/Link';
import Tooltip from '../../../../../../components/controls/Tooltip';
import { translate, translateWithParameters } from '../../../../../../helpers/l10n';
import { getQualityProfileUrl } from '../../../../../../helpers/urls';
import { Languages } from '../../../../../../types/languages';
import { ComponentQualityProfile, Dict } from '../../../../../../types/types';
import withLanguagesContext from '../../../../languages/withLanguagesContext';

interface Props {
  headerClassName?: string;
  languages: Languages;
  profiles: ComponentQualityProfile[];
}

interface State {
  deprecatedByKey: Dict<number>;
}

export class MetaQualityProfiles extends React.PureComponent<Props, State> {
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
    const existingProfiles = this.props.profiles.filter((p) => !p.deleted);
    const requests = existingProfiles.map((profile) =>
      this.loadDeprecatedRulesForProfile(profile.key)
    );
    Promise.all(requests).then(
      (responses) => {
        if (this.mounted) {
          const deprecatedByKey: Dict<number> = {};
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
      ps: 1,
      qprofile: profileKey,
      statuses: 'DEPRECATED',
    };
    return searchRules(data).then((r) => r.total);
  }

  getDeprecatedRulesCount(profile: { key: string }) {
    const count = this.state.deprecatedByKey[profile.key];
    return count || 0;
  }

  renderProfile(profile: ComponentQualityProfile) {
    const languageFromStore = this.props.languages[profile.language];
    const languageName = languageFromStore ? languageFromStore.name : profile.language;

    const inner = (
      <div className="text-ellipsis">
        <span className="spacer-right">({languageName})</span>
        {profile.deleted ? (
          profile.name
        ) : (
          <Link to={getQualityProfileUrl(profile.name, profile.language)}>
            <span
              aria-label={translateWithParameters(
                'overview.link_to_x_profile_y',
                languageName,
                profile.name
              )}
            >
              {profile.name}
            </span>
          </Link>
        )}
      </div>
    );

    if (profile.deleted) {
      const tooltip = translateWithParameters('overview.deleted_profile', profile.name);
      return (
        <Tooltip key={profile.key} overlay={tooltip}>
          <li className="project-info-deleted-profile">{inner}</li>
        </Tooltip>
      );
    }

    const count = this.getDeprecatedRulesCount(profile);

    if (count > 0) {
      const tooltip = translateWithParameters('overview.deprecated_profile', count);
      return (
        <Tooltip key={profile.key} overlay={tooltip}>
          <li className="project-info-deprecated-rules">{inner}</li>
        </Tooltip>
      );
    }

    return <li key={profile.key}>{inner}</li>;
  }

  render() {
    const { headerClassName, profiles } = this.props;

    return (
      <>
        <h3 className={headerClassName}>{translate('overview.quality_profiles')}</h3>

        <ul className="project-info-list">
          {profiles.map((profile) => this.renderProfile(profile))}
        </ul>
      </>
    );
  }
}

export default withLanguagesContext(MetaQualityProfiles);
