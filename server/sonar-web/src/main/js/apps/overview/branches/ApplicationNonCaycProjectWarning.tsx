/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import withAppStateContext, {
  WithAppStateContextProps,
} from '../../../app/components/app-state/withAppStateContext';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { getUrlForDoc } from '../../../helpers/docs';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getProjectQueryUrl } from '../../../helpers/urls';
import { QualityGateStatus } from '../../../types/quality-gates';
import { CaycStatus } from '../../../types/types';

interface Props {
  projects: QualityGateStatus[];
  caycStatus: CaycStatus;
}

function ApplicationNonCaycProjectWarning({
  projects,
  caycStatus,
  appState,
}: Props & WithAppStateContextProps) {
  const caycUrl = getUrlForDoc(appState.version, '/user-guide/clean-as-you-code/');
  const caycDrawbacksUrl = getUrlForDoc(
    appState.version,
    '/user-guide/clean-as-you-code/#potential-drawbacks'
  );

  return (
    <Card className="sw-mt-4 sw-body-sm">
      {caycStatus === CaycStatus.NonCompliant ? (
        <FlagMessage
          ariaLabel={translateWithParameters(
            'overview.quality_gate.application.non_cayc.projects_x',
            projects.length
          )}
          variant="warning"
        >
          {translateWithParameters(
            'overview.quality_gate.application.non_cayc.projects_x',
            projects.length
          )}
        </FlagMessage>
      ) : (
        <p className="sw-p-2">
          {translateWithParameters(
            'overview.quality_gate.application.cayc_over_compliant.projects_x',
            projects.length
          )}
        </p>
      )}

      <ul className="sw-mt-4 sw-ml-2 sw-mb-2">
        {projects.map(({ key, name, branchLike }) => (
          <li key={key} className="sw-text-ellipsis sw-mb-2" title={name}>
            <Link to={getProjectQueryUrl(key, getBranchLikeQuery(branchLike))}>{name}</Link>
          </li>
        ))}
      </ul>
      <hr className="sw-my-4" />
      <div className="sw-m-2 sw-mt-4">
        {caycStatus === CaycStatus.NonCompliant ? (
          <Link to={caycUrl}>{translate('overview.quality_gate.conditions.cayc.link')}</Link>
        ) : (
          <Link to={caycDrawbacksUrl}>
            {translate('overview.quality_gate.conditions.cayc_over_compliant.link')}
          </Link>
        )}
      </div>
    </Card>
  );
}

export default withAppStateContext(ApplicationNonCaycProjectWarning);
