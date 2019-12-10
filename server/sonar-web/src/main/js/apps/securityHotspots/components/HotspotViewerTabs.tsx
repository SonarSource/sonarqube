/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { sanitize } from 'dompurify';
import * as React from 'react';
import BoxedTabs from 'sonar-ui-common/components/controls/BoxedTabs';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { DetailedHotspot } from '../../../types/security-hotspots';

export interface HotspotViewerTabsProps {
  hotspot: DetailedHotspot;
}

export enum Tabs {
  RiskDescription = 'risk',
  VulnerabilityDescription = 'vulnerability',
  FixRecommendation = 'fix'
}

export default function HotspotViewerTabs(props: HotspotViewerTabsProps) {
  const { hotspot } = props;
  const [currentTab, setCurrentTab] = React.useState(Tabs.RiskDescription);

  const tabs = {
    [Tabs.RiskDescription]: {
      title: translate('hotspot.tabs.risk_description'),
      content: hotspot.rule.riskDescription || ''
    },
    [Tabs.VulnerabilityDescription]: {
      title: translate('hotspot.tabs.vulnerability_description'),
      content: hotspot.rule.vulnerabilityDescription || ''
    },
    [Tabs.FixRecommendation]: {
      title: translate('hotspot.tabs.fix_recommendations'),
      content: hotspot.rule.fixRecommendations || ''
    }
  };

  const tabsToDisplay = Object.values(Tabs)
    .filter(tab => Boolean(tabs[tab].content))
    .map(tab => ({ key: tab, label: tabs[tab].title }));

  if (tabsToDisplay.length === 0) {
    return null;
  }

  if (!tabsToDisplay.find(tab => tab.key === currentTab)) {
    setCurrentTab(tabsToDisplay[0].key);
  }

  return (
    <>
      <BoxedTabs onSelect={tab => setCurrentTab(tab)} selected={currentTab} tabs={tabsToDisplay} />
      <div
        className="boxed-group markdown big-padded"
        dangerouslySetInnerHTML={{ __html: sanitize(tabs[currentTab].content) }}
      />
    </>
  );
}
