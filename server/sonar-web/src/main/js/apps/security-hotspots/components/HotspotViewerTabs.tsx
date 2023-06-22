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
import { BranchLike } from '../../../types/branch-like';
import { Standards } from '../../../types/security';
import { Hotspot, HotspotStatusOption } from '../../../types/security-hotspots';
import { Component } from '../../../types/types';
import { RuleDescriptionSection, RuleDescriptionSections } from '../../coding-rules/rule';
import useScrollDownCompress from '../hooks/useScrollDownCompress';
import { HotspotHeader } from './HotspotHeader';

interface Props {
  activityTabContent: React.ReactNode;
  branchLike?: BranchLike;
  codeTabContent: React.ReactNode;
  component: Component;
  hotspot: Hotspot;
  onUpdateHotspot: (statusUpdate?: boolean, statusOption?: HotspotStatusOption) => Promise<void>;
  ruleDescriptionSections?: RuleDescriptionSection[];
  ruleLanguage?: string;
  standards?: Standards;
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

const STICKY_HEADER_SHADOW_OFFSET = 24;
const STICKY_HEADER_COMPRESS_THRESHOLD = 200;

export default function HotspotViewerTabs(props: Props) {
  const {
    activityTabContent,
    branchLike,
    codeTabContent,
    component,
    hotspot,
    ruleDescriptionSections,
    ruleLanguage,
    standards,
  } = props;

  const { isScrolled, isCompressed, resetScrollDownCompress } = useScrollDownCompress(
    STICKY_HEADER_COMPRESS_THRESHOLD,
    STICKY_HEADER_SHADOW_OFFSET
  );

  const tabs = React.useMemo(() => {
    const descriptionSectionsByKey = groupBy(ruleDescriptionSections, (section) => section.key);
    const labelSuffix = isCompressed ? '.short' : '';

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
  }, [isCompressed, ruleDescriptionSections, hotspot.comment]);

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
    resetScrollDownCompress();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentTab]);

  const descriptionSectionsByKey = groupBy(ruleDescriptionSections, (section) => section.key);

  const rootCauseDescriptionSections =
    descriptionSectionsByKey[RuleDescriptionSections.DEFAULT] ||
    descriptionSectionsByKey[RuleDescriptionSections.ROOT_CAUSE];

  return (
    <>
      <HotspotHeader
        branchLike={branchLike}
        component={component}
        hotspot={hotspot}
        isCompressed={isCompressed}
        isScrolled={isScrolled}
        onUpdateHotspot={props.onUpdateHotspot}
        standards={standards}
        tabs={
          <ToggleButton
            role="tablist"
            value={currentTab.value}
            options={tabs}
            onChange={handleSelectTabs}
          />
        }
      />
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
