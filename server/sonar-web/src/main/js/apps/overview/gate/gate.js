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
import React from 'react';

import GateConditions from './gate-conditions';
import GateEmpty from './gate-empty';
import { translate, translateWithParameters } from '../../../helpers/l10n';

export default React.createClass({
  renderGateConditions () {
    return <GateConditions gate={this.props.gate} component={this.props.component}/>;
  },

  renderGateText () {
    let text = '';
    if (this.props.gate.level === 'ERROR') {
      text = translateWithParameters('overview.gate.view.errors', this.props.gate.text);
    } else if (this.props.gate.level === 'WARN') {
      text = translateWithParameters('overview.gate.view.warnings', this.props.gate.text);
    } else {
      text = translate('overview.gate.view.no_alert');
    }
    return <div className="overview-card">{text}</div>;
  },

  render() {
    if (!this.props.gate || !this.props.gate.level) {
      return this.props.component.qualifier === 'TRK' ? <GateEmpty/> : null;
    }

    const level = this.props.gate.level.toLowerCase();
    const badgeClassName = 'badge badge-' + level;
    const badgeText = translate('overview.gate', this.props.gate.level);

    return (
        <div className="overview-gate">
          <h2 className="overview-title">
            {translate('overview.quality_gate')}
            <span className={badgeClassName}>{badgeText}</span>
          </h2>
          {this.props.gate.conditions ? this.renderGateConditions() : this.renderGateText()}
        </div>
    );
  }
});
