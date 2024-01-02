/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { cloneDeep, debounce, groupBy } from 'lodash';
import * as React from 'react';
import { Location } from 'react-router-dom';
import { dismissNotice } from '../../api/users';
import { CurrentUserContextInterface } from '../../app/components/current-user/CurrentUserContext';
import withCurrentUserContext from '../../app/components/current-user/withCurrentUserContext';
import { RuleDescriptionSections } from '../../apps/coding-rules/rule';
import { translate } from '../../helpers/l10n';
import { RuleDetails } from '../../types/types';
import { NoticeType } from '../../types/users';
import ScreenPositionHelper from '../common/ScreenPositionHelper';
import BoxedTabs, { getTabId, getTabPanelId } from '../controls/BoxedTabs';
import withLocation from '../hoc/withLocation';
import MoreInfoRuleDescription from './MoreInfoRuleDescription';
import RuleDescription from './RuleDescription';
import './style.css';

interface RuleTabViewerProps extends CurrentUserContextInterface {
  ruleDetails: RuleDetails;
  extendedDescription?: string;
  ruleDescriptionContextKey?: string;
  codeTabContent?: React.ReactNode;
  scrollInTab?: boolean;
  location: Location;
}

interface State {
  tabs: Tab[];
  selectedTab?: Tab;
  displayEducationalPrinciplesNotification?: boolean;
  educationalPrinciplesNotificationHasBeenDismissed?: boolean;
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
  MoreInfo = 'more_info',
}

const DEBOUNCE_FOR_SCROLL = 250;

export class RuleTabViewer extends React.PureComponent<RuleTabViewerProps, State> {
  state: State = {
    tabs: [],
  };

  educationPrinciplesRef: React.RefObject<HTMLDivElement>;

  constructor(props: RuleTabViewerProps) {
    super(props);
    this.educationPrinciplesRef = React.createRef();
    this.checkIfEducationPrinciplesAreVisible = debounce(
      this.checkIfEducationPrinciplesAreVisible,
      DEBOUNCE_FOR_SCROLL
    );
  }

  componentDidMount() {
    this.setState((prevState) => this.computeState(prevState));
    this.attachScrollEvent();

    const tabs = this.computeTabs(Boolean(this.state.displayEducationalPrinciplesNotification));

    const query = new URLSearchParams(this.props.location.search);
    if (query.has('why')) {
      this.setState({
        selectedTab: tabs.find((tab) => tab.key === TabKeys.WhyIsThisAnIssue) || tabs[0],
      });
    }
  }

  componentDidUpdate(prevProps: RuleTabViewerProps, prevState: State) {
    const { ruleDetails, codeTabContent, ruleDescriptionContextKey, currentUser } = this.props;
    const { selectedTab } = this.state;

    if (
      prevProps.ruleDetails.key !== ruleDetails.key ||
      prevProps.ruleDescriptionContextKey !== ruleDescriptionContextKey ||
      prevProps.codeTabContent !== codeTabContent ||
      prevProps.currentUser !== currentUser
    ) {
      this.setState((pState) =>
        this.computeState(
          pState,
          prevProps.ruleDetails !== ruleDetails || prevProps.codeTabContent !== codeTabContent
        )
      );
    }

    if (selectedTab?.key === TabKeys.MoreInfo) {
      this.checkIfEducationPrinciplesAreVisible();
    }

    if (
      prevState.selectedTab?.key === TabKeys.MoreInfo &&
      prevState.displayEducationalPrinciplesNotification &&
      prevState.educationalPrinciplesNotificationHasBeenDismissed
    ) {
      this.props.updateDismissedNotices(NoticeType.EDUCATION_PRINCIPLES, true);
    }
  }

  componentWillUnmount() {
    this.detachScrollEvent();
  }

  computeState = (prevState: State, resetSelectedTab = false) => {
    const {
      ruleDetails,
      currentUser: { isLoggedIn, dismissedNotices },
    } = this.props;

    const displayEducationalPrinciplesNotification =
      !!ruleDetails.educationPrinciples &&
      ruleDetails.educationPrinciples.length > 0 &&
      isLoggedIn &&
      !dismissedNotices[NoticeType.EDUCATION_PRINCIPLES];
    const tabs = this.computeTabs(displayEducationalPrinciplesNotification);

    return {
      tabs,
      selectedTab: resetSelectedTab || !prevState.selectedTab ? tabs[0] : prevState.selectedTab,
      displayEducationalPrinciplesNotification,
    };
  };

  computeTabs = (displayEducationalPrinciplesNotification: boolean) => {
    const {
      codeTabContent,
      ruleDetails: { descriptionSections, educationPrinciples, type: ruleType },
      ruleDescriptionContextKey,
      extendedDescription,
    } = this.props;

    // As we might tamper with the description later on, we clone to avoid any side effect
    const descriptionSectionsByKey = cloneDeep(
      groupBy(descriptionSections, (section) => section.key)
    );

    if (extendedDescription) {
      if (descriptionSectionsByKey[RuleDescriptionSections.RESOURCES]?.length > 0) {
        // We add the extended description (htmlNote) in the first context, in case there are contexts
        // Extended description will get reworked in future
        descriptionSectionsByKey[RuleDescriptionSections.RESOURCES][0].content +=
          '<br/>' + extendedDescription;
      } else {
        descriptionSectionsByKey[RuleDescriptionSections.RESOURCES] = [
          {
            key: RuleDescriptionSections.RESOURCES,
            content: extendedDescription,
          },
        ];
      }
    }

    const tabs: Tab[] = [
      {
        key: TabKeys.WhyIsThisAnIssue,
        label:
          ruleType === 'SECURITY_HOTSPOT'
            ? translate('coding_rules.description_section.title.root_cause.SECURITY_HOTSPOT')
            : translate('coding_rules.description_section.title.root_cause'),
        content: (descriptionSectionsByKey[RuleDescriptionSections.DEFAULT] ||
          descriptionSectionsByKey[RuleDescriptionSections.ROOT_CAUSE]) && (
          <RuleDescription
            className="padded"
            sections={
              descriptionSectionsByKey[RuleDescriptionSections.DEFAULT] ||
              descriptionSectionsByKey[RuleDescriptionSections.ROOT_CAUSE]
            }
            isDefault={descriptionSectionsByKey[RuleDescriptionSections.DEFAULT] !== undefined}
            defaultContextKey={ruleDescriptionContextKey}
          />
        ),
      },
      {
        key: TabKeys.AssessTheIssue,
        label: translate('coding_rules.description_section.title', TabKeys.AssessTheIssue),
        content: descriptionSectionsByKey[RuleDescriptionSections.ASSESS_THE_PROBLEM] && (
          <RuleDescription
            className="padded"
            sections={descriptionSectionsByKey[RuleDescriptionSections.ASSESS_THE_PROBLEM]}
          />
        ),
      },
      {
        key: TabKeys.HowToFixIt,
        label: translate('coding_rules.description_section.title', TabKeys.HowToFixIt),
        content: descriptionSectionsByKey[RuleDescriptionSections.HOW_TO_FIX] && (
          <RuleDescription
            className="padded"
            sections={descriptionSectionsByKey[RuleDescriptionSections.HOW_TO_FIX]}
            defaultContextKey={ruleDescriptionContextKey}
          />
        ),
      },
      {
        key: TabKeys.MoreInfo,
        label: (
          <>
            {translate('coding_rules.description_section.title', TabKeys.MoreInfo)}
            {displayEducationalPrinciplesNotification && <div className="notice-dot" />}
          </>
        ),
        content: ((educationPrinciples && educationPrinciples.length > 0) ||
          descriptionSectionsByKey[RuleDescriptionSections.RESOURCES]) && (
          <MoreInfoRuleDescription
            educationPrinciples={educationPrinciples}
            sections={descriptionSectionsByKey[RuleDescriptionSections.RESOURCES]}
            displayEducationalPrinciplesNotification={displayEducationalPrinciplesNotification}
            educationPrinciplesRef={this.educationPrinciplesRef}
          />
        ),
      },
    ];

    if (codeTabContent !== undefined) {
      tabs.unshift({
        key: TabKeys.Code,
        label: translate('issue.tabs', TabKeys.Code),
        content: codeTabContent,
      });
    }

    return tabs.filter((tab) => tab.content);
  };

  attachScrollEvent = () => {
    document.addEventListener('scroll', this.checkIfEducationPrinciplesAreVisible, {
      capture: true,
    });
  };

  detachScrollEvent = () => {
    document.removeEventListener('scroll', this.checkIfEducationPrinciplesAreVisible, {
      capture: true,
    });
  };

  checkIfEducationPrinciplesAreVisible = () => {
    const {
      displayEducationalPrinciplesNotification,
      educationalPrinciplesNotificationHasBeenDismissed,
    } = this.state;

    if (this.educationPrinciplesRef.current) {
      const rect = this.educationPrinciplesRef.current.getBoundingClientRect();
      const isVisible = rect.top <= (window.innerHeight || document.documentElement.clientHeight);

      if (
        isVisible &&
        displayEducationalPrinciplesNotification &&
        !educationalPrinciplesNotificationHasBeenDismissed
      ) {
        dismissNotice(NoticeType.EDUCATION_PRINCIPLES)
          .then(() => {
            this.detachScrollEvent();
            this.setState({ educationalPrinciplesNotificationHasBeenDismissed: true });
          })
          .catch(() => {
            /* noop */
          });
      }
    }
  };

  handleSelectTabs = (currentTabKey: TabKeys) => {
    this.setState(({ tabs }) => ({
      selectedTab: tabs.find((tab) => tab.key === currentTabKey) || tabs[0],
    }));
  };

  render() {
    const { scrollInTab } = this.props;
    const { tabs, selectedTab } = this.state;

    if (!tabs || tabs.length === 0 || !selectedTab) {
      return null;
    }

    return (
      <>
        <div>
          <BoxedTabs
            className="big-spacer-top"
            onSelect={this.handleSelectTabs}
            selected={selectedTab.key}
            tabs={tabs}
          />
        </div>
        <ScreenPositionHelper>
          {({ top }) => (
            <div
              style={{
                // We substract the footer height with padding (80) and the main layout padding (20)
                maxHeight: scrollInTab ? `calc(100vh - ${top + 100}px)` : 'initial',
              }}
              className="bordered display-flex-column"
              role="tabpanel"
              aria-labelledby={getTabId(selectedTab.key)}
              id={getTabPanelId(selectedTab.key)}
            >
              {
                // Preserve tabs state by always rendering all of them. Only hide them when not selected
                tabs.map((tab) => (
                  <div
                    className={classNames('overflow-y-auto spacer', {
                      hidden: tab.key !== selectedTab.key,
                    })}
                    key={tab.key}
                  >
                    {tab.content}
                  </div>
                ))
              }
            </div>
          )}
        </ScreenPositionHelper>
      </>
    );
  }
}

export default withCurrentUserContext(withLocation(RuleTabViewer));
