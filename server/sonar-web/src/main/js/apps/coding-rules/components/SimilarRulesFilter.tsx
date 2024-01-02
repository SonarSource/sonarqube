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
import { Button, ButtonPlain } from '../../../components/controls/buttons';
import Dropdown from '../../../components/controls/Dropdown';
import DropdownIcon from '../../../components/icons/DropdownIcon';
import FilterIcon from '../../../components/icons/FilterIcon';
import IssueTypeIcon from '../../../components/icons/IssueTypeIcon';
import TagsIcon from '../../../components/icons/TagsIcon';
import SeverityHelper from '../../../components/shared/SeverityHelper';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { Rule } from '../../../types/types';
import { Query } from '../query';

interface Props {
  onFilterChange: (changes: Partial<Query>) => void;
  rule: Rule;
}

export default class SimilarRulesFilter extends React.PureComponent<Props> {
  handleLanguageClick = () => {
    if (this.props.rule.lang) {
      this.props.onFilterChange({ languages: [this.props.rule.lang] });
    }
  };

  handleTypeClick = () => {
    this.props.onFilterChange({ types: [this.props.rule.type] });
  };

  handleSeverityClick = () => {
    if (this.props.rule.severity) {
      this.props.onFilterChange({ severities: [this.props.rule.severity] });
    }
  };

  handleTagClick = (tag: string) => {
    if (tag) {
      this.props.onFilterChange({ tags: [tag] });
    }
  };

  render() {
    const { rule } = this.props;
    const { tags = [], sysTags = [], severity } = rule;
    const allTags = [...tags, ...sysTags];

    return (
      <Dropdown
        className="display-inline-block"
        overlay={
          <>
            <h3 className="coding-rules-filter-title">
              {translate('coding_rules.filter_similar_rules')}
            </h3>
            <ul className="menu">
              {rule.langName && (
                <li>
                  <ButtonPlain
                    data-test="coding-rules__similar-language"
                    aria-label={translateWithParameters(
                      'coding_rules.filter_by_language',
                      rule.langName
                    )}
                    onClick={this.handleLanguageClick}
                  >
                    {rule.langName}
                  </ButtonPlain>
                </li>
              )}
              <li>
                <ButtonPlain
                  aria-label={translateWithParameters(
                    'coding_rules.filter_by_type',
                    translate('issue.type', rule.type)
                  )}
                  data-test="coding-rules__similar-type"
                  onClick={this.handleTypeClick}
                >
                  <IssueTypeIcon query={rule.type} />
                  <span className="little-spacer-left">{translate('issue.type', rule.type)}</span>
                </ButtonPlain>
              </li>

              {severity && (
                <li>
                  <ButtonPlain
                    data-test="coding-rules__similar-severity"
                    aria-label={translateWithParameters(
                      'coding_rules.filter_by_severity',
                      severity
                    )}
                    onClick={this.handleSeverityClick}
                  >
                    <SeverityHelper className="display-flex-center" severity={severity} />
                  </ButtonPlain>
                </li>
              )}

              {allTags.map((tag, index) => (
                <li
                  className={classNames('coding-rules-similar-tag', {
                    'coding-rules-similar-tag-divider': index === 0,
                  })}
                  key={tag}
                >
                  <ButtonPlain
                    data-tag={tag}
                    data-test="coding-rules__similar-tag"
                    aria-label={translateWithParameters('coding_rules.filter_by_tag', tag)}
                    onClick={() => this.handleTagClick(tag)}
                  >
                    <TagsIcon className="little-spacer-right text-middle" />
                    <span className="text-middle">{tag}</span>
                  </ButtonPlain>
                </li>
              ))}
            </ul>
          </>
        }
      >
        <Button
          className="js-rule-filter spacer-left"
          title={translate('coding_rules.filter_similar_rules')}
        >
          <FilterIcon />
          <DropdownIcon />
        </Button>
      </Dropdown>
    );
  }
}
