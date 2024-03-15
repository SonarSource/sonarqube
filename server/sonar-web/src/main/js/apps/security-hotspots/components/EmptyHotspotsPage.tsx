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

import { Note } from 'design-system';
import * as React from 'react';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { Image } from '../../../components/common/Image';
import { translate } from '../../../helpers/l10n';

export interface EmptyHotspotsPageProps {
  filtered: boolean;
  filterByFile: boolean;
  isStaticListOfHotspots: boolean;
}

export default function EmptyHotspotsPage(props: EmptyHotspotsPageProps) {
  const { filtered, filterByFile, isStaticListOfHotspots } = props;

  let translationRoot;
  if (filterByFile) {
    translationRoot = 'no_hotspots_for_file';
  } else if (isStaticListOfHotspots) {
    translationRoot = 'no_hotspots_for_keys';
  } else if (filtered) {
    translationRoot = 'no_hotspots_for_filters';
  } else {
    translationRoot = 'no_hotspots';
  }

  return (
    <div className="sw-items-center sw-justify-center sw-flex-col sw-flex sw-pt-16">
      <Image
        alt={translate('hotspots.page')}
        className="sw-mt-8"
        height={100}
        src={`/images/${filtered && !filterByFile ? 'filter-large' : 'hotspot-large'}.svg`}
      />
      <h1 className="sw-mt-10 sw-body-sm-highlight">
        {translate(`hotspots.${translationRoot}.title`)}
      </h1>
      <Note className="sw-w-abs-400 sw-text-center sw-mt-4">
        {translate(`hotspots.${translationRoot}.description`)}
      </Note>
      {!(filtered || isStaticListOfHotspots) && (
        <DocumentationLink className="sw-mt-4" to="/user-guide/security-hotspots/">
          {translate('hotspots.learn_more')}
        </DocumentationLink>
      )}
    </div>
  );
}
