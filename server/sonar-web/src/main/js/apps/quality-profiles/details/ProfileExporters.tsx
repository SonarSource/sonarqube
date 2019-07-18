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
import { stringify } from 'querystring';
import * as React from 'react';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { Exporter, Profile } from '../types';

interface Props {
  exporters: Exporter[];
  organization: string | null;
  profile: Profile;
}

export default class ProfileExporters extends React.PureComponent<Props> {
  getExportUrl(exporter: Exporter) {
    const { organization, profile } = this.props;

    const path = '/api/qualityprofiles/export';
    const parameters = {
      exporterKey: exporter.key,
      language: profile.language,
      qualityProfile: profile.name
    };
    if (organization) {
      Object.assign(parameters, { organization });
    }
    return (window as any).baseUrl + path + '?' + stringify(parameters);
  }

  render() {
    const { exporters, profile } = this.props;
    const exportersForLanguage = exporters.filter(e => e.languages.includes(profile.language));

    if (exportersForLanguage.length === 0) {
      return null;
    }

    return (
      <div className="boxed-group quality-profile-exporters">
        <h2>{translate('quality_profiles.exporters')}</h2>
        <div className="boxed-group-inner">
          <ul>
            {exportersForLanguage.map((exporter, index) => (
              <li
                className={index > 0 ? 'spacer-top' : undefined}
                data-key={exporter.key}
                key={exporter.key}>
                <a href={this.getExportUrl(exporter)} target="_blank">
                  {exporter.name}
                </a>
              </li>
            ))}
          </ul>
        </div>
      </div>
    );
  }
}
