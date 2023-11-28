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
import styled from '@emotion/styled';
import {
  Badge,
  BasicSeparator,
  ClipboardIconButton,
  HelperHintIcon,
  IssueMessageHighlighting,
  LightLabel,
  Link,
  LinkIcon,
  Note,
  PageContentFontWrapper,
  TextSubdued,
  themeBorder,
} from 'design-system';
import * as React from 'react';
import DocumentationTooltip from '../../../components/common/DocumentationTooltip';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import Tooltip from '../../../components/controls/Tooltip';
import IssueSeverityIcon from '../../../components/icon-mappers/IssueSeverityIcon';
import IssueTypeIcon from '../../../components/icon-mappers/IssueTypeIcon';
import { CleanCodeAttributePill } from '../../../components/shared/CleanCodeAttributePill';
import SoftwareImpactPillList from '../../../components/shared/SoftwareImpactPillList';
import TagsList from '../../../components/tags/TagsList';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getPathUrlAsString, getRuleUrl } from '../../../helpers/urls';
import { IssueSeverity as IssueSeverityType } from '../../../types/issues';
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
      <Note className="it__coding-rules-detail-property sw-mr-4" data-meta="type">
        <DocumentationTooltip
          content={
            <>
              <p className="sw-mb-4">{translate('coding_rules.type.deprecation.title')}</p>
              <p>{translate('coding_rules.type.deprecation.filter_by')}</p>
            </>
          }
          links={[
            {
              href: '/user-guide/rules/overview',
              label: translate('learn_more'),
            },
          ]}
        >
          <TextSubdued className="sw-flex sw-items-center sw-gap-1">
            <IssueTypeIcon fill="iconTypeDisabled" type={ruleDetails.type} aria-hidden />
            {translate('issue.type', ruleDetails.type)}
          </TextSubdued>
        </DocumentationTooltip>
      </Note>
    );
  };

  renderSeverity = () => (
    <Note className="it__coding-rules-detail-property sw-mr-4" data-meta="severity">
      <DocumentationTooltip
        content={
          <>
            <p className="sw-mb-4">{translate('coding_rules.severity.deprecation.title')}</p>
            <p>{translate('coding_rules.severity.deprecation.filter_by')}</p>
          </>
        }
        links={[
          {
            href: '/user-guide/rules/overview',
            label: translate('learn_more'),
          },
        ]}
      >
        <TextSubdued className="sw-flex sw-items-center sw-gap-1">
          <IssueSeverityIcon
            fill="iconSeverityDisabled"
            severity={this.props.ruleDetails.severity as IssueSeverityType}
            aria-hidden
          />
          {translate('severity', this.props.ruleDetails.severity)}
        </TextSubdued>
      </DocumentationTooltip>
    </Note>
  );

  renderStatus = () => {
    const { ruleDetails } = this.props;
    if (ruleDetails.status === 'READY') {
      return null;
    }
    return (
      <Tooltip overlay={translate('status')}>
        <Note data-meta="status">
          <Badge variant="deleted">{translate('rules.status', ruleDetails.status)}</Badge>
        </Note>
      </Tooltip>
    );
  };

  renderTags = () => {
    const { canWrite, ruleDetails } = this.props;
    const { sysTags = [], tags = [] } = ruleDetails;
    const allTags = [...sysTags, ...tags];
    const TAGS_TO_DISPLAY = 1;

    return (
      <div className="it__coding-rules-detail-property" data-meta="tags">
        <TagsList
          allowUpdate={canWrite}
          tagsToDisplay={TAGS_TO_DISPLAY}
          tags={allTags.length > 0 ? allTags : [translate('coding_rules.no_tags')]}
          overlay={
            canWrite ? (
              <RuleDetailsTagsPopup
                setTags={this.props.onTagsChange}
                sysTags={sysTags}
                tags={tags}
              />
            ) : undefined
          }
        />
      </div>
    );
  };

  renderRepository = () => {
    const { referencedRepositories, ruleDetails } = this.props;
    const repository = referencedRepositories[ruleDetails.repo];
    if (!repository) {
      return null;
    }
    return (
      <Tooltip overlay={translate('coding_rules.repository_language')}>
        <Note className="it__coding-rules-detail-property sw-mr-4" data-meta="repository">
          {repository.name} ({ruleDetails.langName})
        </Note>
      </Tooltip>
    );
  };

  renderTemplate = () => {
    if (!this.props.ruleDetails.isTemplate) {
      return null;
    }
    return (
      <Tooltip overlay={translate('coding_rules.rule_template.title')}>
        <Note className="it__coding-rules-detail-property sw-mr-4">
          {translate('coding_rules.rule_template')}
        </Note>
      </Tooltip>
    );
  };

  renderParentTemplate = () => {
    const { ruleDetails } = this.props;
    if (!ruleDetails.templateKey) {
      return null;
    }
    return (
      <Note className="it__coding-rules-detail-property sw-mr-4">
        {translate('coding_rules.custom_rule')}
        {' ('}
        <Link to={getRuleUrl(ruleDetails.templateKey)}>
          {translate('coding_rules.show_template')}
        </Link>
        {') '}
        <HelpTooltip overlay={translate('coding_rules.custom_rule.help')}>
          <HelperHintIcon />
        </HelpTooltip>
      </Note>
    );
  };

  renderRemediation = () => {
    const { ruleDetails } = this.props;
    if (!ruleDetails.remFnType) {
      return null;
    }
    return (
      <>
        <BasicSeparator className="sw-my-2" />
        <RightMetaHeaderInfo
          title={translate('coding_rules.remediation_function', ruleDetails.remFnType)}
        >
          <Tooltip overlay={translate('coding_rules.remediation_function')}>
            <Note className="it__coding-rules-detail-property" data-meta="remediation-function">
              {ruleDetails.remFnBaseEffort !== undefined && ` ${ruleDetails.remFnBaseEffort}`}
              {ruleDetails.remFnGapMultiplier !== undefined &&
                ` +${ruleDetails.remFnGapMultiplier}`}
              {ruleDetails.gapDescription !== undefined && ` ${ruleDetails.gapDescription}`}
            </Note>
          </Tooltip>
        </RightMetaHeaderInfo>
      </>
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
        <Note className="it__coding-rules-detail-property sw-mr-4">
          <Badge>{engine}</Badge>
        </Note>
      </Tooltip>
    );
  };

  renderKey() {
    const EXTERNAL_PREFIX = 'external_';
    const { ruleDetails } = this.props;
    const displayedKey = ruleDetails.key.startsWith(EXTERNAL_PREFIX)
      ? ruleDetails.key.substring(EXTERNAL_PREFIX.length)
      : ruleDetails.key;
    return (
      <Note className="sw-overflow-hidden sw-text-ellipsis" title={displayedKey}>
        {displayedKey}
      </Note>
    );
  }

  render() {
    const { ruleDetails } = this.props;
    const ruleUrl = getRuleUrl(ruleDetails.key);

    const hasTypeData = !ruleDetails.isExternal || ruleDetails.type !== 'UNKNOWN';
    return (
      <header className="sw-flex sw-mb-6">
        <div className="sw-mr-8 sw-flex-1">
          <div className="sw-mb-2">
            {ruleDetails.cleanCodeAttributeCategory !== undefined && (
              <CleanCodeAttributePill
                cleanCodeAttributeCategory={ruleDetails.cleanCodeAttributeCategory}
                cleanCodeAttribute={ruleDetails.cleanCodeAttribute}
                type="rule"
              />
            )}
          </div>

          <div className="sw-mb-2">
            <PageContentFontWrapper className="sw-heading-md" as="h1">
              <IssueMessageHighlighting message={ruleDetails.name} />
              <ClipboardIconButton
                Icon={LinkIcon}
                aria-label={translate('permalink')}
                className="sw-ml-1 sw-align-bottom"
                copyValue={getPathUrlAsString(ruleUrl, ruleDetails.isExternal)}
                discreet
              />
            </PageContentFontWrapper>
          </div>

          <div className="sw-flex sw-items-center">
            {!!ruleDetails.impacts.length && (
              <div className="sw-flex sw-items-center sw-flex-1">
                <Note>{translate('issue.software_qualities.label')}</Note>
                <SoftwareImpactPillList
                  className="sw-ml-1"
                  softwareImpacts={ruleDetails.impacts}
                  type="rule"
                />
              </div>
            )}
          </div>
          <BasicSeparator className="sw-my-4" />
          {hasTypeData && (
            <div className="sw-flex sw-items-center">
              {!ruleDetails.isExternal && (
                <>
                  {this.renderTemplate()}
                  {this.renderParentTemplate()}
                </>
              )}

              {this.renderRepository()}
              {this.renderType()}
              {this.renderSeverity()}
              {ruleDetails.isExternal && this.renderExternalBadge()}
              {!ruleDetails.isExternal && this.renderStatus()}
            </div>
          )}
        </div>
        <StyledSection className="sw-flex sw-flex-col sw-pl-4 sw-min-w-abs-150 sw-max-w-abs-250">
          {this.renderKey()}
          <BasicSeparator className="sw-my-2" />
          <RightMetaHeaderInfo title={translate('issue.tags')}>
            {this.renderTags()}
          </RightMetaHeaderInfo>
          {this.renderRemediation()}
        </StyledSection>
      </header>
    );
  }
}

function RightMetaHeaderInfo({
  title,
  children,
}: Readonly<{ title: string; children: React.ReactNode }>) {
  return (
    <div>
      <LightLabel as="div" className="sw-body-sm-highlight">
        {title}
      </LightLabel>
      {children}
    </div>
  );
}

const StyledSection = styled.div`
  border-left: ${themeBorder('default', 'pageBlockBorder')};
`;
