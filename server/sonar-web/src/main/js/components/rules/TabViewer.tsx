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
import { debounce, Dictionary } from 'lodash';
import * as React from 'react';
import { dismissNotice, getCurrentUser } from '../../api/users';
import { RuleDescriptionSection, RuleDescriptionSections } from '../../apps/coding-rules/rule';
import { RuleDetails } from '../../types/types';
import { CurrentUser, NoticeType } from '../../types/users';
import BoxedTabs from '../controls/BoxedTabs';
import MoreInfoRuleDescription from './MoreInfoRuleDescription';
import RuleDescription from './RuleDescription';
import './style.css';

interface Props {
  ruleDetails: RuleDetails;
  codeTabContent?: React.ReactNode;
  computeTabs: (
    showNotice: boolean,
    educationPrinciplesRef: React.RefObject<HTMLDivElement>
  ) => Tab[];
  pageType?: string;
}

interface State {
  currentTab: Tab;
  tabs: Tab[];
}

export interface Tab {
  key: TabKeys;
  label: React.ReactNode;
  content: React.ReactNode;
}

export enum TabKeys {
  Code = 'code',
  WhyIsThisAnIssue = 'why',
  HowToFixIt = 'how_to_fix',
  AssessTheIssue = 'assess_the_problem',
  MoreInfo = 'more_info'
}

const DEBOUNCE_FOR_SCROLL = 250;

export default class TabViewer extends React.PureComponent<Props, State> {
  showNotification = false;
  educationPrinciplesRef: React.RefObject<HTMLDivElement>;

  constructor(props: Props) {
    super(props);
    const tabs = this.getUpdatedTabs(false);
    this.state = {
      tabs,
      currentTab: tabs[0]
    };
    this.educationPrinciplesRef = React.createRef();
    this.checkIfConceptIsVisible = debounce(this.checkIfConceptIsVisible, DEBOUNCE_FOR_SCROLL);
    document.addEventListener('scroll', this.checkIfConceptIsVisible, { capture: true });
  }

  componentDidMount() {
    this.getNotificationValue();
  }

  componentDidUpdate(prevProps: Props, prevState: State) {
    const { currentTab } = this.state;
    if (
      prevProps.ruleDetails !== this.props.ruleDetails ||
      prevProps.codeTabContent !== this.props.codeTabContent
    ) {
      const tabs = this.getUpdatedTabs(this.showNotification);
      this.getNotificationValue();
      this.setState({
        tabs,
        currentTab: tabs[0]
      });
    }
    if (currentTab.key === TabKeys.MoreInfo) {
      this.checkIfConceptIsVisible();
    }

    if (prevState.currentTab.key === TabKeys.MoreInfo && !this.showNotification) {
      const tabs = this.getUpdatedTabs(this.showNotification);
      this.setState({ tabs });
    }
  }

  componentWillUnmount() {
    document.removeEventListener('scroll', this.checkIfConceptIsVisible, { capture: true });
  }

  checkIfConceptIsVisible = () => {
    if (this.educationPrinciplesRef.current) {
      const rect = this.educationPrinciplesRef.current.getBoundingClientRect();
      const isView = rect.top <= (window.innerHeight || document.documentElement.clientHeight);
      if (isView && this.showNotification) {
        dismissNotice(NoticeType.EDUCATION_PRINCIPLES)
          .then(() => {
            document.removeEventListener('scroll', this.checkIfConceptIsVisible, { capture: true });
            this.showNotification = false;
          })
          .catch(() => {
            /* noop */
          });
      }
    }
  };

  getNotificationValue() {
    getCurrentUser()
      .then((data: CurrentUser) => {
        const educationPrinciplesDismissed = data.dismissedNotices[NoticeType.EDUCATION_PRINCIPLES];
        if (educationPrinciplesDismissed !== undefined) {
          this.showNotification = !educationPrinciplesDismissed;
          const tabs = this.getUpdatedTabs(!educationPrinciplesDismissed);
          this.setState({ tabs });
        }
      })
      .catch(() => {
        /* noop */
      });
  }

  handleSelectTabs = (currentTabKey: TabKeys) => {
    this.setState(({ tabs }) => ({
      currentTab: tabs.find(tab => tab.key === currentTabKey) || tabs[0]
    }));
  };

  getUpdatedTabs = (showNotification: boolean) => {
    return this.props.computeTabs(showNotification, this.educationPrinciplesRef);
  };

  render() {
    const { tabs, currentTab } = this.state;
    const { pageType } = this.props;
    return (
      <>
        <div
          className={classNames({
            'tab-view-header': pageType === 'issues'
          })}>
          <BoxedTabs
            className="bordered-bottom big-spacer-top"
            onSelect={this.handleSelectTabs}
            selected={currentTab.key}
            tabs={tabs}
          />
        </div>
        <div className="bordered-right bordered-left bordered-bottom huge-spacer-bottom">
          {currentTab.content}
        </div>
      </>
    );
  }
}

export const getMoreInfoTab = (
  showNotification: boolean,
  descriptionSectionsByKey: Dictionary<RuleDescriptionSection[]>,
  educationPrinciplesRef: React.RefObject<HTMLDivElement>,
  title: string,
  educationPrinciples?: string[]
) => {
  return {
    key: TabKeys.MoreInfo,
    label: showNotification ? (
      <div>
        {title}
        <div className="notice-dot" />
      </div>
    ) : (
      title
    ),
    content: ((educationPrinciples && educationPrinciples.length > 0) ||
      descriptionSectionsByKey[RuleDescriptionSections.RESOURCES]) && (
      <MoreInfoRuleDescription
        educationPrinciples={educationPrinciples}
        sections={descriptionSectionsByKey[RuleDescriptionSections.RESOURCES]}
        showNotification={showNotification}
        educationPrinciplesRef={educationPrinciplesRef}
      />
    )
  };
};

export const getHowToFixTab = (
  descriptionSectionsByKey: Dictionary<RuleDescriptionSection[]>,
  title: string,
  ruleDescriptionContextKey?: string
) => {
  return {
    key: TabKeys.HowToFixIt,
    label: title,
    content: descriptionSectionsByKey[RuleDescriptionSections.HOW_TO_FIX] && (
      <RuleDescription
        sections={descriptionSectionsByKey[RuleDescriptionSections.HOW_TO_FIX]}
        defaultContextKey={ruleDescriptionContextKey}
      />
    )
  };
};

export const getWhyIsThisAnIssueTab = (
  rootCauseDescriptionSections: RuleDescriptionSection[],
  descriptionSectionsByKey: Dictionary<RuleDescriptionSection[]>,
  title: string,
  ruleDescriptionContextKey?: string
) => {
  return {
    key: TabKeys.WhyIsThisAnIssue,
    label: title,
    content: rootCauseDescriptionSections && (
      <RuleDescription
        sections={rootCauseDescriptionSections}
        isDefault={descriptionSectionsByKey[RuleDescriptionSections.DEFAULT] !== undefined}
        defaultContextKey={ruleDescriptionContextKey}
      />
    )
  };
};
