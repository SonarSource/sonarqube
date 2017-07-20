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
import { translate } from '../../../helpers/l10n';

export default class DetailsHeader extends React.PureComponent {
  handleRenameClick = e => {
    e.preventDefault();
    this.props.onRename();
  };

  handleCopyClick = e => {
    e.preventDefault();
    this.props.onCopy();
  };

  handleSetAsDefaultClick = e => {
    e.preventDefault();
    this.props.onSetAsDefault();
  };

  handleDeleteClick = e => {
    e.preventDefault();
    this.props.onDelete();
  };

  render() {
    const { qualityGate, edit, organization } = this.props;
    const top = organization ? 95 : 30;
    return (
      <div className="search-navigator-workspace-header" style={{ top }}>
        <h2 className="search-navigator-header-component">
          {qualityGate.name}
        </h2>
        {edit &&
          <div className="search-navigator-header-actions">
            <div className="button-group">
              <button id="quality-gate-rename" onClick={this.handleRenameClick}>
                {translate('rename')}
              </button>
              <button id="quality-gate-copy" onClick={this.handleCopyClick}>
                {translate('copy')}
              </button>
              <button id="quality-gate-toggle-default" onClick={this.handleSetAsDefaultClick}>
                {qualityGate.isDefault
                  ? translate('unset_as_default')
                  : translate('set_as_default')}
              </button>
              <button
                id="quality-gate-delete"
                className="button-red"
                onClick={this.handleDeleteClick}>
                {translate('delete')}
              </button>
            </div>
          </div>}
      </div>
    );
  }
}
