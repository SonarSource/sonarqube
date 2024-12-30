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

import { FacetHelp } from '../../../components/facets/FacetHelp';
import { SOFTWARE_QUALITIES } from '../../../helpers/constants';
import { DocLink } from '../../../helpers/doc-links';
import { SoftwareQuality } from '../../../types/clean-code-taxonomy';
import QGMetricsMismatchHelp from './QGMetricsMismatchHelp';
import { CommonProps, SimpleListStyleFacet } from './SimpleListStyleFacet';

interface Props extends CommonProps {
  qualities: Array<SoftwareQuality>;
}

export function SoftwareQualityFacet(props: Props) {
  const { qualities = [], ...rest } = props;

  return (
    <SimpleListStyleFacet
      property="impactSoftwareQualities"
      itemNamePrefix="software_quality"
      listItems={SOFTWARE_QUALITIES}
      selectedItems={qualities}
      help={
        props.secondLine ? (
          <QGMetricsMismatchHelp />
        ) : (
          <FacetHelp property="impactSoftwareQualities" link={DocLink.CleanCodeSoftwareQualities} />
        )
      }
      {...rest}
    />
  );
}
