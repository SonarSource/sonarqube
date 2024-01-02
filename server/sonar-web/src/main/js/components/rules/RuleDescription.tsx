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
import * as React from 'react';
import { RuleDescriptionSection } from '../../apps/coding-rules/rule';
import applyCodeDifferences from '../../helpers/code-difference';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { sanitizeString } from '../../helpers/sanitize';
import ButtonToggle from '../controls/ButtonToggle';
import { Alert } from '../ui/Alert';
import OtherContextOption from './OtherContextOption';

const OTHERS_KEY = 'others';

interface Props {
  isDefault?: boolean;
  sections: RuleDescriptionSection[];
  defaultContextKey?: string;
  className?: string;
}

interface State {
  contexts: RuleDescriptionContextDisplay[];
  defaultContext?: RuleDescriptionContextDisplay;
  selectedContext?: RuleDescriptionContextDisplay;
}

interface RuleDescriptionContextDisplay {
  displayName: string;
  content: string;
  key: string;
}

export default class RuleDescription extends React.PureComponent<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = this.computeState();
  }

  componentDidUpdate(prevProps: Props) {
    const { sections, defaultContextKey } = this.props;

    if (prevProps.sections !== sections || prevProps.defaultContextKey !== defaultContextKey) {
      this.setState(this.computeState());
    }
  }

  computeState = () => {
    const { sections, defaultContextKey } = this.props;

    const contexts = sections
      .filter(
        (
          section
        ): section is RuleDescriptionSection & Required<Pick<RuleDescriptionSection, 'context'>> =>
          section.context != null
      )
      .map((section) => ({
        displayName: section.context.displayName || section.context.key,
        content: section.content,
        key: section.context.key,
      }))
      .sort((a, b) => a.displayName.localeCompare(b.displayName));

    if (contexts.length > 0) {
      contexts.push({
        displayName: translate('coding_rules.description_context.other'),
        content: '',
        key: OTHERS_KEY,
      });
    }

    let defaultContext: RuleDescriptionContextDisplay | undefined;

    if (defaultContextKey) {
      defaultContext = contexts.find((context) => context.key === defaultContextKey);
    }

    return {
      contexts,
      defaultContext,
      selectedContext: defaultContext ?? contexts[0],
    };
  };

  handleToggleContext = (value: string) => {
    const { contexts } = this.state;

    const selected = contexts.find((ctxt) => ctxt.displayName === value);
    if (selected) {
      this.setState({ selectedContext: selected });
    }
  };

  render() {
    const { className, sections, isDefault } = this.props;
    const { contexts, defaultContext, selectedContext } = this.state;

    const options = contexts.map((ctxt) => ({
      label: ctxt.displayName,
      value: ctxt.displayName,
    }));

    if (contexts.length > 0 && selectedContext) {
      return (
        <div
          className={classNames(className, {
            markdown: isDefault,
            'rule-desc': !isDefault,
          })}
          ref={(node) => {
            applyCodeDifferences(node);
          }}
        >
          <div className="rules-context-description">
            <h2 className="rule-contexts-title">
              {translate('coding_rules.description_context.title')}
            </h2>
            {defaultContext && (
              <Alert variant="info" display="inline" className="big-spacer-bottom">
                {translateWithParameters(
                  'coding_rules.description_context.default_information',
                  defaultContext.displayName
                )}
              </Alert>
            )}
            <div className="big-spacer-bottom">
              <ButtonToggle
                label={translate('coding_rules.description_context.title')}
                onCheck={this.handleToggleContext}
                options={options}
                value={selectedContext.displayName}
              />
              {selectedContext.key !== OTHERS_KEY && (
                <h2>
                  {translateWithParameters(
                    'coding_rules.description_context.sub_title',
                    selectedContext.displayName
                  )}
                </h2>
              )}
            </div>
            {selectedContext.key === OTHERS_KEY ? (
              <OtherContextOption />
            ) : (
              <div
                /* eslint-disable-next-line react/no-danger */
                dangerouslySetInnerHTML={{ __html: sanitizeString(selectedContext.content) }}
              />
            )}
          </div>
        </div>
      );
    }

    return (
      <div
        className={classNames(className, {
          markdown: isDefault,
          'rule-desc': !isDefault,
        })}
        ref={(node) => {
          applyCodeDifferences(node);
        }}
        // eslint-disable-next-line react/no-danger
        dangerouslySetInnerHTML={{
          __html: sanitizeString(sections[0].content),
        }}
      />
    );
  }
}
