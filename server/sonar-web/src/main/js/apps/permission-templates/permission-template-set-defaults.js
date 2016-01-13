/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import _ from 'underscore';
import React from 'react';
import { setDefaultPermissionTemplate } from '../../api/permissions';
import QualifierIcon from '../../components/shared/qualifier-icon';
import { translate } from '../../helpers/l10n';

export default React.createClass({
  propTypes: {
    permissionTemplate: React.PropTypes.object.isRequired,
    topQualifiers: React.PropTypes.array.isRequired,
    refresh: React.PropTypes.func.isRequired
  },

  getAvailableQualifiers() {
    return _.difference(this.props.topQualifiers, this.props.permissionTemplate.defaultFor);
  },

  setDefault(qualifier, e) {
    e.preventDefault();
    setDefaultPermissionTemplate(this.props.permissionTemplate.id, qualifier).done(() => this.props.refresh());
  },

  renderIfSingleTopQualifier(availableQualifiers) {
    let qualifiers = availableQualifiers.map(qualifier => {
      return (
          <span key={qualifier} className="text-middle">
            <a onClick={this.setDefault.bind(this, qualifier)} className="button" href="#">Set Default</a>
          </span>
      );
    });

    return <span className="little-spacer-right">{qualifiers}</span>;
  },

  renderIfMultipleTopQualifiers(availableQualifiers) {
    let qualifiers = availableQualifiers.map(qualifier => {
      return (
          <li key={qualifier}>
            <a onClick={this.setDefault.bind(this, qualifier)} href="#">
              Set Default for <QualifierIcon qualifier={qualifier}/> {translate('qualifier', qualifier)}
            </a>
          </li>
      );
    });

    return (
        <span className="dropdown little-spacer-right">
          <button className="dropdown-toggle" data-toggle="dropdown">
            Set Default <i className="icon-dropdown"></i>
          </button>
          <ul className="dropdown-menu">{qualifiers}</ul>
        </span>
    );
  },

  render() {
    let availableQualifiers = this.getAvailableQualifiers();
    if (availableQualifiers.length === 0) {
      return null;
    }

    return this.props.topQualifiers.length === 1 ?
        this.renderIfSingleTopQualifier(availableQualifiers) :
        this.renderIfMultipleTopQualifiers(availableQualifiers);
  }
});
