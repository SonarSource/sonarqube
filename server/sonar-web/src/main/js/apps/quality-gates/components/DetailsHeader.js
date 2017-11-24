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
import BuiltInQualityGateBadge from './BuiltInQualityGateBadge';
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
    const { qualityGate } = this.props;
    const actions = qualityGate.actions || {};
    return (
      <div className="layout-page-header-panel layout-page-main-header issues-main-header">
        <div className="layout-page-header-panel-inner layout-page-main-header-inner">
          <div className="layout-page-main-inner">
            <h2 className="pull-left">
              {qualityGate.name}
              {qualityGate.isBuiltIn && <BuiltInQualityGateBadge className="spacer-left" />}
            </h2>

            <div className="pull-right">
              {actions.edit && (
                <button id="quality-gate-rename" onClick={this.handleRenameClick}>
                  {translate('rename')}
                </button>
              )}
              {actions.copy && (
                <button
                  className="little-spacer-left"
                  id="quality-gate-copy"
                  onClick={this.handleCopyClick}>
                  {translate('copy')}
                </button>
              )}
              {actions.setAsDefault && (
                <button
                  className="little-spacer-left"
                  id="quality-gate-toggle-default"
                  onClick={this.handleSetAsDefaultClick}>
                  {translate('set_as_default')}
                </button>
              )}
              {actions.edit && (
                <button
                  id="quality-gate-delete"
                  className="little-spacer-left button-red"
                  onClick={this.handleDeleteClick}>
                  {translate('delete')}
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    );
  }
}
