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
import { translate } from '../../helpers/l10n';
import { formatMeasure } from '../../helpers/measures';
import { ComponentQualifier } from '../../types/component';

export interface Props {
  componentQualifier?: string;
  current?: number;
  showShortcuts?: boolean;
  total?: number;
}

export default function PageActions(props: Props) {
  const { componentQualifier, current, showShortcuts, total = 0 } = props;

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
      {total > 0 && componentQualifier === ComponentQualifier.Project && (
        <div className="nowrap">
          <span className="big-spacer-left">
            <strong>
              {current !== undefined && (
                <span>
                  {formatMeasure(current, 'INT')}
                  {' / '}
                </span>
              )}
              {formatMeasure(total, 'INT')}
            </strong>{' '}
            {translate('component_measures.files')}
          </span>
        </div>
      )}
    </div>
  );
}
