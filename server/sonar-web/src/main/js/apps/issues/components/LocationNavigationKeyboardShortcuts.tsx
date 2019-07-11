/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { translate } from 'sonar-ui-common/helpers/l10n';

export interface Props {
  issue: Pick<T.Issue, 'flows' | 'secondaryLocations'> | undefined;
}

export default function LocationNavigationKeyboardShortcuts({ issue }: Props) {
  if (!issue || (!issue.secondaryLocations.length && !issue.flows.length)) {
    return null;
  }
  const hasSeveralFlows = issue.flows.length > 1;
  return (
    <div className="navigation-keyboard-shortcuts big-spacer-top text-center">
      <span>
        alt + ↑ ↓ {hasSeveralFlows && <>←→</>}
        {translate('issues.to_navigate_issue_locations')}
      </span>
    </div>
  );
}
