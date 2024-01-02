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
import HomePageSelect from '../../../components/controls/HomePageSelect';
import PageShortcutsTooltip from '../../../components/ui/PageShortcutsTooltip';
import { translate } from '../../../helpers/l10n';
import { Paging } from '../../../types/types';
import IssuesCounter from './IssuesCounter';
import TotalEffort from './TotalEffort';

export interface PageActionsProps {
  canSetHome: boolean;
  effortTotal: number | undefined;
  paging?: Paging;
  selectedIndex?: number;
}

export default function PageActions(props: PageActionsProps) {
  const { canSetHome, effortTotal, paging, selectedIndex } = props;

  return (
    <div className="display-flex-center display-flex-justify-end">
      <PageShortcutsTooltip
        leftAndRightLabel={translate('issues.to_navigate')}
        upAndDownLabel={translate('issues.to_select_issues')}
      />

      <div className="spacer-left issues-page-actions">
        {paging != null && <IssuesCounter current={selectedIndex} total={paging.total} />}
        {effortTotal !== undefined && <TotalEffort effort={effortTotal} />}
      </div>

      {canSetHome && (
        <HomePageSelect className="huge-spacer-left" currentPage={{ type: 'ISSUES' }} />
      )}
    </div>
  );
}
