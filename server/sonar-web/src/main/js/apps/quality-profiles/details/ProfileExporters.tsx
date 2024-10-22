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

import { LinkStandalone } from '@sonarsource/echoes-react';
import { FlagMessage, SubTitle } from '~design-system';
import { getQualityProfileExporterUrl } from '../../../api/quality-profiles';
import { translate } from '../../../helpers/l10n';
import { Exporter, Profile } from '../types';

interface Props {
  exporters: Exporter[];
  profile: Profile;
}

export default function ProfileExporters({ exporters, profile }: Readonly<Props>) {
  const exportersForLanguage = exporters.filter((e) => e.languages.includes(profile.language));

  if (exportersForLanguage.length === 0) {
    return null;
  }

  return (
    <section aria-label={translate('quality_profiles.exporters')}>
      <div>
        <SubTitle>{translate('quality_profiles.exporters')}</SubTitle>
      </div>

      <FlagMessage className="sw-mb-4" variant="warning">
        {translate('quality_profiles.exporters.deprecated')}
      </FlagMessage>

      <ul className="sw-flex sw-flex-col sw-gap-2">
        {exportersForLanguage.map((exporter) => (
          <li data-key={exporter.key} key={exporter.key}>
            <LinkStandalone shouldOpenInNewTab to={getQualityProfileExporterUrl(exporter, profile)}>
              {exporter.name}
            </LinkStandalone>
          </li>
        ))}
      </ul>
    </section>
  );
}
