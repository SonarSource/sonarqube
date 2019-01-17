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

interface Props {
  current?: number;
  isFile?: boolean;
  paging?: T.Paging;
  showPaging?: boolean;
  showShortcuts?: boolean;
  totalLoadedComponents?: number;
}

export default function PageActions(props: Props) {
  const { isFile, paging, showPaging, showShortcuts, totalLoadedComponents } = props;
  let total = 0;

  if (showPaging && totalLoadedComponents) {
    total = totalLoadedComponents;
  } else if (paging !== undefined) {
    total = isFile && totalLoadedComponents ? totalLoadedComponents : paging.total;
  }

  return (
    <div className="page-actions display-flex-center">
      {!isFile && showShortcuts && renderShortcuts()}
      {isFile && (paging || showPaging) && renderFileShortcuts()}
      {total > 0 && (
        <div className="measure-details-page-actions nowrap">
          <FilesCounter className="big-spacer-left" current={props.current} total={total} />
        </div>
      )}
    </div>
  );
}

function renderShortcuts() {
  return (
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
  );
}

function renderFileShortcuts() {
  return (
    <span className="note nowrap">
      <span>
        <span className="shortcut-button little-spacer-right">j</span>
        <span className="shortcut-button little-spacer-right">k</span>
        {translate('component_measures.to_navigate_files')}
      </span>
    </span>
  );
}
