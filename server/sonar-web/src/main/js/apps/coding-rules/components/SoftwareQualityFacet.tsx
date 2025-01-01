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
import Facet, { BasicProps } from '../../../components/facets/Facet';
import { FacetHelp } from '../../../components/facets/FacetHelp';
import { SOFTWARE_QUALITIES } from '../../../helpers/constants';
import { DocLink } from '../../../helpers/doc-links';
import { translate } from '../../../helpers/l10n';

export default function SoftwareQualityFacet(props: BasicProps) {
  const renderName = React.useCallback(
    (quality: string) => translate('software_quality', quality),
    [],
  );

  return (
    <Facet
      {...props}
      options={SOFTWARE_QUALITIES}
      property="impactSoftwareQualities"
      renderName={renderName}
      renderTextName={renderName}
      help={
        <FacetHelp property="impactSoftwareQualities" link={DocLink.CleanCodeSoftwareQualities} />
      }
    />
  );
}
