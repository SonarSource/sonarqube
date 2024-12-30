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

import { Link } from '@sonarsource/echoes-react';
import { FormattedMessage } from 'react-intl';
import { getBranchLikeQuery } from '~sonar-aligned/helpers/branch-like';
import { getProjectQueryUrl } from '../../../helpers/urls';
import { ComponentQualifier } from '../../../sonar-aligned/types/component';
import { QualityGateStatus } from '../../../types/quality-gates';

interface Props {
  projects: QualityGateStatus[];
}

export default function ApplicationNonCaycProjectWarning({ projects }: Props) {
  return (
    <>
      <p className="sw-font-bold">
        <FormattedMessage
          id={`overview.quality_gate.conditions.cayc.warning.title.${ComponentQualifier.Application}`}
        />
      </p>

      <p className="sw-my-4">
        <FormattedMessage
          id={`overview.quality_gate.conditions.cayc.details.${ComponentQualifier.Application}`}
        />
      </p>

      <ul className="sw-ml-2 sw-list-disc sw-list-inside">
        {projects.map(({ key, name, branchLike }) => (
          <li key={key} className="sw-text-ellipsis" title={name}>
            <Link to={getProjectQueryUrl(key, getBranchLikeQuery(branchLike))}>{name}</Link>
          </li>
        ))}
      </ul>
    </>
  );
}
