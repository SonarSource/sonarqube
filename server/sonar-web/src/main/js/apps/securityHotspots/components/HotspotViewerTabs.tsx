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
import { Hotspot } from '../../../types/security-hotspots';
import { getHotspotReviewHistory } from '../utils';
import HotspotViewerReviewHistoryTab from './HotspotViewerReviewHistoryTab';

export interface HotspotViewerTabsProps {
  hotspot: Hotspot;
}

export enum Tabs {
  RiskDescription = 'risk',
  VulnerabilityDescription = 'vulnerability',
  FixRecommendation = 'fix',
  ReviewHistory = 'review'
}

export default function HotspotViewerTabs(props: HotspotViewerTabsProps) {
  const { hotspot } = props;
  const [currentTabKey, setCurrentTabKey] = React.useState(Tabs.RiskDescription);
  const hotspotReviewHistory = React.useMemo(() => getHotspotReviewHistory(hotspot), [hotspot]);

  const tabs = [
    {
      key: Tabs.RiskDescription,
      label: translate('hotspots.tabs.risk_description'),
      content: hotspot.rule.riskDescription || ''
    },
    {
      key: Tabs.VulnerabilityDescription,
      label: translate('hotspots.tabs.vulnerability_description'),
      content: hotspot.rule.vulnerabilityDescription || ''
    },
    {
      key: Tabs.FixRecommendation,
      label: translate('hotspots.tabs.fix_recommendations'),
      content: hotspot.rule.fixRecommendations || ''
    },
    {
      key: Tabs.ReviewHistory,
      label: (
        <>
          <span>{translate('hotspots.tabs.review_history')}</span>
          <span className="counter-badge spacer-left">{hotspotReviewHistory.length}</span>
        </>
      ),
      content: hotspotReviewHistory.length > 0 && (
        <HotspotViewerReviewHistoryTab history={hotspotReviewHistory} />
      )
    }
  ].filter(tab => Boolean(tab.content));

  if (tabs.length === 0) {
    return null;
  }

  const currentTab = tabs.find(tab => tab.key === currentTabKey) || tabs[0];

  return (
    <>
      <BoxedTabs
        onSelect={tabKey => setCurrentTabKey(tabKey)}
        selected={currentTabKey}
        tabs={tabs}
      />
      <div className="bordered">
        {typeof currentTab.content === 'string' ? (
          <div
            className="markdown big-padded"
            dangerouslySetInnerHTML={{ __html: sanitize(currentTab.content) }}
          />
        ) : (
          <>{currentTab.content}</>
        )}
      </div>
    </>
  );
}
