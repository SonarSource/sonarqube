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
import * as classNames from 'classnames';
import { Link } from 'react-router';
import ActivationButton from './ActivationButton';
import SimilarRulesFilter from './SimilarRulesFilter';
import { Activation, Query } from '../query';
import { Profile, deactivateRule } from '../../../api/quality-profiles';
import { Rule, RuleInheritance } from '../../../app/types';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import Tooltip from '../../../components/controls/Tooltip';
import SeverityIcon from '../../../components/shared/SeverityIcon';
import IssueTypeIcon from '../../../components/ui/IssueTypeIcon';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  activation?: Activation;
  onActivate: (profile: string, rule: string, activation: Activation) => void;
  onDeactivate: (profile: string, rule: string) => void;
  onFilterChange: (changes: Partial<Query>) => void;
  organization: string | undefined;
  path: { pathname: string; query: { [x: string]: any } };
  rule: Rule;
  selected: boolean;
  selectedProfile?: Profile;
}

export default class RuleListItem extends React.PureComponent<Props> {
  handleDeactivate = () => {
    if (this.props.selectedProfile) {
      const data = {
        key: this.props.selectedProfile.key,
        organization: this.props.organization,
        rule: this.props.rule.key
      };
      deactivateRule(data).then(() => this.props.onDeactivate(data.key, data.rule), () => {});
    }
  };

  handleActivate = (severity: string) => {
    if (this.props.selectedProfile) {
      this.props.onActivate(this.props.selectedProfile.key, this.props.rule.key, {
        severity,
        inherit: RuleInheritance.NotInherited
      });
    }
    return Promise.resolve();
  };

  renderActivation = () => {
    const { activation, selectedProfile } = this.props;
    if (!activation) {
      return null;
    }

    return (
      <td className="coding-rule-table-meta-cell coding-rule-activation">
        <SeverityIcon severity={activation.severity} />
        {selectedProfile &&
          selectedProfile.parentName && (
            <>
              {activation.inherit === RuleInheritance.Overridden && (
                <Tooltip
                  overlay={translateWithParameters(
                    'coding_rules.overrides',
                    selectedProfile.name,
                    selectedProfile.parentName
                  )}>
                  <i className="little-spacer-left icon-inheritance icon-inheritance-overridden" />
                </Tooltip>
              )}
              {activation.inherit === RuleInheritance.Inherited && (
                <Tooltip
                  overlay={translateWithParameters(
                    'coding_rules.inherits',
                    selectedProfile.name,
                    selectedProfile.parentName
                  )}>
                  <i className="little-spacer-left icon-inheritance" />
                </Tooltip>
              )}
            </>
          )}
      </td>
    );
  };

  renderActions = () => {
    const { activation, rule, selectedProfile } = this.props;
    if (!selectedProfile) {
      return null;
    }

    const canEdit = selectedProfile.actions && selectedProfile.actions.edit;
    if (!canEdit || selectedProfile.isBuiltIn) {
      return null;
    }

    return (
      <td className="coding-rule-table-meta-cell coding-rule-activation-actions">
        {activation
          ? this.renderDeactivateButton(activation.inherit)
          : !rule.isTemplate && (
              <ActivationButton
                buttonText={translate('coding_rules.activate')}
                className="coding-rules-detail-quality-profile-activate"
                modalHeader={translate('coding_rules.activate_in_quality_profile')}
                onDone={this.handleActivate}
                organization={this.props.organization}
                profiles={[selectedProfile]}
                rule={rule}
              />
            )}
      </td>
    );
  };

  renderDeactivateButton = (inherit: string) => {
    return inherit === 'NONE' ? (
      <ConfirmButton
        confirmButtonText={translate('yes')}
        modalBody={translate('coding_rules.deactivate.confirm')}
        modalHeader={translate('coding_rules.deactivate')}
        onConfirm={this.handleDeactivate}>
        {({ onClick }) => (
          <button
            className="coding-rules-detail-quality-profile-deactivate button-red"
            onClick={onClick}>
            {translate('coding_rules.deactivate')}
          </button>
        )}
      </ConfirmButton>
    ) : (
      <Tooltip overlay={translate('coding_rules.can_not_deactivate')} placement="left">
        <button className="coding-rules-detail-quality-profile-deactivate button-red disabled">
          {translate('coding_rules.deactivate')}
        </button>
      </Tooltip>
    );
  };

  render() {
    const { rule, selected } = this.props;
    const allTags = [...(rule.tags || []), ...(rule.sysTags || [])];
    return (
      <div className={classNames('coding-rule', { selected })} data-rule={rule.key}>
        <table className="coding-rule-table">
          <tbody>
            <tr>
              {this.renderActivation()}

              <td>
                <div className="coding-rule-title">
                  <Link className="link-no-underline" to={this.props.path}>
                    {rule.name}
                  </Link>
                  {rule.isTemplate && (
                    <Tooltip overlay={translate('coding_rules.rule_template.title')}>
                      <span className="outline-badge spacer-left">
                        {translate('coding_rules.rule_template')}
                      </span>
                    </Tooltip>
                  )}
                </div>
              </td>

              <td className="coding-rule-table-meta-cell">
                <div className="display-flex-center coding-rule-meta">
                  {rule.status !== 'READY' && (
                    <span className="spacer-left badge badge-normal-size badge-danger-light">
                      {translate('rules.status', rule.status)}
                    </span>
                  )}
                  <span className="spacer-left note">{rule.langName}</span>
                  <Tooltip overlay={translate('coding_rules.type.tooltip', rule.type)}>
                    <span className="display-inline-flex-center spacer-left note">
                      <IssueTypeIcon className="little-spacer-right" query={rule.type} />
                      {translate('issue.type', rule.type)}
                    </span>
                  </Tooltip>
                  {allTags.length > 0 && (
                    <span className="spacer-left">
                      <i className="icon-tags little-spacer-right" />
                      <span className="note">{allTags.join(', ')}</span>
                    </span>
                  )}
                  <SimilarRulesFilter onFilterChange={this.props.onFilterChange} rule={rule} />
                </div>
              </td>

              {this.renderActions()}
            </tr>
          </tbody>
        </table>
      </div>
    );
  }
}
