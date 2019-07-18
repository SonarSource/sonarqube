/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { sortBy } from 'lodash';
import * as React from 'react';
import { translate } from 'sonar-ui-common/helpers/l10n';
import {
  getAppNodes,
  getClusterMainCardSection,
  getHealth,
  getHealthCauses,
  getNodeName,
  getSearchNodes,
  ignoreInfoFields
} from '../utils';
import HealthCard from './info-items/HealthCard';

interface Props {
  expandedCards: string[];
  sysInfoData: T.SysInfoCluster;
  toggleCard: (toggledCard: string) => void;
}

export default function ClusterSysInfos({ expandedCards, sysInfoData, toggleCard }: Props) {
  const mainCardName = 'System';
  return (
    <>
      <HealthCard
        biggerHealth={true}
        health={getHealth(sysInfoData)}
        healthCauses={getHealthCauses(sysInfoData)}
        name={mainCardName}
        onClick={toggleCard}
        open={expandedCards.includes(mainCardName)}
        sysInfoData={ignoreInfoFields(getClusterMainCardSection(sysInfoData))}
      />
      <li className="note system-info-health-title">
        {translate('system.application_nodes_title')}
      </li>
      {sortBy(getAppNodes(sysInfoData), getNodeName).map((node: T.SysInfoAppNode) => (
        <HealthCard
          health={getHealth(node)}
          healthCauses={getHealthCauses(node)}
          key={getNodeName(node)}
          name={getNodeName(node)}
          onClick={toggleCard}
          open={expandedCards.includes(getNodeName(node))}
          sysInfoData={ignoreInfoFields(node)}
        />
      ))}
      <li className="note system-info-health-title">{translate('system.search_nodes_title')}</li>
      {sortBy(getSearchNodes(sysInfoData), getNodeName).map((node: T.SysInfoSearchNode) => (
        <HealthCard
          key={getNodeName(node)}
          name={getNodeName(node)}
          onClick={toggleCard}
          open={expandedCards.includes(getNodeName(node))}
          sysInfoData={ignoreInfoFields(node)}
        />
      ))}
    </>
  );
}
