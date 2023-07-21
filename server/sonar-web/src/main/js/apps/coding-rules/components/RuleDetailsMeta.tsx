/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import Link from '../../../components/common/Link';
import { ButtonLink } from '../../../components/controls/buttons';
import Dropdown from '../../../components/controls/Dropdown';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import Tooltip from '../../../components/controls/Tooltip';
import IssueTypeIcon from '../../../components/icons/IssueTypeIcon';
import LinkIcon from '../../../components/icons/LinkIcon';
import DateFormatter from '../../../components/intl/DateFormatter';
import SeverityHelper from '../../../components/shared/SeverityHelper';
import TagsList from '../../../components/tags/TagsList';
import { PopupPlacement } from '../../../components/ui/popups';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getRuleUrl } from '../../../helpers/urls';
import { Dict, RuleDetails } from '../../../types/types';
import { Query } from '../query';
import RuleDetailsTagsPopup from './RuleDetailsTagsPopup';
import SimilarRulesFilter from './SimilarRulesFilter';

interface Props {
  canWrite: boolean | undefined;
  hideSimilarRulesFilter?: boolean;
  onFilterChange: (changes: Partial<Query>) => void;
  onTagsChange: (tags: string[]) => void;
  organization: string;
  referencedRepositories: Dict<{ key: string; language: string; name: string }>;
  ruleDetails: RuleDetails;
}

const EXTERNAL_RULE_REPO_PREFIX = 'external_';

export default class RuleDetailsMeta extends React.PureComponent<Props> {
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
          <span className="badge badge-error">{translate('rules.status', ruleDetails.status)}</span>
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
          <Dropdown
            closeOnClick={false}
            closeOnClickOutside={true}
            overlay={
              <RuleDetailsTagsPopup
                setTags={this.props.onTagsChange}
                sysTags={sysTags}
                tags={tags}
              />
            }
            overlayPlacement={PopupPlacement.BottomLeft}
          >
            <ButtonLink>
              <TagsList
                allowUpdate={canWrite}
                tags={allTags.length > 0 ? allTags : [translate('coding_rules.no_tags')]}
              />
            </ButtonLink>
          </Dropdown>
        ) : (
          <TagsList
            allowUpdate={canWrite}
            className="display-flex-center"
            tags={allTags.length > 0 ? allTags : [translate('coding_rules.no_tags')]}
          />
        )}
      </li>
    );
  };

  renderCreationDate = () => (
    <li className="coding-rules-detail-property" data-meta="available-since">
      <span className="little-spacer-right">{translate('coding_rules.available_since')}</span>
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
      <li className="coding-rules-detail-property">
        {translate('coding_rules.custom_rule')}
        {' ('}
        <Link to={getRuleUrl(ruleDetails.templateKey, ruleDetails.organization)}>
          {translate('coding_rules.show_template')}
        </Link>
        {')'}
        <HelpTooltip
          className="little-spacer-left"
          overlay={translate('coding_rules.custom_rule.help')}
        />
      </li>
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

  renderExternalBadge = () => {
    const { ruleDetails } = this.props;
    if (!ruleDetails.repo) {
      return null;
    }
    const engine = ruleDetails.repo.replace(new RegExp(`^${EXTERNAL_RULE_REPO_PREFIX}`), '');
    if (!engine) {
      return null;
    }
    return (
      <Tooltip overlay={translateWithParameters('coding_rules.external_rule.engine', engine)}>
        <li className="coding-rules-detail-property">
          <div className="badge spacer-left text-text-top">{engine}</div>
        </li>
      </Tooltip>
    );
  };

  renderKey() {
    const EXTERNAL_PREFIX = 'external_';
    const { ruleDetails } = this.props;
    const displayedKey = ruleDetails.key.startsWith(EXTERNAL_PREFIX)
      ? ruleDetails.key.substr(EXTERNAL_PREFIX.length)
      : ruleDetails.key;
    return <span className="note text-middle">{displayedKey}</span>;
  }

  render() {
    const { ruleDetails } = this.props;
    const { organization } = this.props;

    const hasTypeData = !ruleDetails.isExternal || ruleDetails.type !== 'UNKNOWN';
    return (
      <div className="js-rule-meta">
        <div className="page-header">
          <div className="pull-right">
            {this.renderKey()}
            {!ruleDetails.isExternal && (
              <Link
                className="coding-rules-detail-permalink link-no-underline spacer-left text-middle"
                title={translate('permalink')}
                to={getRuleUrl(ruleDetails.key, organization)}
              >
                <LinkIcon />
              </Link>
            )}
            {!this.props.hideSimilarRulesFilter && (
              <SimilarRulesFilter onFilterChange={this.props.onFilterChange} rule={ruleDetails} />
            )}
          </div>
          <h3 className="page-title coding-rules-detail-header">
            <big>{ruleDetails.name}</big>
          </h3>
        </div>

        {hasTypeData && (
          <ul className="coding-rules-detail-properties">
            {this.renderType()}
            {this.renderSeverity()}
            {!ruleDetails.isExternal && this.renderStatus()}
            {this.renderTags()}
            {!ruleDetails.isExternal && this.renderCreationDate()}
            {this.renderRepository()}
            {!ruleDetails.isExternal && (
              <>
                {this.renderTemplate()}
                {this.renderParentTemplate()}
                {this.renderRemediation()}
              </>
            )}
            {ruleDetails.isExternal && this.renderExternalBadge()}
          </ul>
        )}
      </div>
    );
  }
}
