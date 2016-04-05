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
import QualifierIcon from '../../components/shared/qualifier-icon';
import { translate } from '../../helpers/l10n';

export default React.createClass({
  propTypes: {
    permissionTemplate: React.PropTypes.object.isRequired,
    topQualifiers: React.PropTypes.array.isRequired
  },

  renderIfSingleTopQualifier() {
    return (
        <ul className="list-inline nowrap spacer-bottom">
          <li>Default</li>
        </ul>
    );
  },

  renderIfMultipleTopQualifiers() {
    const defaults = this.props.permissionTemplate.defaultFor.map(qualifier => {
      return <li key={qualifier}><QualifierIcon qualifier={qualifier}/>&nbsp;{translate('qualifier', qualifier)}</li>;
    });
    return (
        <ul className="list-inline nowrap spacer-bottom">
          <li>Default for</li>
          {defaults}
        </ul>
    );
  },

  render() {
    if (_.size(this.props.permissionTemplate.defaultFor) === 0) {
      return null;
    }
    return this.props.topQualifiers.length === 1 ?
        this.renderIfSingleTopQualifier() :
        this.renderIfMultipleTopQualifiers();
  }
});
