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
import Tab from 'sonar-ui-common/components/controls/Tabs';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { Hotspot } from '../../../types/security-hotspots';

interface Props {
  hotspot: Hotspot;
}

interface State {
  currentTab: Tab;
  tabs: Tab[];
}

interface Tab {
  key: TabKeys;
  label: React.ReactNode;
  content: string;
}

export enum TabKeys {
  RiskDescription = 'risk',
  VulnerabilityDescription = 'vulnerability',
  FixRecommendation = 'fix'
}

export default class HotspotViewerTabs extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    const tabs = this.computeTabs();
    this.state = {
      currentTab: tabs[0],
      tabs
    };
  }

  componentDidUpdate(prevProps: Props) {
    if (this.props.hotspot.key !== prevProps.hotspot.key) {
      const tabs = this.computeTabs();
      this.setState({
        currentTab: tabs[0],
        tabs
      });
    }
  }

  handleSelectTabs = (tabKey: TabKeys) => {
    const { tabs } = this.state;
    const currentTab = tabs.find(tab => tab.key === tabKey)!;
    this.setState({ currentTab });
  };

  computeTabs() {
    const { hotspot } = this.props;
    return [
      {
        key: TabKeys.RiskDescription,
        label: translate('hotspots.tabs.risk_description'),
        content: hotspot.rule.riskDescription || ''
      },
      {
        key: TabKeys.VulnerabilityDescription,
        label: translate('hotspots.tabs.vulnerability_description'),
        content: hotspot.rule.vulnerabilityDescription || ''
      },
      {
        key: TabKeys.FixRecommendation,
        label: translate('hotspots.tabs.fix_recommendations'),
        content: hotspot.rule.fixRecommendations || ''
      }
    ].filter(tab => Boolean(tab.content));
  }

  render() {
    const { tabs, currentTab } = this.state;
    if (tabs.length === 0) {
      return null;
    }

    return (
      <>
        <BoxedTabs onSelect={this.handleSelectTabs} selected={currentTab.key} tabs={tabs} />
        <div className="bordered huge-spacer-bottom">
          <div
            className="markdown big-padded"
            dangerouslySetInnerHTML={{ __html: sanitize(currentTab.content) }}
          />
        </div>
      </>
    );
  }
}
