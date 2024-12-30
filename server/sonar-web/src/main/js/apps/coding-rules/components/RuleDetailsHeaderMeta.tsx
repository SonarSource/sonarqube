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

import { Badge, HelperHintIcon, Link, Note, SeparatorCircleIcon } from '~design-system';
import HelpTooltip from '~sonar-aligned/components/controls/HelpTooltip';
import Tooltip from '../../../components/controls/Tooltip';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getRuleUrl } from '../../../helpers/urls';
import { Dict, RuleDetails } from '../../../types/types';

const EXTERNAL_RULE_REPO_PREFIX = 'external_';

interface Props {
  referencedRepositories: Dict<{ key: string; language: string; name: string }>;
  ruleDetails: RuleDetails;
  organization: string;
}

export default function RuleDetailsHeaderMeta(props: Readonly<Props>) {
  const { referencedRepositories, ruleDetails } = props;
  const repository = referencedRepositories[ruleDetails.repo];
  const externalEngine = ruleDetails.repo.replace(new RegExp(`^${EXTERNAL_RULE_REPO_PREFIX}`), '');

  return (
    <Note className="sw-flex sw-flex-wrap sw-items-center sw-gap-2 sw-typo-sm" as="ul">
      {/* Template */}
      {!ruleDetails.isExternal && ruleDetails.isTemplate && (
        <>
          <li>
            <Tooltip content={translate('coding_rules.rule_template.title')}>
              <span className="it__coding-rules-detail-property">
                {translate('coding_rules.rule_template')}
              </span>
            </Tooltip>
          </li>
          <SeparatorCircleIcon aria-hidden as="li" />
        </>
      )}

      {/* Parent template */}
      {!ruleDetails.isExternal && ruleDetails.templateKey && (
        <>
          <li
            className="it__coding-rules-detail-property sw-flex sw-items-center sw-gap-1"
            data-meta="parent"
          >
            <span>
              {translate('coding_rules.custom_rule')}
              {' ('}
              <Link to={getRuleUrl(ruleDetails.templateKey, props.organization)}>
                {translate('coding_rules.show_template')}
              </Link>
              {') '}
            </span>
            <HelpTooltip overlay={translate('coding_rules.custom_rule.help')}>
              <HelperHintIcon />
            </HelpTooltip>
          </li>
          <SeparatorCircleIcon aria-hidden as="li" />
        </>
      )}

      {/* Key */}
      <li className="sw-flex sw-gap-1">
        <span>{translate('coding_rules.rule_id')}</span>
        <span className="sw-font-semibold">{ruleDetails.key}</span>
      </li>

      {/* Scope */}
      {ruleDetails.scope && (
        <>
          <SeparatorCircleIcon aria-hidden as="li" />
          <li className="sw-flex sw-gap-1">
            <span>{translate('coding_rules.analysis_scope')}</span>
            <span className="sw-font-semibold">
              {translate('coding_rules.scope', ruleDetails.scope)}
            </span>
          </li>
        </>
      )}

      {/* Repository */}
      {repository && (
        <>
          <SeparatorCircleIcon aria-hidden as="li" />
          <li className="sw-flex sw-gap-1">
            <span>{translate('coding_rules.repository')}</span>
            <span
              className="it__coding-rules-detail-property sw-font-semibold"
              data-meta="repository"
            >
              {repository.name} ({ruleDetails.langName})
            </span>
          </li>
        </>
      )}

      {/* Engine */}
      {ruleDetails.isExternal && ruleDetails.repo && externalEngine && (
        <>
          <SeparatorCircleIcon aria-hidden as="li" />
          <li>
            <Tooltip
              content={translateWithParameters(
                'coding_rules.external_rule.engine_tooltip',
                externalEngine,
              )}
            >
              <div className="sw-flex sw-gap-1">
                <span>{translate('coding_rules.external_rule.engine')}</span>
                <span
                  className="it__coding-rules-detail-property sw-font-semibold"
                  data-meta="engine"
                >
                  <Badge>{externalEngine}</Badge>
                </span>
              </div>
            </Tooltip>
          </li>
        </>
      )}

      {/* Status */}
      {!ruleDetails.isExternal && ruleDetails.status !== 'READY' && (
        <>
          <SeparatorCircleIcon aria-hidden as="li" />
          <li>
            <Tooltip content={translate('status')}>
              <Note data-meta="status">
                <Badge variant="deleted">{translate('rules.status', ruleDetails.status)}</Badge>
              </Note>
            </Tooltip>
          </li>
        </>
      )}

      {/* Effort */}
      {ruleDetails.remFnType && ruleDetails.remFnBaseEffort && (
        <>
          <SeparatorCircleIcon aria-hidden as="li" />
          <li className="sw-flex sw-gap-1">
            <span>{translate('coding_rules.remediation_effort')}</span>
            <span
              className="it__coding-rules-detail-property sw-font-semibold"
              data-meta="remediation-function"
            >
              {ruleDetails.remFnBaseEffort !== undefined && ` ${ruleDetails.remFnBaseEffort}`}
            </span>
          </li>
        </>
      )}
    </Note>
  );
}
