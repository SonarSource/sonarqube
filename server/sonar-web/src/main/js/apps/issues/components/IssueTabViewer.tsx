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
import { translate } from '../../../helpers/l10n';
import { sanitizeString } from '../../../helpers/sanitize';
import { getRuleUrl } from '../../../helpers/urls';
import { Component, Issue, RuleDetails } from '../../../types/types';
import RuleContextDescription from '../../../components/rules/RuleContextDescription';
import { RuleDescriptionSection, RuleDescriptionSections } from '../../coding-rules/rule';

interface Props {
  component?: Component;
  issue: Issue;
  codeTabContent: React.ReactNode;
  ruleDetails: RuleDetails;
}

interface State {
  currentTabKey: TabKeys;
  tabs: Tab[];
}

interface Tab {
  key: TabKeys;
  label: React.ReactNode;
  descriptionSections: RuleDescriptionSection[];
  isDefault: boolean;
}

enum TabKeys {
  Code = 'code',
  WhyIsThisAnIssue = 'why',
  HowToFixIt = 'how',
  Resources = 'resources'
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
    if (prevProps.ruleDetails !== this.props.ruleDetails) {
      const tabs = this.computeTabs();
      this.setState({
        currentTabKey: tabs[0].key,
        tabs
      });
    }
  }

  handleSelectTabs = (currentTabKey: TabKeys) => {
    this.setState({ currentTabKey });
  };

  computeTabs() {
    const { ruleDetails } = this.props;
    const groupedDescriptions = groupBy(ruleDetails.descriptionSections, 'key');

    if (ruleDetails.htmlNote) {
      if (groupedDescriptions[RuleDescriptionSections.RESOURCES] !== undefined) {
        // We add the extended description (htmlNote) in the first context, in case there are contexts
        // Extended description will get reworked in future
        groupedDescriptions[RuleDescriptionSections.RESOURCES][0].content +=
          '<br/>' + ruleDetails.htmlNote;
      } else {
        groupedDescriptions[RuleDescriptionSections.RESOURCES] = [
          {
            key: RuleDescriptionSections.RESOURCES,
            content: ruleDetails.htmlNote
          }
        ];
      }
    }

    return [
      {
        key: TabKeys.Code,
        label: translate('issue.tabs', TabKeys.Code),
        descriptionSections: []
      },
      {
        key: TabKeys.WhyIsThisAnIssue,
        label: translate('issue.tabs', TabKeys.WhyIsThisAnIssue),
        descriptionSections:
          groupedDescriptions[RuleDescriptionSections.DEFAULT] ||
          groupedDescriptions[RuleDescriptionSections.ROOT_CAUSE],
        isDefault:
          ruleDetails.descriptionSections?.filter(
            section => section.key === RuleDescriptionSections.DEFAULT
          ) !== undefined
      },
      {
        key: TabKeys.HowToFixIt,
        label: translate('issue.tabs', TabKeys.HowToFixIt),
        descriptionSections: groupedDescriptions[RuleDescriptionSections.HOW_TO_FIX],
        isDefault: false
      },
      {
        key: TabKeys.Resources,
        label: translate('issue.tabs', TabKeys.Resources),
        descriptionSections: groupedDescriptions[RuleDescriptionSections.RESOURCES],
        isDefault: false
      }
    ].filter(tab => tab.descriptionSections) as Array<Tab>;
  }

  render() {
    const {
      component,
      codeTabContent,
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
            {selectedTab.key === TabKeys.Code && <div className="padded">{codeTabContent}</div>}
            {selectedTab.key !== TabKeys.Code &&
              (selectedTab.descriptionSections.length === 1 &&
              !selectedTab.descriptionSections[0].context ? (
                <div
                  key={selectedTab.key}
                  className={classNames('big-padded', {
                    markdown: selectedTab.isDefault,
                    'rule-desc': !selectedTab.isDefault
                  })}
                  // eslint-disable-next-line react/no-danger
                  dangerouslySetInnerHTML={{
                    __html: sanitizeString(selectedTab.descriptionSections[0].content)
                  }}
                />
              ) : (
                <div
                  key={selectedTab.key}
                  className={classNames('big-padded', {
                    markdown: selectedTab.isDefault,
                    'rule-desc': !selectedTab.isDefault
                  })}>
                  <RuleContextDescription description={selectedTab.descriptionSections} />
                </div>
              ))}
          </div>
        )}
      </>
    );
  }
}
