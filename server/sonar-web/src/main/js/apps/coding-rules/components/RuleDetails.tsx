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
import * as React from 'react';
import CustomRuleButton from './CustomRuleButton';
import RuleDetailsCustomRules from './RuleDetailsCustomRules';
import RuleDetailsDescription from './RuleDetailsDescription';
import RuleDetailsIssues from './RuleDetailsIssues';
import RuleDetailsMeta from './RuleDetailsMeta';
import RuleDetailsParameters from './RuleDetailsParameters';
import RuleDetailsProfiles from './RuleDetailsProfiles';
import { Query, Activation } from '../query';
import { Profile } from '../../../api/quality-profiles';
import { getRuleDetails, deleteRule, updateRule } from '../../../api/rules';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import DocTooltip from '../../../components/docs/DocTooltip';
import { Button } from '../../../components/ui/buttons';
import { translate, translateWithParameters } from '../../../helpers/l10n';

interface Props {
  allowCustomRules?: boolean;
  canWrite?: boolean;
  hideQualityProfiles?: boolean;
  onActivate: (profile: string, rule: string, activation: Activation) => void;
  onDeactivate: (profile: string, rule: string) => void;
  onDelete: (rule: string) => void;
  onFilterChange: (changes: Partial<Query>) => void;
  organization: string | undefined;
  referencedProfiles: T.Dict<Profile>;
  referencedRepositories: T.Dict<{ key: string; language: string; name: string }>;
  ruleKey: string;
  selectedProfile?: Profile;
}

interface State {
  actives?: T.RuleActivation[];
  loading: boolean;
  ruleDetails?: T.RuleDetails;
}

export default class RuleDetails extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true };

  componentDidMount() {
    this.mounted = true;
    this.setState({ loading: true });
    this.fetchRuleDetails();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.ruleKey !== this.props.ruleKey) {
      this.setState({ loading: true });
      this.fetchRuleDetails();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchRuleDetails = () =>
    getRuleDetails({
      actives: true,
      key: this.props.ruleKey,
      organization: this.props.organization
    }).then(
      ({ actives, rule }) => {
        if (this.mounted) {
          this.setState({ actives, loading: false, ruleDetails: rule });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );

  handleRuleChange = (ruleDetails: T.RuleDetails) => {
    if (this.mounted) {
      this.setState({ ruleDetails });
    }
  };

  handleTagsChange = (tags: string[]) => {
    // optimistic update
    const oldTags = this.state.ruleDetails && this.state.ruleDetails.tags;
    this.setState(state =>
      state.ruleDetails ? { ruleDetails: { ...state.ruleDetails, tags } } : null
    );
    updateRule({
      key: this.props.ruleKey,
      organization: this.props.organization,
      tags: tags.join()
    }).catch(() => {
      if (this.mounted) {
        this.setState(state =>
          state.ruleDetails ? { ruleDetails: { ...state.ruleDetails, tags: oldTags } } : null
        );
      }
    });
  };

  handleActivate = () =>
    this.fetchRuleDetails().then(() => {
      const { ruleKey, selectedProfile } = this.props;
      if (selectedProfile && this.state.actives) {
        const active = this.state.actives.find(active => active.qProfile === selectedProfile.key);
        if (active) {
          this.props.onActivate(selectedProfile.key, ruleKey, active);
        }
      }
    });

  handleDeactivate = () =>
    this.fetchRuleDetails().then(() => {
      const { ruleKey, selectedProfile } = this.props;
      if (selectedProfile && this.state.actives) {
        if (!this.state.actives.find(active => active.qProfile === selectedProfile.key)) {
          this.props.onDeactivate(selectedProfile.key, ruleKey);
        }
      }
    });

  handleDelete = () =>
    deleteRule({ key: this.props.ruleKey, organization: this.props.organization }).then(() =>
      this.props.onDelete(this.props.ruleKey)
    );

  render() {
    const { ruleDetails } = this.state;

    if (!ruleDetails) {
      return <div className="coding-rule-details" />;
    }

    const {
      allowCustomRules,
      canWrite,
      hideQualityProfiles,
      organization,
      referencedProfiles
    } = this.props;
    const { params = [] } = ruleDetails;

    const isCustom = !!ruleDetails.templateKey;
    const isEditable = canWrite && !!this.props.allowCustomRules && isCustom;

    return (
      <div className="coding-rule-details">
        <DeferredSpinner loading={this.state.loading}>
          <RuleDetailsMeta
            canWrite={canWrite}
            onFilterChange={this.props.onFilterChange}
            onTagsChange={this.handleTagsChange}
            organization={organization}
            referencedRepositories={this.props.referencedRepositories}
            ruleDetails={ruleDetails}
          />

          <RuleDetailsDescription
            canWrite={canWrite}
            onChange={this.handleRuleChange}
            organization={organization}
            ruleDetails={ruleDetails}
          />

          {params.length > 0 && <RuleDetailsParameters params={params} />}

          {isEditable && (
            <div className="coding-rules-detail-description display-flex-center">
              {/* `templateRule` is used to get rule meta data, `customRule` is used to get parameter values */}
              {/* it's expected to pass the same rule to both parameters */}
              <CustomRuleButton
                customRule={ruleDetails}
                onDone={this.handleRuleChange}
                organization={organization}
                templateRule={ruleDetails}>
                {({ onClick }) => (
                  <Button
                    className="js-edit-custom"
                    id="coding-rules-detail-custom-rule-change"
                    onClick={onClick}>
                    {translate('edit')}
                  </Button>
                )}
              </CustomRuleButton>
              <ConfirmButton
                confirmButtonText={translate('delete')}
                isDestructive={true}
                modalBody={translateWithParameters(
                  'coding_rules.delete.custom.confirm',
                  ruleDetails.name
                )}
                modalHeader={translate('coding_rules.delete_rule')}
                onConfirm={this.handleDelete}>
                {({ onClick }) => (
                  <>
                    <Button
                      className="button-red spacer-left js-delete"
                      id="coding-rules-detail-rule-delete"
                      onClick={onClick}>
                      {translate('delete')}
                    </Button>
                    <DocTooltip
                      className="spacer-left"
                      doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/rules/custom-rule-removal.md')}
                    />
                  </>
                )}
              </ConfirmButton>
            </div>
          )}

          {ruleDetails.isTemplate && (
            <RuleDetailsCustomRules
              canChange={allowCustomRules && canWrite}
              organization={organization}
              ruleDetails={ruleDetails}
            />
          )}

          {!ruleDetails.isTemplate && !hideQualityProfiles && (
            <RuleDetailsProfiles
              activations={this.state.actives}
              canWrite={canWrite}
              onActivate={this.handleActivate}
              onDeactivate={this.handleDeactivate}
              organization={organization}
              referencedProfiles={referencedProfiles}
              ruleDetails={ruleDetails}
            />
          )}

          {!ruleDetails.isTemplate && (
            <RuleDetailsIssues organization={organization} ruleDetails={ruleDetails} />
          )}
        </DeferredSpinner>
      </div>
    );
  }
}
