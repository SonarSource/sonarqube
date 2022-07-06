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
import { RuleDescriptionSection } from '../../apps/coding-rules/rule';
import { translate } from '../../helpers/l10n';
import { sanitizeString } from '../../helpers/sanitize';
import RadioToggle from '../controls/RadioToggle';
import OtherContextOption from './OtherContextOption';

const OTHERS_KEY = 'others';

interface Props {
  isDefault?: boolean;
  description: RuleDescriptionSection[];
}

interface State {
  contexts: RuleDescriptionContextDisplay[];
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
    this.state = this.computeState(props.description);
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.description !== this.props.description) {
      this.setState(this.computeState(this.props.description));
    }
  }

  computeState = (descriptions: RuleDescriptionSection[]) => {
    const contexts = descriptions
      .map(sec => ({
        displayName: sec.context?.displayName || '',
        content: sec.content,
        key: sec.key.toString()
      }))
      .filter(sec => sec.displayName !== '')
      .sort((a, b) => a.displayName.localeCompare(b.displayName));

    if (contexts.length > 0) {
      contexts.push({
        displayName: translate('coding_rules.description_context_other'),
        content: '',
        key: OTHERS_KEY
      });
    }

    return {
      contexts,
      selectedContext: contexts[0]
    };
  };

  handleToggleContext = (value: string) => {
    const { contexts } = this.state;

    const selected = contexts.find(ctxt => ctxt.displayName === value);
    if (selected) {
      this.setState({ selectedContext: selected });
    }
  };

  render() {
    const { description, isDefault } = this.props;
    const { contexts } = this.state;
    const { selectedContext } = this.state;

    const options = contexts.map(ctxt => ({
      label: ctxt.displayName,
      value: ctxt.displayName
    }));

    if (!description[0].context && description.length === 1) {
      return (
        <div
          className={classNames('big-padded', {
            markdown: isDefault,
            'rule-desc': !isDefault
          })}
          // eslint-disable-next-line react/no-danger
          dangerouslySetInnerHTML={{
            __html: sanitizeString(description[0].content)
          }}
        />
      );
    }
    if (!selectedContext) {
      return null;
    }
    return (
      <div
        className={classNames('big-padded', {
          markdown: isDefault,
          'rule-desc': !isDefault
        })}>
        <div className="rules-context-description">
          <h2 className="rule-contexts-title">
            {translate('coding_rules.description_context_title')}
          </h2>
          <RadioToggle
            className="big-spacer-bottom"
            name="filter"
            onCheck={this.handleToggleContext}
            options={options}
            value={selectedContext.displayName}
          />
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
}
