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
import { translate } from '../../../helpers/l10n';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import PageCounter from '../../../components/common/PageCounter';
import ReloadButton from '../../../components/controls/ReloadButton';

interface Props {
  loading: boolean;
  onReload: () => void;
  paging?: T.Paging;
  selectedIndex?: number;
}

export default function PageActions(props: Props) {
  return (
    <div className="pull-right">
      <Shortcuts />

      <DeferredSpinner loading={props.loading}>
        <ReloadButton onClick={props.onReload} />
      </DeferredSpinner>

      {props.paging && (
        <PageCounter
          className="spacer-left flash flash-heavy"
          current={props.selectedIndex}
          label={translate('coding_rules._rules')}
          total={props.paging.total}
        />
      )}
    </div>
  );
}

function Shortcuts() {
  return (
    <span className="note big-spacer-right">
      <span className="big-spacer-right">
        <span className="shortcut-button little-spacer-right">↑</span>
        <span className="shortcut-button little-spacer-right">↓</span>
        {translate('coding_rules.to_select_rules')}
      </span>

      <span>
        <span className="shortcut-button little-spacer-right">←</span>
        <span className="shortcut-button little-spacer-right">→</span>
        {translate('issues.to_navigate')}
      </span>
    </span>
  );
}
