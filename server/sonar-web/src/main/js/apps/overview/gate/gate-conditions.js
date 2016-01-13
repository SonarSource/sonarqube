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
import GateCondition from './gate-condition';

export default React.createClass({
  propTypes: {
    gate: React.PropTypes.object.isRequired,
    component: React.PropTypes.object.isRequired
  },

  render() {
    let conditions = this.props.gate.conditions
        .filter(c => c.level !== 'OK')
        .map(c => <GateCondition key={c.metric.name} condition={c} component={this.props.component}/>);
    return <ul className="overview-gate-conditions-list">{conditions}</ul>;
  }
});
