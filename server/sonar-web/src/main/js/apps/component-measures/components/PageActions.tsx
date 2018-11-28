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
import { translate } from '../../../helpers/l10n';
import { View } from '../utils';

interface Props {
  current?: number;
  isFile?: boolean;
  paging?: T.Paging;
  totalLoadedComponents?: number;
  view?: View;
}

export default function PageActions(props: Props) {
  const { isFile, paging, totalLoadedComponents } = props;
  const showShortcuts = props.view && ['list', 'tree'].includes(props.view);
  return (
    <div className="display-flex-center">
      {!isFile && showShortcuts && renderShortcuts()}
      {isFile && paging && renderFileShortcuts()}
      <div className="measure-details-page-actions nowrap">
        {paging != null && (
          <FilesCounter
            className="spacer-left"
            current={props.current}
            total={isFile && totalLoadedComponents != null ? totalLoadedComponents : paging.total}
          />
        )}
      </div>
    </div>
  );
}

function renderShortcuts() {
  return (
    <span className="note big-spacer-right nowrap">
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
    <span className="note spacer-right nowrap">
      <span>
        <span className="shortcut-button little-spacer-right">j</span>
        <span className="shortcut-button little-spacer-right">k</span>
        {translate('component_measures.to_navigate_files')}
      </span>
    </span>
  );
}
