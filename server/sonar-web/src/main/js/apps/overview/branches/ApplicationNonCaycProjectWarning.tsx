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
import { Card, FlagMessage, Link } from 'design-system';
import * as React from 'react';
import { useDocUrl } from '../../../helpers/docs';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getProjectQueryUrl } from '../../../helpers/urls';
import { getBranchLikeQuery } from '../../../sonar-aligned/helpers/branch-like';
import { QualityGateStatus } from '../../../types/quality-gates';

interface Props {
  projects: QualityGateStatus[];
}

export default function ApplicationNonCaycProjectWarning({ projects }: Props) {
  const caycUrl = useDocUrl('/user-guide/clean-as-you-code/');

  return (
    <Card className="sw-mt-4 sw-body-sm">
      <FlagMessage variant="warning">
        {translateWithParameters(
          'overview.quality_gate.application.non_cayc.projects_x',
          projects.length,
        )}
      </FlagMessage>

      <ul className="sw-mt-4 sw-ml-2 sw-mb-2">
        {projects.map(({ key, name, branchLike }) => (
          <li key={key} className="sw-text-ellipsis sw-mb-2" title={name}>
            <Link to={getProjectQueryUrl(key, getBranchLikeQuery(branchLike))}>{name}</Link>
          </li>
        ))}
      </ul>
      <hr className="sw-my-4" />
      <div className="sw-m-2 sw-mt-4">
        <Link to={caycUrl}>{translate('overview.quality_gate.conditions.cayc.link')}</Link>
      </div>
    </Card>
  );
}
