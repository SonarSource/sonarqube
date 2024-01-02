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
import PageCounter from '../../../components/common/PageCounter';
import PageShortcutsTooltip from '../../../components/ui/PageShortcutsTooltip';
import { translate } from '../../../helpers/l10n';
import { Paging } from '../../../types/types';

export interface PageActionsProps {
  paging?: Paging;
  selectedIndex?: number;
}

export default function PageActions(props: PageActionsProps) {
  return (
    <div className="display-flex-center">
      <PageShortcutsTooltip
        className="big-spacer-right"
        leftAndRightLabel={translate('issues.to_navigate')}
        upAndDownLabel={translate('coding_rules.to_select_rules')}
      />

      {props.paging && (
        <PageCounter
          className="spacer-left"
          current={props.selectedIndex}
          label={translate('coding_rules._rules')}
          total={props.paging.total}
        />
      )}
    </div>
  );
}
