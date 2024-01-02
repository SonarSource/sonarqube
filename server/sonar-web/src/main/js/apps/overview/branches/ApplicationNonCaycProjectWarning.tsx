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
import DocLink from '../../../components/common/DocLink';
import Link from '../../../components/common/Link';
import QualifierIcon from '../../../components/icons/QualifierIcon';
import { Alert } from '../../../components/ui/Alert';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getProjectQueryUrl } from '../../../helpers/urls';
import { ComponentQualifier } from '../../../types/component';
import { QualityGateStatus } from '../../../types/quality-gates';
import { CaycStatus } from '../../../types/types';

interface Props {
  projects: QualityGateStatus[];
  caycStatus: CaycStatus;
}

export default function ApplicationNonCaycProjectWarning({ projects, caycStatus }: Props) {
  return (
    <div className="overview-quality-gate-conditions-list padded big-spacer-top">
      {caycStatus === CaycStatus.NonCompliant ? (
        <Alert variant="warning">
          {translateWithParameters(
            'overview.quality_gate.application.non_cayc.projects_x',
            projects.length
          )}
        </Alert>
      ) : (
        <p className="padded">
          {translateWithParameters(
            'overview.quality_gate.application.cayc_over_compliant.projects_x',
            projects.length
          )}
        </p>
      )}

      <ul className="spacer-left spacer-bottom big-spacer-top">
        {projects.map(({ key, name, branchLike }) => (
          <li key={key} className="text-ellipsis spacer-bottom" title={name}>
            <Link
              className="link-no-underline"
              to={getProjectQueryUrl(key, getBranchLikeQuery(branchLike))}
            >
              <QualifierIcon
                className="little-spacer-right"
                qualifier={ComponentQualifier.Project}
              />
              {name}
            </Link>
          </li>
        ))}
      </ul>
      <hr className="big-spacer-top big-spacer-bottom" />
      <div className="spacer spacer-bottom big-spacer-top">
        {caycStatus === CaycStatus.NonCompliant ? (
          <DocLink to="/user-guide/clean-as-you-code/">
            {translate('overview.quality_gate.conditions.cayc.link')}
          </DocLink>
        ) : (
          <DocLink to="/user-guide/clean-as-you-code/#potential-drawbacks">
            {translate('overview.quality_gate.conditions.cayc_over_compliant.link')}
          </DocLink>
        )}
      </div>
    </div>
  );
}
