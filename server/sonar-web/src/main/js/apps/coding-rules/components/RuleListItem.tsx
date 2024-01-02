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
import { deactivateRule, Profile } from '../../../api/quality-profiles';
import Link from '../../../components/common/Link';
import { Button } from '../../../components/controls/buttons';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import Tooltip from '../../../components/controls/Tooltip';
import IssueTypeIcon from '../../../components/icons/IssueTypeIcon';
import SeverityIcon from '../../../components/icons/SeverityIcon';
import TagsList from '../../../components/tags/TagsList';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getRuleUrl } from '../../../helpers/urls';
import { Rule } from '../../../types/types';
import { Activation, Query } from '../query';
import ActivationButton from './ActivationButton';
import RuleInheritanceIcon from './RuleInheritanceIcon';
import SimilarRulesFilter from './SimilarRulesFilter';

interface Props {
  activation?: Activation;
  isLoggedIn: boolean;
  onActivate: (profile: string, rule: string, activation: Activation) => void;
  onDeactivate: (profile: string, rule: string) => void;
  onFilterChange: (changes: Partial<Query>) => void;
  onOpen: (ruleKey: string) => void;
  rule: Rule;
  selected: boolean;
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
        () => {}
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
    this.props.onOpen(this.props.rule.key);
  };

  renderActivation = () => {
    const { activation, selectedProfile } = this.props;
    if (!activation) {
      return null;
    }

    return (
      <td className="coding-rule-table-meta-cell coding-rule-activation">
        <SeverityIcon severity={activation.severity} />
        {selectedProfile && selectedProfile.parentName && (
          <>
            {activation.inherit === 'OVERRIDES' && (
              <Tooltip
                overlay={translateWithParameters(
                  'coding_rules.overrides',
                  selectedProfile.name,
                  selectedProfile.parentName
                )}
              >
                <RuleInheritanceIcon
                  className="little-spacer-left"
                  inheritance={activation.inherit}
                />
              </Tooltip>
            )}
            {activation.inherit === 'INHERITED' && (
              <Tooltip
                overlay={translateWithParameters(
                  'coding_rules.inherits',
                  selectedProfile.name,
                  selectedProfile.parentName
                )}
              >
                <RuleInheritanceIcon
                  className="little-spacer-left"
                  inheritance={activation.inherit}
                />
              </Tooltip>
            )}
          </>
        )}
      </td>
    );
  };

  renderActions = () => {
    const { activation, isLoggedIn, rule, selectedProfile } = this.props;
    if (!selectedProfile || !isLoggedIn) {
      return null;
    }

    const canCopy = selectedProfile.actions && selectedProfile.actions.copy;
    if (selectedProfile.isBuiltIn && canCopy) {
      return (
        <td className="coding-rule-table-meta-cell coding-rule-activation-actions">
          <Tooltip overlay={translate('coding_rules.need_extend_or_copy')}>
            <Button className="coding-rules-detail-quality-profile-deactivate button-red disabled">
              {translate('coding_rules', activation ? 'deactivate' : 'activate')}
            </Button>
          </Tooltip>
        </td>
      );
    }

    const canEdit = selectedProfile.actions && selectedProfile.actions.edit;
    if (!canEdit) {
      return null;
    }

    if (activation) {
      return (
        <td className="coding-rule-table-meta-cell coding-rule-activation-actions">
          {activation.inherit === 'NONE' ? (
            <ConfirmButton
              confirmButtonText={translate('yes')}
              modalBody={translate('coding_rules.deactivate.confirm')}
              modalHeader={translate('coding_rules.deactivate')}
              onConfirm={this.handleDeactivate}
            >
              {({ onClick }) => (
                <Button
                  className="coding-rules-detail-quality-profile-deactivate button-red"
                  onClick={onClick}
                >
                  {translate('coding_rules.deactivate')}
                </Button>
              )}
            </ConfirmButton>
          ) : (
            <Tooltip overlay={translate('coding_rules.can_not_deactivate')}>
              <Button className="coding-rules-detail-quality-profile-deactivate button-red disabled">
                {translate('coding_rules.deactivate')}
              </Button>
            </Tooltip>
          )}
        </td>
      );
    }

    return (
      <td className="coding-rule-table-meta-cell coding-rule-activation-actions">
        {!rule.isTemplate && (
          <ActivationButton
            buttonText={translate('coding_rules.activate')}
            className="coding-rules-detail-quality-profile-activate"
            modalHeader={translate('coding_rules.activate_in_quality_profile')}
            onDone={this.handleActivate}
            profiles={[selectedProfile]}
            rule={rule}
          />
        )}
      </td>
    );
  };

  render() {
    const { rule, selected } = this.props;
    const allTags = [...(rule.tags || []), ...(rule.sysTags || [])];
    return (
      <li
        className={classNames('coding-rule', { selected })}
        aria-current={selected}
        data-rule={rule.key}
      >
        <table className="coding-rule-table">
          <tbody>
            <tr>
              {this.renderActivation()}
              <td>
                <div className="coding-rule-title">
                  <Link
                    className="link-no-underline"
                    onClick={this.handleNameClick}
                    to={getRuleUrl(rule.key)}
                  >
                    {rule.name}
                  </Link>
                  {rule.isTemplate && (
                    <Tooltip overlay={translate('coding_rules.rule_template.title')}>
                      <span className="badge spacer-left">
                        {translate('coding_rules.rule_template')}
                      </span>
                    </Tooltip>
                  )}
                </div>
              </td>

              <td className="coding-rule-table-meta-cell">
                <div className="display-flex-center coding-rule-meta">
                  {rule.status !== 'READY' && (
                    <span className="spacer-left badge badge-error">
                      {translate('rules.status', rule.status)}
                    </span>
                  )}
                  <span className="display-inline-flex-center spacer-left note">
                    {rule.langName}
                  </span>
                  <Tooltip overlay={translate('coding_rules.type.tooltip', rule.type)}>
                    <span className="display-inline-flex-center spacer-left note">
                      <IssueTypeIcon query={rule.type} />
                      <span className="little-spacer-left text-middle">
                        {translate('issue.type', rule.type)}
                      </span>
                    </span>
                  </Tooltip>
                  {allTags.length > 0 && (
                    <TagsList
                      allowUpdate={false}
                      className="display-inline-flex-center note spacer-left"
                      tags={allTags}
                    />
                  )}
                  <SimilarRulesFilter onFilterChange={this.props.onFilterChange} rule={rule} />
                </div>
              </td>

              {this.renderActions()}
            </tr>
          </tbody>
        </table>
      </li>
    );
  }
}
