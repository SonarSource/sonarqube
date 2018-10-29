/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { Link } from 'react-router';
import { FormattedMessage } from 'react-intl';
import { translate } from '../../../helpers/l10n';
import { BranchLike } from '../../../app/types';
import { isBranch, isLongLivingBranch } from '../../../helpers/branches';
import { Alert } from '../../../components/ui/Alert';

interface Props {
  branchLike?: BranchLike;
  branchLikes: BranchLike[];
  component: string;
  showWarning?: boolean;
}

export default function EmptyOverview({ branchLike, branchLikes, component, showWarning }: Props) {
  const hasBranches = branchLikes.length > 1;
  const hasBadConfig =
    branchLikes.length > 2 ||
    (branchLikes.length === 2 && branchLikes.some(branch => isLongLivingBranch(branch)));

  const branchWarnMsg = hasBadConfig
    ? translate('provisioning.no_analysis_on_main_branch.bad_configuration')
    : translate('provisioning.no_analysis_on_main_branch');

  return (
    <div className="page page-limited">
      {showWarning && (
        <div className="big-spacer-bottom">
          <Alert variant="warning">
            {hasBranches && isBranch(branchLike) ? (
              <FormattedMessage
                defaultMessage={branchWarnMsg}
                id={branchWarnMsg}
                values={{
                  branchName: branchLike.name,
                  branchType: (
                    <div className="outline-badge text-baseline">
                      {translate('branches.main_branch')}
                    </div>
                  )
                }}
              />
            ) : (
              translate('provisioning.no_analysis')
            )}
          </Alert>

          {!hasBranches && (
            <div className="big-spacer-top">
              <FormattedMessage
                defaultMessage={translate('provisioning.no_analysis.delete')}
                id={'provisioning.no_analysis.delete'}
                values={{
                  link: (
                    <Link
                      className="text-danger"
                      to={{ pathname: '/project/deletion', query: { id: component } }}>
                      {translate('provisioning.no_analysis.delete_project')}
                    </Link>
                  )
                }}
              />
            </div>
          )}
        </div>
      )}

      <div>
        <h4>{translate('key')}</h4>
        <code>{component}</code>
      </div>
    </div>
  );
}
