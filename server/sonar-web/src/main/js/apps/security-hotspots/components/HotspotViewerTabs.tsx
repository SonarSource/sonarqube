/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { ToggleButton, getTabId, getTabPanelId } from 'design-system';
import { groupBy, omit } from 'lodash';
import * as React from 'react';
import RuleDescription from '../../../components/rules/RuleDescription';
import { isInput, isShortcut } from '../../../helpers/keyboardEventHelpers';
import { KeyboardKeys } from '../../../helpers/keycodes';
import { translate } from '../../../helpers/l10n';
import { Hotspot } from '../../../types/security-hotspots';
import { RuleDescriptionSection, RuleDescriptionSections } from '../../coding-rules/rule';

interface Props {
  activityTabContent: React.ReactNode;
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
  value: TabKeys;
  label: string;
}

export enum TabKeys {
  Code = 'code',
  RiskDescription = 'risk',
  VulnerabilityDescription = 'vulnerability',
  FixRecommendation = 'fix',
  Activity = 'activity',
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
    if (this.props.hotspot.key !== prevProps.hotspot.key) {
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
    const currentTab = tabs.find((tab) => tab.value === tabKey);
    if (currentTab) {
      this.setState({ currentTab });
    }
  };

  computeTabs() {
    const { ruleDescriptionSections } = this.props;
    const descriptionSectionsByKey = groupBy(ruleDescriptionSections, (section) => section.key);

    return [
      {
        value: TabKeys.Code,
        label: translate('hotspots.tabs.code'),
        show: true,
      },
      {
        value: TabKeys.RiskDescription,
        label: translate('hotspots.tabs.risk_description'),
        show:
          descriptionSectionsByKey[RuleDescriptionSections.DEFAULT] ||
          descriptionSectionsByKey[RuleDescriptionSections.ROOT_CAUSE],
      },
      {
        value: TabKeys.VulnerabilityDescription,
        label: translate('hotspots.tabs.vulnerability_description'),
        show: descriptionSectionsByKey[RuleDescriptionSections.ASSESS_THE_PROBLEM] !== undefined,
      },
      {
        value: TabKeys.FixRecommendation,
        label: translate('hotspots.tabs.fix_recommendations'),
        show: descriptionSectionsByKey[RuleDescriptionSections.HOW_TO_FIX] !== undefined,
      },
      {
        value: TabKeys.Activity,
        label: translate('hotspots.tabs.activity'),
        show: true,
      },
    ]
      .filter((tab) => tab.show)
      .map((tab) => omit(tab, 'show'));
  }

  selectNeighboringTab(shift: number) {
    this.setState(({ tabs, currentTab }) => {
      const index = currentTab && tabs.findIndex((tab) => tab.value === currentTab.value);

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
    const { ruleDescriptionSections, codeTabContent, activityTabContent } = this.props;
    const { tabs, currentTab } = this.state;

    const descriptionSectionsByKey = groupBy(ruleDescriptionSections, (section) => section.key);
    const rootCauseDescriptionSections =
      descriptionSectionsByKey[RuleDescriptionSections.DEFAULT] ||
      descriptionSectionsByKey[RuleDescriptionSections.ROOT_CAUSE];

    return (
      <>
        <ToggleButton
          role="tablist"
          value={currentTab.value}
          options={tabs}
          onChange={this.handleSelectTabs}
        />
        <div
          aria-labelledby={getTabId(currentTab.value)}
          className="sw-mt-6"
          id={getTabPanelId(currentTab.value)}
          role="tabpanel"
        >
          {currentTab.value === TabKeys.Code && codeTabContent}

          {currentTab.value === TabKeys.RiskDescription && rootCauseDescriptionSections && (
            <RuleDescription sections={rootCauseDescriptionSections} />
          )}

          {currentTab.value === TabKeys.VulnerabilityDescription &&
            descriptionSectionsByKey[RuleDescriptionSections.ASSESS_THE_PROBLEM] && (
              <RuleDescription
                sections={descriptionSectionsByKey[RuleDescriptionSections.ASSESS_THE_PROBLEM]}
              />
            )}

          {currentTab.value === TabKeys.FixRecommendation &&
            descriptionSectionsByKey[RuleDescriptionSections.HOW_TO_FIX] && (
              <RuleDescription
                sections={descriptionSectionsByKey[RuleDescriptionSections.HOW_TO_FIX]}
              />
            )}

          {currentTab.value === TabKeys.Activity && activityTabContent}
        </div>
      </>
    );
  }
}
