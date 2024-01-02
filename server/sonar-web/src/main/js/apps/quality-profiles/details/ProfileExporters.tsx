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
import { getQualityProfileExporterUrl } from '../../../api/quality-profiles';
import Link from '../../../components/common/Link';
import { Alert } from '../../../components/ui/Alert';
import { translate } from '../../../helpers/l10n';
import { getBaseUrl } from '../../../helpers/system';
import { Exporter, Profile } from '../types';

interface Props {
  exporters: Exporter[];
  profile: Profile;
}

export default class ProfileExporters extends React.PureComponent<Props> {
  getExportUrl(exporter: Exporter) {
    const { profile } = this.props;
    return `${getBaseUrl()}${getQualityProfileExporterUrl(exporter, profile)}`;
  }

  render() {
    const { exporters, profile } = this.props;
    const exportersForLanguage = exporters.filter((e) => e.languages.includes(profile.language));

    if (exportersForLanguage.length === 0) {
      return null;
    }

    return (
      <div className="boxed-group quality-profile-exporters">
        <h2>{translate('quality_profiles.exporters')}</h2>
        <div className="boxed-group-inner">
          <Alert className="big-spacer-bottom" variant="warning">
            {translate('quality_profiles.exporters.deprecated')}
          </Alert>
          <ul>
            {exportersForLanguage.map((exporter, index) => (
              <li
                className={index > 0 ? 'spacer-top' : undefined}
                data-key={exporter.key}
                key={exporter.key}
              >
                <Link to={this.getExportUrl(exporter)} target="_blank">
                  {exporter.name}
                </Link>
              </li>
            ))}
          </ul>
        </div>
      </div>
    );
  }
}
