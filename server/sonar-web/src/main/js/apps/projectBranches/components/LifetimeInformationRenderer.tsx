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
import { FormattedMessage } from 'react-intl';
import Link from '../../../components/common/Link';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate } from '../../../helpers/l10n';
import { formatMeasure } from '../../../helpers/measures';

export interface LifetimeInformationRendererProps {
  branchAndPullRequestLifeTimeInDays?: string;
  canAdmin?: boolean;
  loading: boolean;
}

export function LifetimeInformationRenderer(props: LifetimeInformationRendererProps) {
  const { branchAndPullRequestLifeTimeInDays, canAdmin, loading } = props;

  return (
    <DeferredSpinner loading={loading}>
      {branchAndPullRequestLifeTimeInDays && (
        <p className="page-description">
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
                settings: <Link to="/admin/settings">{translate('settings.page')}</Link>,
              }}
            />
          )}
        </p>
      )}
    </DeferredSpinner>
  );
}

export default React.memo(LifetimeInformationRenderer);
