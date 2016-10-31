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
import { Link } from 'react-router';
import Filter from './Filter';
import Level from '../../../components/ui/Level';

export default class QualityGateFilter extends React.Component {
  static propTypes = {
    value: React.PropTypes.any,
    getFilterUrl: React.PropTypes.func.isRequired,
    toggleFilter: React.PropTypes.func.isRequired
  };

  renderOptions () {
    const options = ['ERROR', 'WARN', 'OK'];

    return options.map(option => (
        <Link key={option}
              className={'facet search-navigator-facet ' + (option === this.props.value ? 'active' : '')}
              to={this.props.getFilterUrl({ gate: option })}
              onClick={this.props.toggleFilter}>
          <span className="facet-name projects-facet-name">
            <Level level={option}/>
          </span>
        </Link>
    ));
  }

  render () {
    return (
        <Filter
            renderName={() => 'Quality Gate'}
            renderOptions={() => this.renderOptions()}
            {...this.props}/>
    );
  }
}
