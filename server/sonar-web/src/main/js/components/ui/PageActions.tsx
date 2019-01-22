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
import FilesCounter from './FilesCounter';
import { translate } from '../../helpers/l10n';

export interface Props {
  current?: number;
  showShortcuts?: boolean;
  total?: number;
}

export default function PageActions(props: Props) {
  const { current, showShortcuts, total = 0 } = props;

  return (
    <div className="page-actions display-flex-center">
      {showShortcuts && (
        <span className="note nowrap">
          <span className="big-spacer-right">
            <span className="shortcut-button little-spacer-right">↑</span>
            <span className="shortcut-button little-spacer-right">↓</span>
            {translate('component_measures.to_select_files')}
          </span>

          <span>
            <span className="shortcut-button little-spacer-right">←</span>
            <span className="shortcut-button little-spacer-right">→</span>
            {translate('component_measures.to_navigate')}
          </span>
        </span>
      )}
      {total > 0 && (
        <div className="nowrap">
          <FilesCounter className="big-spacer-left" current={current} total={total} />
        </div>
      )}
    </div>
  );
}
