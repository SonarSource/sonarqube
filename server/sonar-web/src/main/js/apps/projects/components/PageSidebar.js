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
import CoverageFilterContainer from '../filters/CoverageFilterContainer';
import DuplicationsFilterContainer from '../filters/DuplicationsFilterContainer';
import SizeFilterContainer from '../filters/SizeFilterContainer';
import QualityGateFilterContainer from '../filters/QualityGateFilterContainer';

export default class PageSidebar extends React.Component {
  render () {
    return (
        <div>
          <CoverageFilterContainer query={this.props.query}/>
          <DuplicationsFilterContainer query={this.props.query}/>
          <SizeFilterContainer query={this.props.query}/>
          <QualityGateFilterContainer query={this.props.query}/>
        </div>
    );
  }
}
