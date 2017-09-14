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
import { sortBy } from 'lodash';
import HealthCard from './info-items/HealthCard';
import { translate } from '../../../helpers/l10n';
import { SysInfo } from '../types';

interface Props {
  sysInfoData: SysInfo;
}

export default class ClusterSysInfos extends React.PureComponent<Props> {
  render() {
    const { sysInfoData } = this.props;
    return (
      <ul>
        <HealthCard
          biggerHealth={true}
          health={sysInfoData.health}
          healthCauses={sysInfoData.healthCauses}
          name="SonarQube"
          open={false}
        />
        <li className="note system-info-health-title">
          {translate('system.application_nodes_title')}
        </li>
        {sortBy(sysInfoData.applicationNodes, 'name').map(node => (
          <HealthCard
            key={node.name}
            health={node.health}
            healthCauses={node.healthCauses}
            name={node.name}
            open={false}
          />
        ))}
        <li className="note system-info-health-title">{translate('system.search_nodes_title')}</li>
        {sortBy(sysInfoData.searchNodes, 'name').map(node => (
          <HealthCard
            key={node.name}
            health={node.health}
            healthCauses={node.healthCauses}
            name={node.name}
            open={false}
          />
        ))}
      </ul>
    );
  }
}
