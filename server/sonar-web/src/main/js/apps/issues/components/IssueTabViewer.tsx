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
import TabViewer, {
  getHowToFixTab,
  getMoreInfoTab,
  getWhyIsThisAnIssueTab,
  Tab,
  TabKeys
} from '../../../components/rules/TabViewer';
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

export default class IssueViewerTabs extends React.PureComponent<Props> {
  computeTabs = (showNotice: boolean, educationPrinciplesRef: React.RefObject<HTMLDivElement>) => {
    const {
      ruleDetails,
      codeTabContent,
      issue: { ruleDescriptionContextKey }
    } = this.props;
    const descriptionSectionsByKey = groupBy(
      ruleDetails.descriptionSections,
      section => section.key
    );
    const hasEducationPrinciples =
      !!ruleDetails.educationPrinciples && ruleDetails.educationPrinciples.length > 0;
    const showNotification = showNotice && hasEducationPrinciples;

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
        key: TabKeys.Code,
        label: translate('issue.tabs', TabKeys.Code),
        content: <div className="padded">{codeTabContent}</div>
      },
      getWhyIsThisAnIssueTab(
        rootCauseDescriptionSections,
        descriptionSectionsByKey,
        translate('issue.tabs', TabKeys.WhyIsThisAnIssue),
        ruleDescriptionContextKey
      ),
      getHowToFixTab(
        descriptionSectionsByKey,
        translate('issue.tabs', TabKeys.HowToFixIt),
        ruleDescriptionContextKey
      ),
      getMoreInfoTab(
        showNotification,
        descriptionSectionsByKey,
        educationPrinciplesRef,
        translate('issue.tabs', TabKeys.MoreInfo),
        ruleDetails.educationPrinciples
      )
    ].filter(tab => tab.content) as Array<Tab>;
  };

  render() {
    const { ruleDetails, codeTabContent } = this.props;
    const {
      component,
      ruleDetails: { name, key },
      issue: { message }
    } = this.props;
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
        </div>
        <TabViewer
          ruleDetails={ruleDetails}
          computeTabs={this.computeTabs}
          codeTabContent={codeTabContent}
          pageType="issues"
        />
      </>
    );
  }
}
