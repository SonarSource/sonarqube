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

import { Note } from '~design-system';
import { Image } from '~sonar-aligned/components/common/Image';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { DocLink } from '../../../helpers/doc-links';
import { translate } from '../../../helpers/l10n';

export interface EmptyHotspotsPageProps {
  emptyTranslationKey: string;
  filterByFile: boolean;
  filtered: boolean;
  isStaticListOfHotspots: boolean;
}

export default function EmptyHotspotsPage(props: EmptyHotspotsPageProps) {
  const { filtered, filterByFile, emptyTranslationKey, isStaticListOfHotspots } = props;

  return (
    <div className="sw-items-center sw-justify-center sw-flex-col sw-flex sw-pt-16">
      <Image
        alt={translate('hotspots.page')}
        className="sw-mt-8"
        height={100}
        src={`/images/${filtered && !filterByFile ? 'filter-large' : 'hotspot-large'}.svg`}
      />
      <span className="sw-mt-10 sw-typo-semibold">
        {translate(`hotspots.${emptyTranslationKey}.title`)}
      </span>
      <Note className="sw-w-abs-400 sw-text-center sw-mt-4">
        {translate(`hotspots.${emptyTranslationKey}.description`)}
      </Note>
      {!(filtered || isStaticListOfHotspots) && (
        <DocumentationLink className="sw-mt-4" to={DocLink.SecurityHotspots}>
          {translate('hotspots.learn_more')}
        </DocumentationLink>
      )}
    </div>
  );
}
