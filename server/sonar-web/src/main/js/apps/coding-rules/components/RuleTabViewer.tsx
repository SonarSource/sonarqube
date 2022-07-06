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
import MoreInfoRuleDescription from '../../../components/rules/MoreInfoRuleDescription';
import RuleDescription from '../../../components/rules/RuleDescription';
import { translate } from '../../../helpers/l10n';
import { sanitizeString } from '../../../helpers/sanitize';
import { RuleDetails } from '../../../types/types';
import { RuleDescriptionSections } from '../rule';

interface Props {
  ruleDetails: RuleDetails;
}

interface State {
  currentTab: Tab;
  tabs: Tab[];
}

interface Tab {
  key: RuleTabKeys;
  label: React.ReactNode;
  content: React.ReactNode;
}

enum RuleTabKeys {
  WhyIsThisAnIssue = 'why',
  HowToFixIt = 'how_to_fix',
  AssessTheIssue = 'assess_the_problem',
  MoreInfo = 'more_info'
}

export default class RuleViewerTabs extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = this.computeState();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.ruleDetails !== this.props.ruleDetails) {
      this.setState(this.computeState());
    }
  }

  handleSelectTabs = (currentTabKey: RuleTabKeys) => {
    this.setState(({ tabs }) => ({
      currentTab: tabs.find(tab => tab.key === currentTabKey) || tabs[0]
    }));
  };

  computeState() {
    const { ruleDetails } = this.props;
    const descriptionSectionsByKey = groupBy(
      ruleDetails.descriptionSections,
      section => section.key
    );

    const tabs = [
      {
        key: RuleTabKeys.WhyIsThisAnIssue,
        label:
          ruleDetails.type === 'SECURITY_HOTSPOT'
            ? translate('coding_rules.description_section.title.root_cause.SECURITY_HOTSPOT')
            : translate('coding_rules.description_section.title.root_cause'),
        content: descriptionSectionsByKey[RuleDescriptionSections.ROOT_CAUSE] && (
          <RuleDescription
            sections={descriptionSectionsByKey[RuleDescriptionSections.ROOT_CAUSE]}
          />
        )
      },
      {
        key: RuleTabKeys.AssessTheIssue,
        label: translate('coding_rules.description_section.title', RuleTabKeys.AssessTheIssue),
        content: descriptionSectionsByKey[RuleDescriptionSections.ASSESS_THE_PROBLEM] && (
          <RuleDescription
            sections={descriptionSectionsByKey[RuleDescriptionSections.ASSESS_THE_PROBLEM]}
          />
        )
      },
      {
        key: RuleTabKeys.HowToFixIt,
        label: translate('coding_rules.description_section.title', RuleTabKeys.HowToFixIt),
        content: descriptionSectionsByKey[RuleDescriptionSections.HOW_TO_FIX] && (
          <RuleDescription
            sections={descriptionSectionsByKey[RuleDescriptionSections.HOW_TO_FIX]}
          />
        )
      },
      {
        key: RuleTabKeys.MoreInfo,
        label: translate('coding_rules.description_section.title', RuleTabKeys.MoreInfo),
        content: (ruleDetails.genericConcepts ||
          descriptionSectionsByKey[RuleDescriptionSections.RESOURCES]) && (
          <MoreInfoRuleDescription
            genericConcepts={ruleDetails.genericConcepts}
            sections={descriptionSectionsByKey[RuleDescriptionSections.RESOURCES]}
          />
        )
      }
    ].filter(tab => tab.content) as Array<Tab>;

    return {
      currentTab: tabs[0],
      tabs
    };
  }

  render() {
    const { ruleDetails } = this.props;
    const { tabs, currentTab } = this.state;
    const intro = ruleDetails.descriptionSections?.find(
      section => section.key === RuleDescriptionSections.INTRODUCTION
    )?.content;
    return (
      <>
        {intro && (
          <div
            className="rule-desc"
            // eslint-disable-next-line react/no-danger
            dangerouslySetInnerHTML={{ __html: sanitizeString(intro) }}
          />
        )}
        <BoxedTabs
          className="bordered-bottom big-spacer-top"
          onSelect={this.handleSelectTabs}
          selected={currentTab.key}
          tabs={tabs}
        />

        <div className="bordered-right bordered-left bordered-bottom huge-spacer-bottom">
          {currentTab.content}
        </div>
      </>
    );
  }
}
