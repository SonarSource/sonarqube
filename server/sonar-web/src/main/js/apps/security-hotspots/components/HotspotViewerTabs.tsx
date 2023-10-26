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

import styled from '@emotion/styled';
import {
  LAYOUT_GLOBAL_NAV_HEIGHT,
  LAYOUT_PROJECT_NAV_HEIGHT,
  ToggleButton,
  getTabId,
  getTabPanelId,
  themeColor,
  themeShadow,
} from 'design-system';
import { groupBy, omit } from 'lodash';
import * as React from 'react';
import RuleDescription from '../../../components/rules/RuleDescription';
import { isInput, isShortcut } from '../../../helpers/keyboardEventHelpers';
import { KeyboardKeys } from '../../../helpers/keycodes';
import { translate } from '../../../helpers/l10n';
import { useRefreshBranchStatus } from '../../../queries/branch';
import { BranchLike } from '../../../types/branch-like';
import { Hotspot, HotspotStatusOption } from '../../../types/security-hotspots';
import { Component } from '../../../types/types';
import { RuleDescriptionSection, RuleDescriptionSections } from '../../coding-rules/rule';
import useStickyDetection from '../hooks/useStickyDetection';
import HotspotSnippetHeader from './HotspotSnippetHeader';
import StatusReviewButton from './status/StatusReviewButton';

interface Props {
  activityTabContent: React.ReactNode;
  codeTabContent: React.ReactNode;
  hotspot: Hotspot;
  onUpdateHotspot: (statusUpdate?: boolean, statusOption?: HotspotStatusOption) => Promise<void>;
  ruleDescriptionSections?: RuleDescriptionSection[];
  ruleLanguage?: string;
  component: Component;
  branchLike?: BranchLike;
}

interface Tab {
  counter?: number;
  label: string;
  value: TabKeys;
}

export enum TabKeys {
  Code = 'code',
  RiskDescription = 'risk',
  VulnerabilityDescription = 'vulnerability',
  FixRecommendation = 'fix',
  Activity = 'activity',
}

const TABS_OFFSET = LAYOUT_GLOBAL_NAV_HEIGHT + LAYOUT_PROJECT_NAV_HEIGHT;

export default function HotspotViewerTabs(props: Props) {
  const {
    activityTabContent,
    codeTabContent,
    hotspot,
    ruleDescriptionSections,
    ruleLanguage,
    component,
    branchLike,
  } = props;

  const refreshBranchStatus = useRefreshBranchStatus();
  const isSticky = useStickyDetection('.hotspot-tabs', {
    offset: TABS_OFFSET,
  });

  const tabs = React.useMemo(() => {
    const descriptionSectionsByKey = groupBy(ruleDescriptionSections, (section) => section.key);
    const labelSuffix = isSticky ? '.short' : '';

    return [
      {
        value: TabKeys.Code,
        label: translate(`hotspots.tabs.code${labelSuffix}`),
        show: true,
      },
      {
        value: TabKeys.RiskDescription,
        label: translate(`hotspots.tabs.risk_description${labelSuffix}`),
        show:
          descriptionSectionsByKey[RuleDescriptionSections.DEFAULT] ||
          descriptionSectionsByKey[RuleDescriptionSections.ROOT_CAUSE],
      },
      {
        value: TabKeys.VulnerabilityDescription,
        label: translate(`hotspots.tabs.vulnerability_description${labelSuffix}`),
        show: descriptionSectionsByKey[RuleDescriptionSections.ASSESS_THE_PROBLEM] !== undefined,
      },
      {
        value: TabKeys.FixRecommendation,
        label: translate(`hotspots.tabs.fix_recommendations${labelSuffix}`),
        show: descriptionSectionsByKey[RuleDescriptionSections.HOW_TO_FIX] !== undefined,
      },
      {
        value: TabKeys.Activity,
        label: translate(`hotspots.tabs.activity${labelSuffix}`),
        show: true,
        counter: hotspot.comment.length,
      },
    ]
      .filter((tab) => tab.show)
      .map((tab) => omit(tab, 'show'));
  }, [isSticky, ruleDescriptionSections, hotspot.comment]);

  const [currentTab, setCurrentTab] = React.useState<Tab>(tabs[0]);

  const handleKeyboardNavigation = (event: KeyboardEvent) => {
    if (isInput(event) || isShortcut(event)) {
      return true;
    }

    if (event.key === KeyboardKeys.LeftArrow) {
      event.preventDefault();
      selectNeighboringTab(-1);
    } else if (event.key === KeyboardKeys.RightArrow) {
      event.preventDefault();
      selectNeighboringTab(+1);
    }
  };

  const selectNeighboringTab = (shift: number) => {
    setCurrentTab((currentTab) => {
      const index = currentTab && tabs.findIndex((tab) => tab.value === currentTab.value);

      if (index !== undefined && index > -1) {
        const newIndex = Math.max(0, Math.min(tabs.length - 1, index + shift));
        return tabs[newIndex];
      }

      return currentTab;
    });
  };

  const handleSelectTabs = (tabKey: TabKeys) => {
    const currentTab = tabs.find((tab) => tab.value === tabKey);

    if (currentTab) {
      setCurrentTab(currentTab);
    }
  };

  const handleStatusChange = async (statusOption: HotspotStatusOption) => {
    await props.onUpdateHotspot(true, statusOption);
    refreshBranchStatus();
  };

  React.useEffect(() => {
    document.addEventListener('keydown', handleKeyboardNavigation);

    return () => document.removeEventListener('keydown', handleKeyboardNavigation);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  React.useEffect(() => {
    setCurrentTab(tabs[0]);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hotspot.key]);

  React.useEffect(() => {
    if (currentTab.value !== TabKeys.Code) {
      window.scrollTo({ top: 0 });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentTab]);

  const descriptionSectionsByKey = groupBy(ruleDescriptionSections, (section) => section.key);

  const rootCauseDescriptionSections =
    descriptionSectionsByKey[RuleDescriptionSections.DEFAULT] ||
    descriptionSectionsByKey[RuleDescriptionSections.ROOT_CAUSE];

  return (
    <>
      <StickyTabs
        isSticky={isSticky}
        top={TABS_OFFSET}
        className="sw-sticky sw-py-4 sw--mx-6 sw-px-6 sw-z-filterbar-header hotspot-tabs"
      >
        <div className="sw-flex sw-justify-between">
          <ToggleButton
            role="tablist"
            value={currentTab.value}
            options={tabs}
            onChange={handleSelectTabs}
          />
          {isSticky && <StatusReviewButton hotspot={hotspot} onStatusChange={handleStatusChange} />}
        </div>
        {currentTab.value === TabKeys.Code && codeTabContent && (
          <HotspotSnippetHeader hotspot={hotspot} component={component} branchLike={branchLike} />
        )}
      </StickyTabs>
      <div
        aria-labelledby={getTabId(currentTab.value)}
        className="sw-mt-2"
        id={getTabPanelId(currentTab.value)}
        role="tabpanel"
      >
        {currentTab.value === TabKeys.Code && codeTabContent}

        {currentTab.value === TabKeys.RiskDescription && rootCauseDescriptionSections && (
          <RuleDescription language={ruleLanguage} sections={rootCauseDescriptionSections} />
        )}

        {currentTab.value === TabKeys.VulnerabilityDescription &&
          descriptionSectionsByKey[RuleDescriptionSections.ASSESS_THE_PROBLEM] && (
            <RuleDescription
              language={ruleLanguage}
              sections={descriptionSectionsByKey[RuleDescriptionSections.ASSESS_THE_PROBLEM]}
            />
          )}

        {currentTab.value === TabKeys.FixRecommendation &&
          descriptionSectionsByKey[RuleDescriptionSections.HOW_TO_FIX] && (
            <RuleDescription
              language={ruleLanguage}
              sections={descriptionSectionsByKey[RuleDescriptionSections.HOW_TO_FIX]}
            />
          )}

        {currentTab.value === TabKeys.Activity && activityTabContent}
      </div>
    </>
  );
}

const StickyTabs = styled.div<{ top: number; isSticky: boolean }>`
  background-color: ${themeColor('pageBlock')};
  box-shadow: ${({ isSticky }) => (isSticky ? themeShadow('sm') : 'none')};
  top: ${({ top }) => top}px;
`;
