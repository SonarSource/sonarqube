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
import RuleDescription from '../../../components/rules/RuleDescription';
import TabViewer, {
  getHowToFixTab,
  getMoreInfoTab,
  getWhyIsThisAnIssueTab,
  Tab,
  TabKeys
} from '../../../components/rules/TabViewer';
import { translate } from '../../../helpers/l10n';
import { sanitizeString } from '../../../helpers/sanitize';
import { RuleDetails } from '../../../types/types';
import { RuleDescriptionSections } from '../rule';

interface Props {
  ruleDetails: RuleDetails;
}

export default class RuleViewerTabs extends React.PureComponent<Props> {
  computeTabs = (showNotice: boolean, educationPrinciplesRef: React.RefObject<HTMLDivElement>) => {
    const { ruleDetails } = this.props;
    const descriptionSectionsByKey = groupBy(
      ruleDetails.descriptionSections,
      section => section.key
    );
    const hasEducationPrinciples =
      !!ruleDetails.educationPrinciples && ruleDetails.educationPrinciples.length > 0;
    const showNotification = showNotice && hasEducationPrinciples;

    const rootCauseTitle =
      ruleDetails.type === 'SECURITY_HOTSPOT'
        ? translate('coding_rules.description_section.title.root_cause.SECURITY_HOTSPOT')
        : translate('coding_rules.description_section.title.root_cause');

    return [
      getWhyIsThisAnIssueTab(
        descriptionSectionsByKey[RuleDescriptionSections.ROOT_CAUSE],
        descriptionSectionsByKey,
        rootCauseTitle
      ),
      {
        key: TabKeys.AssessTheIssue,
        label: translate('coding_rules.description_section.title', TabKeys.AssessTheIssue),
        content: descriptionSectionsByKey[RuleDescriptionSections.ASSESS_THE_PROBLEM] && (
          <RuleDescription
            sections={descriptionSectionsByKey[RuleDescriptionSections.ASSESS_THE_PROBLEM]}
          />
        )
      },
      getHowToFixTab(
        descriptionSectionsByKey,
        translate('coding_rules.description_section.title', TabKeys.HowToFixIt)
      ),
      getMoreInfoTab(
        showNotification,
        descriptionSectionsByKey,
        educationPrinciplesRef,
        translate('coding_rules.description_section.title', TabKeys.MoreInfo),
        ruleDetails.educationPrinciples
      )
    ].filter(tab => tab.content) as Array<Tab>;
  };

  render() {
    const { ruleDetails } = this.props;
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
        <TabViewer ruleDetails={this.props.ruleDetails} computeTabs={this.computeTabs} />
      </>
    );
  }
}
