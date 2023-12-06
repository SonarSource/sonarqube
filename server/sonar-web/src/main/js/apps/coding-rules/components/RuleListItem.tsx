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
  DangerButtonSecondary,
  InheritanceIcon,
  Link,
  OverridenIcon,
  SeparatorCircleIcon,
  TextSubdued,
  themeBorder,
} from 'design-system';
import * as React from 'react';
import { Profile, deactivateRule } from '../../../api/quality-profiles';
import DocumentationTooltip from '../../../components/common/DocumentationTooltip';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import Tooltip from '../../../components/controls/Tooltip';
import { CleanCodeAttributePill } from '../../../components/shared/CleanCodeAttributePill';
import SoftwareImpactPillList from '../../../components/shared/SoftwareImpactPillList';
import TypeHelper from '../../../components/shared/TypeHelper';
import TagsList from '../../../components/tags/TagsList';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getRuleUrl } from '../../../helpers/urls';
import { Rule } from '../../../types/types';
import { Activation } from '../query';
import ActivationButton from './ActivationButton';

interface Props {
  activation?: Activation;
  isLoggedIn: boolean;
  canDeactivateInherited?: boolean;
  onActivate: (profile: string, rule: string, activation: Activation) => void;
  onDeactivate: (profile: string, rule: string) => void;
  onOpen: (ruleKey: string) => void;
  rule: Rule;
  selected: boolean;
  selectRule: (key: string) => void;
  selectedProfile?: Profile;
}

export default class RuleListItem extends React.PureComponent<Props> {
  handleDeactivate = () => {
    if (this.props.selectedProfile) {
      const data = {
        key: this.props.selectedProfile.key,
        rule: this.props.rule.key,
      };
      deactivateRule(data).then(
        () => this.props.onDeactivate(data.key, data.rule),
        () => {},
      );
    }
  };

  handleActivate = (severity: string) => {
    if (this.props.selectedProfile) {
      this.props.onActivate(this.props.selectedProfile.key, this.props.rule.key, {
        severity,
        inherit: 'NONE',
      });
    }
    return Promise.resolve();
  };

  handleNameClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    // cmd(ctrl) + click should open a rule permalink in a new tab
    const isLeftClickEvent = event.button === 0;
    const isModifiedEvent = !!(event.metaKey || event.altKey || event.ctrlKey || event.shiftKey);
    if (isModifiedEvent || !isLeftClickEvent) {
      return;
    }

    event.preventDefault();
    event.stopPropagation();
    this.props.onOpen(this.props.rule.key);
  };

  renderActivation = () => {
    const { activation, selectedProfile } = this.props;
    if (!activation || !selectedProfile?.parentName) {
      return null;
    }

    if (!['OVERRIDES', 'INHERITED'].includes(activation.inherit)) {
      return null;
    }

    return (
      <div className="sw-mr-2 sw-shrink-0">
        {activation.inherit === 'OVERRIDES' && (
          <Tooltip
            overlay={translateWithParameters(
              'coding_rules.overrides',
              selectedProfile.name,
              selectedProfile.parentName,
            )}
          >
            <OverridenIcon className="sw-ml-1" />
          </Tooltip>
        )}
        {activation.inherit === 'INHERITED' && (
          <Tooltip
            overlay={translateWithParameters(
              'coding_rules.inherits',
              selectedProfile.name,
              selectedProfile.parentName,
            )}
          >
            <InheritanceIcon className="sw-ml-1" />
          </Tooltip>
        )}
      </div>
    );
  };

  renderActions = () => {
    const { activation, isLoggedIn, canDeactivateInherited, rule, selectedProfile } = this.props;

    if (!selectedProfile || !isLoggedIn) {
      return null;
    }

    const canCopy = selectedProfile.actions?.copy;
    if (selectedProfile.isBuiltIn && canCopy) {
      return (
        <div className="sw-ml-4">
          <Tooltip overlay={translate('coding_rules.need_extend_or_copy')}>
            <DangerButtonSecondary disabled>
              {translate('coding_rules', activation ? 'deactivate' : 'activate')}
            </DangerButtonSecondary>
          </Tooltip>
        </div>
      );
    }

    const canEdit = selectedProfile.actions?.edit;
    if (!canEdit) {
      return null;
    }

    if (activation) {
      return (
        <div className="sw-ml-4">
          {activation.inherit === 'NONE' || canDeactivateInherited ? (
            <ConfirmButton
              confirmButtonText={translate('yes')}
              modalBody={translate('coding_rules.deactivate.confirm')}
              modalHeader={translate('coding_rules.deactivate')}
              onConfirm={this.handleDeactivate}
            >
              {({ onClick }) => (
                <DangerButtonSecondary onClick={onClick}>
                  {translate('coding_rules.deactivate')}
                </DangerButtonSecondary>
              )}
            </ConfirmButton>
          ) : (
            <Tooltip overlay={translate('coding_rules.can_not_deactivate')}>
              <DangerButtonSecondary disabled>
                {translate('coding_rules.deactivate')}
              </DangerButtonSecondary>
            </Tooltip>
          )}
        </div>
      );
    }

    return (
      <div className="sw-ml-4">
        {!rule.isTemplate && (
          <ActivationButton
            buttonText={translate('coding_rules.activate')}
            modalHeader={translate('coding_rules.activate_in_quality_profile')}
            onDone={this.handleActivate}
            profiles={[selectedProfile]}
            rule={rule}
          />
        )}
      </div>
    );
  };

  render() {
    const { rule, selected } = this.props;
    const allTags = [...(rule.tags ?? []), ...(rule.sysTags ?? [])];
    return (
      <ListItemStyled
        selected={selected}
        className="it__coding-rule sw-p-3 sw-mb-4 sw-rounded-1 sw-bg-white"
        aria-current={selected}
        data-rule={rule.key}
        onClick={() => this.props.selectRule(rule.key)}
      >
        <div className="sw-flex sw-flex-col sw-gap-3">
          <div className="sw-flex sw-justify-between sw-items-center">
            <div className="sw-flex sw-items-center">
              {this.renderActivation()}

              <Link
                className="sw-body-sm-highlight"
                onClick={this.handleNameClick}
                to={getRuleUrl(rule.key)}
              >
                {rule.name}
              </Link>
            </div>

            <div>
              {rule.cleanCodeAttributeCategory !== undefined && (
                <CleanCodeAttributePill
                  cleanCodeAttributeCategory={rule.cleanCodeAttributeCategory}
                  type="rule"
                />
              )}
            </div>
          </div>

          <div className="sw-flex sw-items-center">
            <div className="sw-grow sw-flex sw-gap-2 sw-items-center sw-body-xs">
              {rule.impacts.length > 0 && (
                <SoftwareImpactPillList softwareImpacts={rule.impacts} type="rule" />
              )}
            </div>

            <ul className="sw-flex sw-gap-2 sw-items-center sw-body-xs">
              <li>{rule.langName}</li>

              <SeparatorCircleIcon aria-hidden as="li" />
              <li>
                <DocumentationTooltip
                  content={
                    <div>
                      <p className="sw-mb-2">{translate('coding_rules.type.deprecation.title')}</p>
                      <p>{translate('coding_rules.type.deprecation.filter_by')}</p>
                    </div>
                  }
                  links={[
                    {
                      href: '/user-guide/clean-code',
                      label: translate('learn_more'),
                    },
                  ]}
                >
                  <TextSubdued className="sw-whitespace-nowrap">
                    <TypeHelper
                      className="sw-flex sw-items-center"
                      iconFill="iconTypeDisabled"
                      type={rule.type}
                    />
                  </TextSubdued>
                </DocumentationTooltip>
              </li>

              {rule.isTemplate && (
                <>
                  <SeparatorCircleIcon aria-hidden as="li" />
                  <li>
                    <Tooltip overlay={translate('coding_rules.rule_template.title')}>
                      <span>
                        <Badge>{translate('coding_rules.rule_template')}</Badge>
                      </span>
                    </Tooltip>
                  </li>
                </>
              )}

              {rule.status !== 'READY' && (
                <>
                  <SeparatorCircleIcon aria-hidden as="li" />
                  <li>
                    <Badge variant="deleted">{translate('rules.status', rule.status)}</Badge>
                  </li>
                </>
              )}

              {allTags.length > 0 && (
                <>
                  <SeparatorCircleIcon aria-hidden as="li" />
                  <li>
                    <TagsList
                      allowUpdate={false}
                      className="sw-body-xs"
                      tagsClassName="sw-body-xs"
                      tags={allTags}
                    />
                  </li>
                </>
              )}
            </ul>

            <div className="sw-flex sw-items-center">{this.renderActions()}</div>
          </div>
        </div>
      </ListItemStyled>
    );
  }
}

const ListItemStyled = styled.li<{ selected: boolean }>`
  outline: ${(props) =>
    props.selected ? themeBorder('heavy', 'primary') : themeBorder('default', 'almCardBorder')};
  outline-offset: ${(props) => (props.selected ? '-2px' : '-1px')};
`;
