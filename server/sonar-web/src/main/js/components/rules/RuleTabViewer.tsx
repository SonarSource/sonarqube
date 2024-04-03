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
import { ToggleButton, getTabId, getTabPanelId } from 'design-system';
import { cloneDeep, debounce, groupBy, isEqual } from 'lodash';
import * as React from 'react';
import { Location } from 'react-router-dom';
import { dismissNotice } from '../../api/users';
import { CurrentUserContextInterface } from '../../app/components/current-user/CurrentUserContext';
import withCurrentUserContext from '../../app/components/current-user/withCurrentUserContext';
import { RuleDescriptionSections } from '../../apps/coding-rules/rule';
import { translate } from '../../helpers/l10n';
import { RuleDetails } from '../../types/types';
import { NoticeType } from '../../types/users';
import withLocation from '../hoc/withLocation';
import MoreInfoRuleDescription from './MoreInfoRuleDescription';
import RuleDescription from './RuleDescription';

interface RuleTabViewerProps extends CurrentUserContextInterface {
  ruleDetails: RuleDetails;
  location: Location;
}

interface State {
  tabs: Tab[];
  selectedTab?: Tab;
  displayEducationalPrinciplesNotification?: boolean;
  educationalPrinciplesNotificationHasBeenDismissed?: boolean;
}

export interface Tab {
  value: TabKeys;
  label: string;
  content: React.ReactNode;
  counter?: number;
}

export enum TabKeys {
  Code = 'code',
  WhyIsThisAnIssue = 'why',
  HowToFixIt = 'how_to_fix',
  AssessTheIssue = 'assess_the_problem',
  Activity = 'activity',
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
      DEBOUNCE_FOR_SCROLL,
    );
  }

  componentDidMount() {
    this.setState((prevState) => this.computeState(prevState));
    this.attachScrollEvent();

    const tabs = this.computeTabs(Boolean(this.state.displayEducationalPrinciplesNotification));

    const query = new URLSearchParams(this.props.location.search);

    if (query.has('why')) {
      this.setState({
        selectedTab: tabs.find((tab) => tab.value === TabKeys.WhyIsThisAnIssue) ?? tabs[0],
      });
    }
  }

  componentDidUpdate(prevProps: RuleTabViewerProps, prevState: State) {
    const { ruleDetails, currentUser } = this.props;

    const { selectedTab } = this.state;

    if (
      !isEqual(prevProps.ruleDetails, ruleDetails) ||
      !isEqual(prevProps.currentUser, currentUser)
    ) {
      this.setState((pState) =>
        this.computeState(pState, prevProps.ruleDetails.key !== ruleDetails.key),
      );
    }

    if (selectedTab?.value === TabKeys.MoreInfo) {
      this.checkIfEducationPrinciplesAreVisible();
    }

    if (
      prevState.selectedTab?.value === TabKeys.MoreInfo &&
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
      ruleDetails: { descriptionSections, educationPrinciples, lang: ruleLanguage, type: ruleType },
    } = this.props;

    // As we might tamper with the description later on, we clone to avoid any side effect
    const descriptionSectionsByKey = cloneDeep(
      groupBy(descriptionSections, (section) => section.key),
    );

    const tabs: Tab[] = [
      {
        content: (descriptionSectionsByKey[RuleDescriptionSections.DEFAULT] ||
          descriptionSectionsByKey[RuleDescriptionSections.ROOT_CAUSE]) && (
          <RuleDescription
            language={ruleLanguage}
            sections={(
              descriptionSectionsByKey[RuleDescriptionSections.DEFAULT] ??
              descriptionSectionsByKey[RuleDescriptionSections.ROOT_CAUSE]
            ).concat(descriptionSectionsByKey[RuleDescriptionSections.INTRODUCTION] ?? [])}
          />
        ),
        value: TabKeys.WhyIsThisAnIssue,
        label:
          ruleType === 'SECURITY_HOTSPOT'
            ? translate('coding_rules.description_section.title.root_cause.SECURITY_HOTSPOT')
            : translate('coding_rules.description_section.title.root_cause'),
      },
      {
        content: descriptionSectionsByKey[RuleDescriptionSections.ASSESS_THE_PROBLEM] && (
          <RuleDescription
            language={ruleLanguage}
            sections={descriptionSectionsByKey[RuleDescriptionSections.ASSESS_THE_PROBLEM]}
          />
        ),
        value: TabKeys.AssessTheIssue,
        label: translate('coding_rules.description_section.title', TabKeys.AssessTheIssue),
      },
      {
        content: descriptionSectionsByKey[RuleDescriptionSections.HOW_TO_FIX] && (
          <RuleDescription
            language={ruleLanguage}
            sections={descriptionSectionsByKey[RuleDescriptionSections.HOW_TO_FIX]}
          />
        ),
        value: TabKeys.HowToFixIt,
        label: translate('coding_rules.description_section.title', TabKeys.HowToFixIt),
      },
      {
        content: ((educationPrinciples && educationPrinciples.length > 0) ||
          descriptionSectionsByKey[RuleDescriptionSections.RESOURCES]) && (
          <MoreInfoRuleDescription
            displayEducationalPrinciplesNotification={displayEducationalPrinciplesNotification}
            educationPrinciples={educationPrinciples}
            educationPrinciplesRef={this.educationPrinciplesRef}
            language={ruleLanguage}
            sections={descriptionSectionsByKey[RuleDescriptionSections.RESOURCES]}
          />
        ),
        value: TabKeys.MoreInfo,
        label: translate('coding_rules.description_section.title', TabKeys.MoreInfo),
        counter: displayEducationalPrinciplesNotification ? 1 : undefined,
      },
    ];

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
      selectedTab: tabs.find((tab) => tab.value === currentTabKey) ?? tabs[0],
    }));
  };

  render() {
    const { tabs, selectedTab } = this.state;

    if (!tabs || tabs.length === 0 || !selectedTab) {
      return null;
    }

    return (
      <>
        <div className="sw-mt-4">
          <ToggleButton
            role="tablist"
            onChange={this.handleSelectTabs}
            options={tabs}
            value={selectedTab.value}
          />
        </div>

        <div
          aria-labelledby={getTabId(selectedTab.value)}
          className="sw-flex sw-flex-col"
          id={getTabPanelId(selectedTab.value)}
          role="tabpanel"
        >
          {
            // Preserve tabs state by always rendering all of them. Only hide them when not selected
            tabs.map((tab) => (
              <div
                className={classNames({
                  'sw-hidden': tab.value !== selectedTab.value,
                })}
                key={tab.value}
              >
                {tab.content}
              </div>
            ))
          }
        </div>
      </>
    );
  }
}

export default withCurrentUserContext(withLocation(RuleTabViewer));
