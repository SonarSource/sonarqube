/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { Link } from 'react-router';
import RuleDetailsTagsPopup from './RuleDetailsTagsPopup';
import SimilarRulesFilter from './SimilarRulesFilter';
import { Query } from '../query';
import { RuleDetails, RuleScope } from '../../../app/types';
import { getRuleUrl } from '../../../helpers/urls';
import LinkIcon from '../../../components/icons-components/LinkIcon';
import RuleScopeIcon from '../../../components/icons-components/RuleScopeIcon';
import Tooltip from '../../../components/controls/Tooltip';
import { translate } from '../../../helpers/l10n';
import IssueTypeIcon from '../../../components/ui/IssueTypeIcon';
import SeverityHelper from '../../../components/shared/SeverityHelper';
import BubblePopupHelper from '../../../components/common/BubblePopupHelper';
import TagsList from '../../../components/tags/TagsList';
import DateFormatter from '../../../components/intl/DateFormatter';

interface Props {
  canWrite: boolean | undefined;
  onFilterChange: (changes: Partial<Query>) => void;
  onTagsChange: (tags: string[]) => void;
  organization: string | undefined;
  referencedRepositories: { [repository: string]: { key: string; language: string; name: string } };
  ruleDetails: RuleDetails;
}

interface State {
  tagsPopup: boolean;
}

export default class RuleDetailsMeta extends React.PureComponent<Props, State> {
  state: State = { tagsPopup: false };

  handleTagsClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.currentTarget.blur();
    this.setState(state => ({ tagsPopup: !state.tagsPopup }));
  };

  handleTagsPopupToggle = (show: boolean) => this.setState({ tagsPopup: show });

  renderType = () => {
    const { ruleDetails } = this.props;
    return (
      <Tooltip overlay={translate('coding_rules.type.tooltip', ruleDetails.type)}>
        <li className="coding-rules-detail-property" data-meta="type">
          <IssueTypeIcon className="little-spacer-right" query={ruleDetails.type} />
          {translate('issue.type', ruleDetails.type)}
        </li>
      </Tooltip>
    );
  };

  renderSeverity = () => (
    <Tooltip overlay={translate('default_severity')}>
      <li className="coding-rules-detail-property" data-meta="severity">
        <SeverityHelper
          className="display-inline-flex-center"
          severity={this.props.ruleDetails.severity}
        />
      </li>
    </Tooltip>
  );

  renderStatus = () => {
    const { ruleDetails } = this.props;
    if (ruleDetails.status === 'READY') {
      return null;
    }
    return (
      <Tooltip overlay={translate('status')}>
        <li className="coding-rules-detail-property" data-meta="status">
          <span className="badge badge-normal-size badge-danger-light">
            {translate('rules.status', ruleDetails.status)}
          </span>
        </li>
      </Tooltip>
    );
  };

  renderTags = () => {
    const { canWrite, ruleDetails } = this.props;
    const { sysTags = [], tags = [] } = ruleDetails;
    const allTags = [...sysTags, ...tags];
    return (
      <li className="coding-rules-detail-property" data-meta="tags">
        {this.props.canWrite ? (
          <BubblePopupHelper
            isOpen={this.state.tagsPopup}
            position="bottomleft"
            popup={
              <RuleDetailsTagsPopup
                organization={this.props.organization}
                setTags={this.props.onTagsChange}
                sysTags={sysTags}
                tags={tags}
              />
            }
            togglePopup={this.handleTagsPopupToggle}>
            <button className="button-link" onClick={this.handleTagsClick}>
              <TagsList
                allowUpdate={canWrite}
                tags={allTags.length > 0 ? allTags : [translate('coding_rules.no_tags')]}
              />
            </button>
          </BubblePopupHelper>
        ) : (
          <TagsList
            allowUpdate={canWrite}
            tags={allTags.length > 0 ? allTags : [translate('coding_rules.no_tags')]}
          />
        )}
      </li>
    );
  };

  renderCreationDate = () => (
    <li className="coding-rules-detail-property" data-meta="available-since">
      {translate('coding_rules.available_since')}{' '}
      <DateFormatter date={this.props.ruleDetails.createdAt} />
    </li>
  );

  renderRepository = () => {
    const { referencedRepositories, ruleDetails } = this.props;
    const repository = referencedRepositories[ruleDetails.repo];
    if (!repository) {
      return null;
    }
    return (
      <Tooltip overlay={translate('coding_rules.repository_language')}>
        <li className="coding-rules-detail-property" data-meta="repository">
          {repository.name} ({ruleDetails.langName})
        </li>
      </Tooltip>
    );
  };

  renderTemplate = () => {
    if (!this.props.ruleDetails.isTemplate) {
      return null;
    }
    return (
      <Tooltip overlay={translate('coding_rules.rule_template.title')}>
        <li className="coding-rules-detail-property">{translate('coding_rules.rule_template')}</li>
      </Tooltip>
    );
  };

  renderParentTemplate = () => {
    const { ruleDetails } = this.props;
    if (!ruleDetails.templateKey) {
      return null;
    }
    return (
      <Tooltip overlay={translate('coding_rules.custom_rule.title')}>
        <li className="coding-rules-detail-property">
          {translate('coding_rules.custom_rule')}
          {' ('}
          <Link to={getRuleUrl(ruleDetails.templateKey, this.props.organization)}>
            {translate('coding_rules.show_template')}
          </Link>
          {')'}
        </li>
      </Tooltip>
    );
  };

  renderRemediation = () => {
    const { ruleDetails } = this.props;
    if (!ruleDetails.debtRemFnType) {
      return null;
    }
    return (
      <Tooltip overlay={translate('coding_rules.remediation_function')}>
        <li className="coding-rules-detail-property" data-meta="remediation-function">
          {translate('coding_rules.remediation_function', ruleDetails.debtRemFnType)}
          {':'}
          {ruleDetails.debtRemFnOffset !== undefined && ` ${ruleDetails.debtRemFnOffset}`}
          {ruleDetails.debtRemFnCoeff !== undefined && ` +${ruleDetails.debtRemFnCoeff}`}
          {ruleDetails.effortToFixDescription !== undefined &&
            ` ${ruleDetails.effortToFixDescription}`}
        </li>
      </Tooltip>
    );
  };

  renderScope = () => {
    const scope = this.props.ruleDetails.scope || RuleScope.Main;
    return (
      <Tooltip overlay={translate('coding_rules.scope.title')}>
        <li className="coding-rules-detail-property">
          <RuleScopeIcon className="little-spacer-right" />
          {translate('coding_rules.scope', scope)}
        </li>
      </Tooltip>
    );
  };

  render() {
    const { ruleDetails } = this.props;
    return (
      <div className="js-rule-meta">
        <header className="page-header">
          <div className="pull-right">
            <span className="note text-middle">{ruleDetails.key}</span>
            <Link
              className="coding-rules-detail-permalink link-no-underline spacer-left text-middle"
              to={getRuleUrl(ruleDetails.key, this.props.organization)}>
              <LinkIcon />
            </Link>
            <SimilarRulesFilter onFilterChange={this.props.onFilterChange} rule={ruleDetails} />
          </div>
          <h3 className="page-title coding-rules-detail-header">
            <big>{ruleDetails.name}</big>
          </h3>
        </header>

        <ul className="coding-rules-detail-properties">
          {this.renderType()}
          {this.renderSeverity()}
          {this.renderStatus()}
          {this.renderScope()}
          {this.renderTags()}
          {this.renderCreationDate()}
          {this.renderRepository()}
          {this.renderTemplate()}
          {this.renderParentTemplate()}
          {this.renderRemediation()}
        </ul>
      </div>
    );
  }
}
