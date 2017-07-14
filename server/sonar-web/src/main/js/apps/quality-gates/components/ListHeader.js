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
import CreateView from '../views/create-view';
import { translate } from '../../../helpers/l10n';

export default function ListHeader({ canEdit, onAdd }) {
  function handleAddClick(e) {
    e.preventDefault();
    new CreateView({ onAdd }).render();
  }

  return (
    <div>
      <h1 className="page-title">
        {translate('quality_gates.page')}
      </h1>
      {canEdit &&
        <div className="page-actions">
          <div className="button-group">
            <button id="quality-gate-add" onClick={handleAddClick}>
              {translate('create')}
            </button>
          </div>
        </div>}
    </div>
  );
}
