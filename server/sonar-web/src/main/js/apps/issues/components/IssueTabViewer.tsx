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
import classNames from 'classnames';
import { groupBy } from 'lodash';
import * as React from 'react';
import { Link } from 'react-router-dom';
import BoxedTabs from '../../../components/controls/BoxedTabs';
import MoreInfoRuleDescription from '../../../components/rules/MoreInfoRuleDescription';
import RuleDescription from '../../../components/rules/RuleDescription';
import { translate } from '../../../helpers/l10n';
import { getRuleUrl } from '../../../helpers/urls';
import { Component, Issue, RuleDetails } from '../../../types/types';
import { RuleDescriptionSections } from '../../coding-rules/rule';

interface Props {
  component?: Component;
  issue: Issue;
  codeTabContent: React.ReactNode;
  ruleDetails: RuleDetails;
}

interface State {
  currentTabKey: IssueTabKeys;
  tabs: Tab[];
}

interface Tab {
  key: IssueTabKeys;
  label: React.ReactNode;
  content: React.ReactNode;
}

enum IssueTabKeys {
  Code = 'code',
  WhyIsThisAnIssue = 'why',
  HowToFixIt = 'how',
  MoreInfo = 'more_info'
}

export default class IssueViewerTabs extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    const tabs = this.computeTabs();
    this.state = {
      currentTabKey: tabs[0].key,
      tabs
    };
  }

  componentDidUpdate(prevProps: Props) {
    if (
      prevProps.ruleDetails !== this.props.ruleDetails ||
      prevProps.codeTabContent !== this.props.codeTabContent
    ) {
      const tabs = this.computeTabs();
      this.setState({
        currentTabKey: tabs[0].key,
        tabs
      });
    }
  }

  handleSelectTabs = (currentTabKey: IssueTabKeys) => {
    this.setState({ currentTabKey });
  };

  computeTabs() {
    const {
      ruleDetails,
      codeTabContent,
      issue: { ruleDescriptionContextKey }
    } = this.props;
    const descriptionSectionsByKey = groupBy(
      ruleDetails.descriptionSections,
      section => section.key
    );

    if (ruleDetails.htmlNote) {
      if (descriptionSectionsByKey[RuleDescriptionSections.RESOURCES] !== undefined) {
        // We add the extended description (htmlNote) in the first context, in case there are contexts
        // Extended description will get reworked in future
        descriptionSectionsByKey[RuleDescriptionSections.RESOURCES][0].content +=
          '<br/>' + ruleDetails.htmlNote;
      } else {
        descriptionSectionsByKey[RuleDescriptionSections.RESOURCES] = [
          {
            key: RuleDescriptionSections.RESOURCES,
            content: ruleDetails.htmlNote
          }
        ];
      }
    }

    const rootCauseDescriptionSections =
      descriptionSectionsByKey[RuleDescriptionSections.DEFAULT] ||
      descriptionSectionsByKey[RuleDescriptionSections.ROOT_CAUSE];

    return [
      {
        key: IssueTabKeys.Code,
        label: translate('issue.tabs', IssueTabKeys.Code),
        content: <div className="padded">{codeTabContent}</div>
      },
      {
        key: IssueTabKeys.WhyIsThisAnIssue,
        label: translate('issue.tabs', IssueTabKeys.WhyIsThisAnIssue),
        content: rootCauseDescriptionSections && (
          <RuleDescription
            sections={rootCauseDescriptionSections}
            isDefault={descriptionSectionsByKey[RuleDescriptionSections.DEFAULT] !== undefined}
            defaultContextKey={ruleDescriptionContextKey}
          />
        )
      },
      {
        key: IssueTabKeys.HowToFixIt,
        label: translate('issue.tabs', IssueTabKeys.HowToFixIt),
        content: descriptionSectionsByKey[RuleDescriptionSections.HOW_TO_FIX] && (
          <RuleDescription
            sections={descriptionSectionsByKey[RuleDescriptionSections.HOW_TO_FIX]}
            defaultContextKey={ruleDescriptionContextKey}
          />
        )
      },
      {
        key: IssueTabKeys.MoreInfo,
        label: translate('issue.tabs', IssueTabKeys.MoreInfo),
        content: (ruleDetails.genericConcepts ||
          descriptionSectionsByKey[RuleDescriptionSections.RESOURCES]) && (
          <MoreInfoRuleDescription
            genericConcepts={ruleDetails.genericConcepts}
            sections={descriptionSectionsByKey[RuleDescriptionSections.RESOURCES]}
          />
        )
      }
    ].filter(tab => tab.content) as Array<Tab>;
  }

  render() {
    const {
      component,
      ruleDetails: { name, key },
      issue: { message }
    } = this.props;
    const { tabs, currentTabKey } = this.state;
    const selectedTab = tabs.find(tab => tab.key === currentTabKey);
    return (
      <>
        <div
          className={classNames('issue-header', {
            'issue-project-level': component !== undefined
          })}>
          <h1 className="text-bold">{message}</h1>
          <div className="spacer-top big-spacer-bottom">
            <span className="note padded-right">{name}</span>
            <Link className="small" to={getRuleUrl(key)} target="_blank">
              {key}
            </Link>
          </div>
          <BoxedTabs
            className="bordered-bottom"
            onSelect={this.handleSelectTabs}
            selected={currentTabKey}
            tabs={tabs}
          />
        </div>
        {selectedTab && (
          <div className="bordered-right bordered-left bordered-bottom huge-spacer-bottom">
            {selectedTab.content}
          </div>
        )}
      </>
    );
  }
}
