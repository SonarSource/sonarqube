/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import * as classNames from 'classnames';
import * as React from 'react';
import { Link } from 'react-router';
import { Button } from 'sonar-ui-common/components/controls/buttons';
import ConfirmButton from 'sonar-ui-common/components/controls/ConfirmButton';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import IssueTypeIcon from 'sonar-ui-common/components/icons/IssueTypeIcon';
import SeverityIcon from 'sonar-ui-common/components/icons/SeverityIcon';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { deactivateRule, Profile } from '../../../api/quality-profiles';
import TagsList from '../../../components/tags/TagsList';
import { getRuleUrl } from '../../../helpers/urls';
import { Activation, Query } from '../query';
import ActivationButton from './ActivationButton';
import RuleInheritanceIcon from './RuleInheritanceIcon';
import SimilarRulesFilter from './SimilarRulesFilter';

interface Props {
  activation?: Activation;
  canWrite?: boolean;
  isLoggedIn: boolean;
  onActivate: (profile: string, rule: string, activation: Activation) => void;
  onDeactivate: (profile: string, rule: string) => void;
  onFilterChange: (changes: Partial<Query>) => void;
  onOpen: (ruleKey: string) => void;
  organization: string | undefined;
  rule: T.Rule;
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
        inherit: 'NONE'
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
                )}>
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
                )}>
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
          {this.renderDeactivateButton('', 'coding_rules.need_extend_or_copy')}
        </td>
      );
    }

    const canEdit = selectedProfile.actions && selectedProfile.actions.edit;
    if (!canEdit) {
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

  renderDeactivateButton = (
    inherit: string,
    overlayTranslationKey = 'coding_rules.can_not_deactivate'
  ) => {
    return inherit === 'NONE' ? (
      <ConfirmButton
        confirmButtonText={translate('yes')}
        modalBody={translate('coding_rules.deactivate.confirm')}
        modalHeader={translate('coding_rules.deactivate')}
        onConfirm={this.handleDeactivate}>
        {({ onClick }) => (
          <Button
            className="coding-rules-detail-quality-profile-deactivate button-red"
            onClick={onClick}>
            {translate('coding_rules.deactivate')}
          </Button>
        )}
      </ConfirmButton>
    ) : (
      <Tooltip overlay={translate(overlayTranslationKey)}>
        <Button
          className="coding-rules-detail-quality-profile-deactivate button-red"
          disabled={true}>
          {translate('coding_rules.deactivate')}
        </Button>
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
                  <Link
                    className="link-no-underline"
                    onClick={this.handleNameClick}
                    to={getRuleUrl(rule.key, this.props.organization)}>
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
                  <span className="spacer-left note">{rule.langName}</span>
                  <Tooltip overlay={translate('coding_rules.type.tooltip', rule.type)}>
                    <span className="display-inline-flex-center spacer-left note">
                      <IssueTypeIcon className="little-spacer-right" query={rule.type} />
                      {translate('issue.type', rule.type)}
                    </span>
                  </Tooltip>
                  {allTags.length > 0 && (
                    <TagsList allowUpdate={false} className="note spacer-left" tags={allTags} />
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
