/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { groupBy } from 'lodash';
import * as React from 'react';
import BoxedTabs, { getTabId, getTabPanelId } from '../../../components/controls/BoxedTabs';
import RuleDescription from '../../../components/rules/RuleDescription';
import { isInput, isShortcut } from '../../../helpers/keyboardEventHelpers';
import { KeyboardKeys } from '../../../helpers/keycodes';
import { translate } from '../../../helpers/l10n';
import { Hotspot } from '../../../types/security-hotspots';
import { RuleDescriptionSection, RuleDescriptionSections } from '../../coding-rules/rule';

interface Props {
  codeTabContent: React.ReactNode;
  hotspot: Hotspot;
  ruleDescriptionSections?: RuleDescriptionSection[];
  selectedHotspotLocation?: number;
}

interface State {
  currentTab: Tab;
  tabs: Tab[];
}

interface Tab {
  key: TabKeys;
  label: React.ReactNode;
  content: React.ReactNode;
}

export enum TabKeys {
  Code = 'code',
  RiskDescription = 'risk',
  VulnerabilityDescription = 'vulnerability',
  FixRecommendation = 'fix',
}

export default class HotspotViewerTabs extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    const tabs = this.computeTabs();
    this.state = {
      currentTab: tabs[0],
      tabs,
    };
  }

  componentDidMount() {
    this.registerKeyboardEvents();
  }

  componentDidUpdate(prevProps: Props) {
    if (
      this.props.hotspot.key !== prevProps.hotspot.key ||
      prevProps.codeTabContent !== this.props.codeTabContent
    ) {
      const tabs = this.computeTabs();
      this.setState({
        currentTab: tabs[0],
        tabs,
      });
    } else if (
      this.props.selectedHotspotLocation !== undefined &&
      this.props.selectedHotspotLocation !== prevProps.selectedHotspotLocation
    ) {
      const { tabs } = this.state;
      this.setState({
        currentTab: tabs[0],
      });
    }
  }

  componentWillUnmount() {
    this.unregisterKeyboardEvents();
  }

  handleKeyboardNavigation = (event: KeyboardEvent) => {
    if (isInput(event) || isShortcut(event)) {
      return true;
    }
    if (event.key === KeyboardKeys.LeftArrow) {
      event.preventDefault();
      this.selectNeighboringTab(-1);
    } else if (event.key === KeyboardKeys.RightArrow) {
      event.preventDefault();
      this.selectNeighboringTab(+1);
    }
  };

  registerKeyboardEvents() {
    document.addEventListener('keydown', this.handleKeyboardNavigation);
  }

  unregisterKeyboardEvents() {
    document.removeEventListener('keydown', this.handleKeyboardNavigation);
  }

  handleSelectTabs = (tabKey: TabKeys) => {
    const { tabs } = this.state;
    const currentTab = tabs.find((tab) => tab.key === tabKey)!;
    this.setState({ currentTab });
  };

  computeTabs() {
    const { ruleDescriptionSections, codeTabContent } = this.props;
    const descriptionSectionsByKey = groupBy(ruleDescriptionSections, (section) => section.key);
    const rootCauseDescriptionSections =
      descriptionSectionsByKey[RuleDescriptionSections.DEFAULT] ||
      descriptionSectionsByKey[RuleDescriptionSections.ROOT_CAUSE];

    return [
      {
        key: TabKeys.Code,
        label: translate('hotspots.tabs.code'),
        content: <div className="padded">{codeTabContent}</div>,
      },
      {
        key: TabKeys.RiskDescription,
        label: translate('hotspots.tabs.risk_description'),
        content: rootCauseDescriptionSections && (
          <RuleDescription
            className="big-padded"
            sections={rootCauseDescriptionSections}
            isDefault={true}
          />
        ),
      },
      {
        key: TabKeys.VulnerabilityDescription,
        label: translate('hotspots.tabs.vulnerability_description'),
        content: descriptionSectionsByKey[RuleDescriptionSections.ASSESS_THE_PROBLEM] && (
          <RuleDescription
            className="big-padded"
            sections={descriptionSectionsByKey[RuleDescriptionSections.ASSESS_THE_PROBLEM]}
            isDefault={true}
          />
        ),
      },
      {
        key: TabKeys.FixRecommendation,
        label: translate('hotspots.tabs.fix_recommendations'),
        content: descriptionSectionsByKey[RuleDescriptionSections.HOW_TO_FIX] && (
          <RuleDescription
            className="big-padded"
            sections={descriptionSectionsByKey[RuleDescriptionSections.HOW_TO_FIX]}
            isDefault={true}
          />
        ),
      },
    ].filter((tab) => tab.content);
  }

  selectNeighboringTab(shift: number) {
    this.setState(({ tabs, currentTab }) => {
      const index = currentTab && tabs.findIndex((tab) => tab.key === currentTab.key);

      if (index !== undefined && index > -1) {
        const newIndex = Math.max(0, Math.min(tabs.length - 1, index + shift));
        return {
          currentTab: tabs[newIndex],
        };
      }

      return { currentTab };
    });
  }

  render() {
    const { tabs, currentTab } = this.state;
    return (
      <>
        <BoxedTabs onSelect={this.handleSelectTabs} selected={currentTab.key} tabs={tabs} />
        <div
          className="bordered huge-spacer-bottom"
          role="tabpanel"
          aria-labelledby={getTabId(currentTab.key)}
          id={getTabPanelId(currentTab.key)}
        >
          {currentTab.content}
        </div>
      </>
    );
  }
}
