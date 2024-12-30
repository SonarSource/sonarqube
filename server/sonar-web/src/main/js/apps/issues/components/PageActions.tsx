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

import { KeyboardHint } from '~design-system';
import HomePageSelect from '../../../components/controls/HomePageSelect';
import { translate } from '../../../helpers/l10n';
import { Paging } from '../../../types/types';
import IssuesCounter from './IssuesCounter';
import TotalEffort from './TotalEffort';

export interface PageActionsProps {
  canSetHome: boolean;
  effortTotal: number | undefined;
  paging?: Paging;
}

export default function PageActions(props: PageActionsProps) {
  const { canSetHome, effortTotal, paging } = props;

  return (
    <div className="sw-typo-default sw-flex sw-items-center sw-gap-6 sw-justify-end sw-flex-1">
      <KeyboardHint title={translate('issues.to_select_issues')} command="ArrowUp ArrowDown" />
      <KeyboardHint title={translate('issues.to_navigate')} command="ArrowLeft ArrowRight" />

      {paging != null && <IssuesCounter total={paging.total} />}
      {effortTotal !== undefined && <TotalEffort effort={effortTotal} />}

      {canSetHome && <HomePageSelect currentPage={{ type: 'ISSUES' }} />}
    </div>
  );
}
