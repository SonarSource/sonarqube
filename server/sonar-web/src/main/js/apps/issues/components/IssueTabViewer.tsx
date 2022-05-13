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
import * as React from 'react';
import BoxedTabs from '../../../components/controls/BoxedTabs';
import { translate } from '../../../helpers/l10n';
import { sanitizeString } from '../../../helpers/sanitize';
import { RuleDescriptionSections, RuleDetails } from '../../../types/types';

interface Props {
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
  content: string;
}

export enum TabKeys {
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

    const tabs = [
      {
        key: TabKeys.Code,
        label: translate('issue.tabs', TabKeys.Code),
        content: ''
      },
      {
        key: TabKeys.WhyIsThisAnIssue,
        label: translate('issue.tabs', TabKeys.WhyIsThisAnIssue),
        content: ruleDetails.descriptionSections?.find(section =>
          [RuleDescriptionSections.DEFAULT, RuleDescriptionSections.ROOT_CAUSE].includes(
            section.key
          )
        )?.content
      },
      {
        key: TabKeys.HowToFixIt,
        label: translate('issue.tabs', TabKeys.HowToFixIt),
        content: ruleDetails.descriptionSections?.find(
          section => section.key === RuleDescriptionSections.HOW_TO_FIX
        )?.content
      },
      {
        key: TabKeys.Resources,
        label: translate('issue.tabs', TabKeys.Resources),
        content: ruleDetails.descriptionSections?.find(
          section => section.key === RuleDescriptionSections.RESOURCES
        )?.content
      }
    ].filter(tab => tab.content !== undefined) as Array<Tab>;

    if (ruleDetails.htmlNote) {
      tabs[tabs.length - 1].content += '<br/>' + ruleDetails.htmlNote;
    }

    return tabs;
  }

  render() {
    const { codeTabContent } = this.props;
    const { tabs, currentTabKey } = this.state;

    return (
      <>
        <BoxedTabs onSelect={this.handleSelectTabs} selected={currentTabKey} tabs={tabs} />
        <div className="bordered huge-spacer-bottom">
          <div
            className={classNames('padded', {
              hidden: currentTabKey !== TabKeys.Code
            })}>
            {codeTabContent}
          </div>
          {tabs.slice(1).map(tab => (
            <div
              key={tab.key}
              className={classNames('markdown big-padded', {
                hidden: currentTabKey !== tab.key
              })}
              // eslint-disable-next-line react/no-danger
              dangerouslySetInnerHTML={{ __html: sanitizeString(tab.content) }}
            />
          ))}
        </div>
      </>
    );
  }
}
