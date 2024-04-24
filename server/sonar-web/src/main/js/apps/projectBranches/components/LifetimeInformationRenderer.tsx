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
import { Link, Spinner } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { translate } from '../../../helpers/l10n';

export interface LifetimeInformationRendererProps {
  branchAndPullRequestLifeTimeInDays?: string;
  canAdmin?: boolean;
  loading: boolean;
}

function LifetimeInformationRenderer(props: LifetimeInformationRendererProps) {
  const { branchAndPullRequestLifeTimeInDays, canAdmin, loading } = props;

  return (
    <Spinner loading={loading}>
      {branchAndPullRequestLifeTimeInDays && (
        <p>
          <FormattedMessage
            defaultMessage={translate('project_branch_pull_request.lifetime_information')}
            id="project_branch_pull_request.lifetime_information"
            values={{ days: formatMeasure(branchAndPullRequestLifeTimeInDays, 'INT') }}
          />
          {canAdmin && (
            <FormattedMessage
              defaultMessage={translate('project_branch_pull_request.lifetime_information.admin')}
              id="project_branch_pull_request.lifetime_information.admin"
              values={{
                settings: (
                  <Link to="/admin/settings?category=housekeeping#sonar.dbcleaner.daysBeforeDeletingInactiveBranchesAndPRs">
                    {translate('settings.page')}
                  </Link>
                ),
              }}
            />
          )}
        </p>
      )}
    </Spinner>
  );
}

export default React.memo(LifetimeInformationRenderer);
