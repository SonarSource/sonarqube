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
import * as React from 'react';
import HealthItem from './HealthItem';
import OpenCloseIcon from '../../../../components/icons-components/OpenCloseIcon';
import { HealthType, HealthCause } from '../../types';

interface Props {
  biggerHealth?: boolean;
  health: HealthType;
  healthCauses: HealthCause[];
  open: boolean;
  name: string;
}

export default class HealthCard extends React.PureComponent<Props> {
  render() {
    return (
      <li className="boxed-group system-info-health-card">
        <div className="boxed-group-header">
          <OpenCloseIcon className="spacer-right" open={this.props.open} />
          <span className="system-info-health-card-title">{this.props.name}</span>
          <HealthItem
            bigger={this.props.biggerHealth}
            className="pull-right"
            health={this.props.health}
            healthCauses={this.props.healthCauses}
          />
        </div>
        <div className="boxed-group-inner">{/*TODO Add the sys info sections here if open */}</div>
      </li>
    );
  }
}
