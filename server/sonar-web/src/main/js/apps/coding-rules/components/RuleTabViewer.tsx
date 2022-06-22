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
import * as React from 'react';
import BoxedTabs from '../../../components/controls/BoxedTabs';
import { translate } from '../../../helpers/l10n';
import { sanitizeString } from '../../../helpers/sanitize';
import { RuleDescriptionSections, RuleDetails } from '../../../types/types';

interface Props {
  ruleDetails: RuleDetails;
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

enum TabKeys {
  WhyIsThisAnIssue = 'why',
  HowToFixIt = 'how_to_fix',
  AssessTheIssue = 'assess_the_problem',
  Resources = 'resources'
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

  handleSelectTabs = (currentTabKey: TabKeys) => {
    this.setState(({ tabs }) => ({
      currentTab: tabs.find(tab => tab.key === currentTabKey) || tabs[0]
    }));
  };

  computeState() {
    const { ruleDetails } = this.props;

    const tabs = [
      {
        key: TabKeys.WhyIsThisAnIssue,
        label:
          ruleDetails.type === 'SECURITY_HOTSPOT'
            ? translate('coding_rules.description_section.title.root_cause.SECURITY_HOTSPOT')
            : translate('coding_rules.description_section.title.root_cause'),
        content: ruleDetails.descriptionSections?.find(
          section => section.key === RuleDescriptionSections.ROOT_CAUSE
        )?.content
      },
      {
        key: TabKeys.AssessTheIssue,
        label: translate('coding_rules.description_section.title', TabKeys.AssessTheIssue),
        content: ruleDetails.descriptionSections?.find(
          section => section.key === RuleDescriptionSections.ASSESS_THE_PROBLEM
        )?.content
      },
      {
        key: TabKeys.HowToFixIt,
        label: translate('coding_rules.description_section.title', TabKeys.HowToFixIt),
        content: ruleDetails.descriptionSections?.find(
          section => section.key === RuleDescriptionSections.HOW_TO_FIX
        )?.content
      },
      {
        key: TabKeys.Resources,
        label: translate('coding_rules.description_section.title', TabKeys.Resources),
        content: ruleDetails.descriptionSections?.find(
          section => section.key === RuleDescriptionSections.RESOURCES
        )?.content
      }
    ].filter(tab => tab.content !== undefined) as Array<Tab>;

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
          <>
            <h2>{translate('coding_rules.description_section.title.introduction')}</h2>
            <div
              className="big-padded rule-desc"
              // eslint-disable-next-line react/no-danger
              dangerouslySetInnerHTML={{ __html: sanitizeString(intro) }}
            />
          </>
        )}
        <BoxedTabs
          className="bordered-bottom"
          onSelect={this.handleSelectTabs}
          selected={currentTab.key}
          tabs={tabs}
        />

        <div className="bordered-right bordered-left bordered-bottom huge-spacer-bottom">
          <div
            className="big-padded rule-desc"
            // eslint-disable-next-line react/no-danger
            dangerouslySetInnerHTML={{ __html: sanitizeString(currentTab.content) }}
          />
        </div>
      </>
    );
  }
}
