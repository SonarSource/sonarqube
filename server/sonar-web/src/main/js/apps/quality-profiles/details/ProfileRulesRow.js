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
import ProgressBar from './ProgressBar';

export default class ProfileRulesRow extends React.Component {
  static propTypes = {
    count: React.PropTypes.number,
    total: React.PropTypes.number,
    tooltip: React.PropTypes.string,
    renderTitle: React.PropTypes.func.isRequired,
    renderCount: React.PropTypes.func.isRequired
  };

  render () {
    const { total, count, tooltip, renderTitle, renderCount } = this.props;

    return (
        <div title={tooltip} data-toggle="tooltip">
          <div className="clearfix">
            <div className="pull-left">
              {renderTitle()}
            </div>
            <div className="pull-right">
              {renderCount()}
            </div>
          </div>
          <div className="little-spacer-top" style={{ height: 2 }}>
            <ProgressBar
                count={count || 0}
                total={total || 0}
                width={300}/>
          </div>
        </div>
    );
  }
}
