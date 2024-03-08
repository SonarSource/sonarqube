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
import { AppStateContext } from '../../../app/components/app-state/AppStateContext';
import Link from '../../../components/common/Link';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { ComponentQualifier } from '../../../types/component';
import { Task } from '../../../types/tasks';
import { useLicenseIsValid } from './useLicenseIsValid';

interface Props {
  currentTask: Task;
}

export function AnalysisLicenseError(props: Props) {
  const { currentTask } = props;
  const appState = React.useContext(AppStateContext);
  const [licenseIsValid, loading] = useLicenseIsValid();

  if (loading || !currentTask.errorType) {
    return null;
  }

  if (licenseIsValid && currentTask.errorType !== 'LICENSING_LOC') {
    return (
      <>
        {translateWithParameters(
          'component_navigation.status.last_blocked_due_to_bad_license_X',
          translate('qualifier', currentTask.componentQualifier ?? ComponentQualifier.Project),
        )}
      </>
    );
  }

  return (
    <>
      <span className="sw-mr-1">{currentTask.errorMessage}</span>
      {appState.canAdmin ? (
        <Link to="/admin/extension/license/app">
          {translate('license.component_navigation.button', currentTask.errorType)}.
        </Link>
      ) : (
        translate('please_contact_administrator')
      )}
    </>
  );
}
