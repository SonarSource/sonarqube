/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import BoxedTabs from '../../../components/controls/BoxedTabs';
import { isInput, isShortcut } from '../../../helpers/keyboardEventHelpers';
import { KeyboardKeys } from '../../../helpers/keycodes';
import { translate } from '../../../helpers/l10n';
import { sanitizeString } from '../../../helpers/sanitize';
import { Hotspot } from '../../../types/security-hotspots';
import RuleContextDescription from '../../../components/rules/RuleContextDescription';
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
  descriptionSections: RuleDescriptionSection[];
}

export enum TabKeys {
  Code = 'code',
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

  componentDidMount() {
    this.registerKeyboardEvents();
  }

  componentDidUpdate(prevProps: Props) {
    if (this.props.hotspot.key !== prevProps.hotspot.key) {
      const tabs = this.computeTabs();
      this.setState({
        currentTab: tabs[0],
        tabs
      });
    } else if (
      this.props.selectedHotspotLocation !== undefined &&
      this.props.selectedHotspotLocation !== prevProps.selectedHotspotLocation
    ) {
      const { tabs } = this.state;
      this.setState({
        currentTab: tabs[0]
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
    const currentTab = tabs.find(tab => tab.key === tabKey)!;
    this.setState({ currentTab });
  };

  computeTabs() {
    const { ruleDescriptionSections } = this.props;
    const groupedDescriptions = groupBy(ruleDescriptionSections, description => description.key);

    const descriptionTabs = [
      {
        key: TabKeys.RiskDescription,
        label: translate('hotspots.tabs.risk_description'),
        descriptionSections:
          groupedDescriptions[RuleDescriptionSections.DEFAULT] ||
          groupedDescriptions[RuleDescriptionSections.ROOT_CAUSE]
      },
      {
        key: TabKeys.VulnerabilityDescription,
        label: translate('hotspots.tabs.vulnerability_description'),
        descriptionSections: groupedDescriptions[RuleDescriptionSections.ASSESS_THE_PROBLEM]
      },
      {
        key: TabKeys.FixRecommendation,
        label: translate('hotspots.tabs.fix_recommendations'),
        descriptionSections: groupedDescriptions[RuleDescriptionSections.HOW_TO_FIX]
      }
    ].filter(tab => tab.descriptionSections);

    return [
      {
        key: TabKeys.Code,
        label: translate('hotspots.tabs.code'),
        descriptionSections: []
      },
      ...descriptionTabs
    ];
  }

  selectNeighboringTab(shift: number) {
    this.setState(({ tabs, currentTab }) => {
      const index = currentTab && tabs.findIndex(tab => tab.key === currentTab.key);

      if (index !== undefined && index > -1) {
        const newIndex = Math.max(0, Math.min(tabs.length - 1, index + shift));
        return {
          currentTab: tabs[newIndex]
        };
      }

      return { currentTab };
    });
  }

  render() {
    const { codeTabContent } = this.props;
    const { tabs, currentTab } = this.state;
    return (
      <>
        <BoxedTabs onSelect={this.handleSelectTabs} selected={currentTab.key} tabs={tabs} />
        <div className="bordered huge-spacer-bottom">
          {currentTab.key === TabKeys.Code && <div className="padded">{codeTabContent}</div>}
          {currentTab.key !== TabKeys.Code &&
            (currentTab.descriptionSections.length === 1 &&
            !currentTab.descriptionSections[0].context ? (
              <div
                key={currentTab.key}
                className="markdown big-padded"
                // eslint-disable-next-line react/no-danger
                dangerouslySetInnerHTML={{
                  __html: sanitizeString(currentTab.descriptionSections[0].content)
                }}
              />
            ) : (
              <div className="markdown big-padded">
                <RuleContextDescription description={currentTab.descriptionSections} />
              </div>
            ))}
        </div>
      </>
    );
  }
}
