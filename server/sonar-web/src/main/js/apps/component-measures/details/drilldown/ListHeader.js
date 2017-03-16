/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import Breadcrumbs from './Breadcrumbs';
import { translateWithParameters } from '../../../../helpers/l10n';

const ListHeader = props => {
  const { metric, breadcrumbs, onBrowse } = props;
  const { selectedIndex, componentsCount, onSelectPrevious, onSelectNext } = props;
  const hasPrevious = selectedIndex > 0;
  const hasNext = selectedIndex < componentsCount - 1;
  const blur = fn => {
    return e => {
      e.target.blur();
      fn();
    };
  };

  return (
    <header className="measure-details-viewer-header">
      {breadcrumbs != null &&
        breadcrumbs.length > 1 &&
        <div className="measure-details-header-container">
          <Breadcrumbs breadcrumbs={breadcrumbs} metric={metric} onBrowse={onBrowse} />
        </div>}

      {selectedIndex != null &&
        selectedIndex !== -1 &&
        <div className="pull-right">
          <span className="note spacer-right">
            {translateWithParameters(
              'component_measures.x_of_y',
              selectedIndex + 1,
              componentsCount
            )}
          </span>

          <div className="button-group">
            {hasPrevious && <button onClick={blur(onSelectPrevious)}>&lt;</button>}
            {hasNext && <button onClick={blur(onSelectNext)}>&gt;</button>}
          </div>
        </div>}
    </header>
  );
};

export default ListHeader;
