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
import { formatMeasure } from '../../../helpers/measures';

export default class AboutProjects extends React.Component {
  static propTypes = {
    count: React.PropTypes.number.isRequired
  };

  render () {
    const { count } = this.props;
    const label = count > 1 ? `${formatMeasure(count, 'INT')} projects` : '1 project';

    return (
        <div className="about-page-text">
          {count > 0 ? (
              <Link to="/projects">{label}</Link>
          ) : 'Put your projects'}
          {' '}
          under continuous<br/>code quality management
        </div>
    );
  }
}
