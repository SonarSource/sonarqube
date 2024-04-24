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
import { pick } from 'lodash';
import { Query, queryToSearch } from '../../helpers/urls';

import { Path } from 'react-router-dom';
import { BranchLike } from '../../types/branch-like';
import { SecurityStandard } from '../../types/security';
import { getBranchLikeQuery } from './branch-like';

/**
 * Generate URL for a component's issues page
 */

export function getComponentIssuesUrl(componentKey: string, query?: Query): Path {
  return {
    pathname: '/project/issues',
    search: queryToSearch({ ...(query || {}), id: componentKey }),
    hash: '',
  };
}

/**
 * Generate URL for a component's security hotspot page
 */

export function getComponentSecurityHotspotsUrl(
  componentKey: string,
  branchLike?: BranchLike,
  query: Query = {},
): Path {
  const { inNewCodePeriod, hotspots, assignedToMe, files } = query;
  return {
    pathname: '/security_hotspots',
    search: queryToSearch({
      id: componentKey,
      inNewCodePeriod,
      hotspots,
      assignedToMe,
      files,
      ...getBranchLikeQuery(branchLike),
      ...pick(query, [
        SecurityStandard.OWASP_TOP10_2021,
        SecurityStandard.OWASP_TOP10,
        SecurityStandard.SONARSOURCE,
        SecurityStandard.CWE,
        SecurityStandard.PCI_DSS_3_2,
        SecurityStandard.PCI_DSS_4_0,
        SecurityStandard.OWASP_ASVS_4_0,
        'owaspAsvsLevel',
      ]),
    }),
    hash: '',
  };
}
