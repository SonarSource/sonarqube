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
import { colors } from '../../../app/theme';
import DocumentationTooltip from '../../../components/common/DocumentationTooltip';
import Link from '../../../components/common/Link';
import Dropdown from '../../../components/controls/Dropdown';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import Tooltip from '../../../components/controls/Tooltip';
import { ButtonLink } from '../../../components/controls/buttons';
import IssueTypeIcon from '../../../components/icons/IssueTypeIcon';
import LinkIcon from '../../../components/icons/LinkIcon';
import DateFormatter from '../../../components/intl/DateFormatter';
import { CleanCodeAttributePill } from '../../../components/shared/CleanCodeAttributePill';
import SeverityHelper from '../../../components/shared/SeverityHelper';
import SoftwareImpactPill from '../../../components/shared/SoftwareImpactPill';
import TagsList from '../../../components/tags/TagsList';
import { PopupPlacement } from '../../../components/ui/popups';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getRuleUrl } from '../../../helpers/urls';
import { Dict, RuleDetails } from '../../../types/types';
import RuleDetailsTagsPopup from './RuleDetailsTagsPopup';

interface Props {
  canWrite: boolean | undefined;
  onTagsChange: (tags: string[]) => void;
  referencedRepositories: Dict<{ key: string; language: string; name: string }>;
  ruleDetails: RuleDetails;
}

const EXTERNAL_RULE_REPO_PREFIX = 'external_';

export default class RuleDetailsMeta extends React.PureComponent<Props> {
  renderType = () => {
    const { ruleDetails } = this.props;
    return (
      <li className="coding-rules-detail-property muted" data-meta="type">
        <DocumentationTooltip
          content={
            <>
              <p className="sw-mb-4">{translate('coding_rules.type.deprecation.title')}</p>
              <p>{translate('coding_rules.type.deprecation.filter_by')}</p>
            </>
          }
          links={[
            {
              href: '/user-guide/rules',
              label: translate('learn_more'),
            },
          ]}
        >
          <IssueTypeIcon className="little-spacer-right" query={ruleDetails.type} />
          {translate('issue.type', ruleDetails.type)}
        </DocumentationTooltip>
      </li>
    );
  };

  renderSeverity = () => (
    <li className="coding-rules-detail-property muted" data-meta="severity">
      <DocumentationTooltip
        content={
          <>
            <p className="sw-mb-4">{translate('coding_rules.severity.deprecation.title')}</p>
            <p>{translate('coding_rules.severity.deprecation.filter_by')}</p>
          </>
        }
        links={[
          {
            href: '/user-guide/rules',
            label: translate('learn_more'),
          },
        ]}
      >
        <SeverityHelper
          fill={colors.neutral200}
          className="display-inline-flex-center"
          severity={this.props.ruleDetails.severity}
        />
      </DocumentationTooltip>
    </li>
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
      <div className="coding-rules-detail-property null-spacer-bottom" data-meta="tags">
        {this.props.canWrite ? (
          <Dropdown
            closeOnClick={false}
            closeOnClickOutside
            overlay={
              <RuleDetailsTagsPopup
                setTags={this.props.onTagsChange}
                sysTags={sysTags}
                tags={tags}
              />
            }
            overlayPlacement={PopupPlacement.BottomRight}
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
      </div>
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
        <Link to={getRuleUrl(ruleDetails.templateKey)}>
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
    if (!ruleDetails.remFnType) {
      return null;
    }
    return (
      <Tooltip overlay={translate('coding_rules.remediation_function')}>
        <li className="coding-rules-detail-property" data-meta="remediation-function">
          {translate('coding_rules.remediation_function', ruleDetails.remFnType)}
          {':'}
          {ruleDetails.remFnBaseEffort !== undefined && ` ${ruleDetails.remFnBaseEffort}`}
          {ruleDetails.remFnGapMultiplier !== undefined && ` +${ruleDetails.remFnGapMultiplier}`}
          {ruleDetails.gapDescription !== undefined && ` ${ruleDetails.gapDescription}`}
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
      ? ruleDetails.key.substring(EXTERNAL_PREFIX.length)
      : ruleDetails.key;
    return <span className="note text-middle">{displayedKey}</span>;
  }

  render() {
    const { ruleDetails } = this.props;
    const hasTypeData = !ruleDetails.isExternal || ruleDetails.type !== 'UNKNOWN';
    return (
      <div className="js-rule-meta">
        <div className="display-flex-space-between spacer-bottom">
          {ruleDetails.cleanCodeAttributeCategory !== undefined && (
            <CleanCodeAttributePill
              cleanCodeAttributeCategory={ruleDetails.cleanCodeAttributeCategory}
              cleanCodeAttribute={ruleDetails.cleanCodeAttribute}
              type="rule"
            />
          )}
          <div className="pull-right display-flex-center spacer-right">
            {this.renderKey()}
            {!ruleDetails.isExternal && (
              <Link
                className="coding-rules-detail-permalink link-no-underline spacer-left text-middle"
                title={translate('permalink')}
                to={getRuleUrl(ruleDetails.key)}
              >
                <LinkIcon />
              </Link>
            )}
          </div>
        </div>

        <div className="display-flex-space-between big-spacer-bottom">
          <h1 className="page-title coding-rules-detail-header">{ruleDetails.name}</h1>
          {this.renderTags()}
        </div>

        <div className="display-flex-center">
          {!!ruleDetails.impacts.length && (
            <div className="sw-flex sw-items-center flex-1">
              <span>{translate('issue.software_qualities.label')}</span>
              <ul className="sw-flex sw-gap-2">
                {ruleDetails.impacts.map(({ severity, softwareQuality }) => (
                  <li key={softwareQuality}>
                    <SoftwareImpactPill
                      className="little-spacer-left"
                      severity={severity}
                      quality={softwareQuality}
                      type="rule"
                    />
                  </li>
                ))}
              </ul>
            </div>
          )}

          {hasTypeData && (
            <ul className="coding-rules-detail-properties">
              {this.renderType()}
              {this.renderSeverity()}
              {!ruleDetails.isExternal && this.renderStatus()}
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
      </div>
    );
  }
}
