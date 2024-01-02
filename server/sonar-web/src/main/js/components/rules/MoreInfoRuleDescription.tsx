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
import * as React from 'react';
import { RuleDescriptionSection } from '../../apps/coding-rules/rule';
import { translate } from '../../helpers/l10n';
import { Dict } from '../../types/types';
import { ButtonLink } from '../controls/buttons';
import { Alert } from '../ui/Alert';
import RuleDescription from './RuleDescription';
import DefenseInDepth from './educationPrinciples/DefenseInDepth';
import NeverTrustUserInput from './educationPrinciples/NeverTrustUserInput';
import './style.css';

interface Props {
  displayEducationalPrinciplesNotification?: boolean;
  educationPrinciples?: string[];
  educationPrinciplesRef?: React.RefObject<HTMLDivElement>;
  language?: string;
  sections?: RuleDescriptionSection[];
}

const EDUCATION_PRINCIPLES_MAP: Dict<React.ComponentType<React.PropsWithChildren>> = {
  defense_in_depth: DefenseInDepth,
  never_trust_user_input: NeverTrustUserInput,
};

export default class MoreInfoRuleDescription extends React.PureComponent<Props, {}> {
  handleNotificationScroll = () => {
    const element = this.props.educationPrinciplesRef?.current;

    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' });
    }
  };

  render() {
    const {
      displayEducationalPrinciplesNotification,
      language,
      sections = [],
      educationPrinciples = [],
      educationPrinciplesRef,
    } = this.props;

    return (
      <div className="sw-my-6 rule-desc">
        {displayEducationalPrinciplesNotification && (
          <Alert variant="info">
            <p className="little-spacer-bottom little-spacer-top">
              {translate('coding_rules.more_info.notification_message')}
            </p>

            <ButtonLink
              onClick={() => {
                this.handleNotificationScroll();
              }}
            >
              {translate('coding_rules.more_info.scroll_message')}
            </ButtonLink>
          </Alert>
        )}

        {sections.length > 0 && (
          <>
            <h2>{translate('coding_rules.more_info.resources.title')}</h2>
            <RuleDescription language={language} sections={sections} />
          </>
        )}

        {educationPrinciples.length > 0 && (
          <>
            <h2 ref={educationPrinciplesRef}>
              {translate('coding_rules.more_info.education_principles.title')}
            </h2>

            {educationPrinciples.map((key) => {
              const Concept = EDUCATION_PRINCIPLES_MAP[key];

              if (Concept === undefined) {
                return null;
              }

              return (
                <div key={key} className="education-principles big-spacer-top big-padded">
                  <Concept />
                </div>
              );
            })}
          </>
        )}
      </div>
    );
  }
}
