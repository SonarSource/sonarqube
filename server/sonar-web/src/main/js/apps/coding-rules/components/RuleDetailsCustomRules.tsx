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
import { sortBy } from 'lodash';
import CustomRuleButton from './CustomRuleButton';
import { searchRules, deleteRule } from '../../../api/rules';
import { Rule, RuleDetails } from '../../../app/types';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import SeverityHelper from '../../../components/shared/SeverityHelper';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getRuleUrl } from '../../../helpers/urls';

interface Props {
  canChange?: boolean;
  organization: string | undefined;
  ruleDetails: RuleDetails;
}

interface State {
  loading: boolean;
  rules?: Rule[];
}

export default class RuleDetailsCustomRules extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: false };

  componentDidMount() {
    this.mounted = true;
    this.fetchRules();
  }

  componentDidUpdate(prevProps: Props) {
    if (prevProps.ruleDetails.key !== this.props.ruleDetails.key) {
      this.fetchRules();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchRules = () => {
    this.setState({ loading: true });
    searchRules({
      f: 'name,severity,params',
      organization: this.props.organization,
      /* eslint-disable camelcase */
      template_key: this.props.ruleDetails.key
      /* eslint-enable camelcase */
    }).then(
      ({ rules }) => {
        if (this.mounted) {
          this.setState({ rules, loading: false });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  handleRuleCreate = (newRuleDetails: RuleDetails) => {
    if (this.mounted) {
      this.setState(({ rules = [] }: State) => ({
        rules: [...rules, newRuleDetails]
      }));
    }
  };

  handleRuleDelete = (ruleKey: string) => {
    return deleteRule({ key: ruleKey, organization: this.props.organization }).then(() => {
      if (this.mounted) {
        this.setState(({ rules = [] }) => ({
          rules: rules.filter(rule => rule.key !== ruleKey)
        }));
      }
    });
  };

  renderRule = (rule: Rule) => (
    <tr key={rule.key} data-rule={rule.key}>
      <td className="coding-rules-detail-list-name">
        <Link to={getRuleUrl(rule.key, this.props.organization)}>{rule.name}</Link>
      </td>

      <td className="coding-rules-detail-list-severity">
        <SeverityHelper severity={rule.severity} />
      </td>

      <td className="coding-rules-detail-list-parameters">
        {rule.params &&
          rule.params.filter(param => param.defaultValue).map(param => (
            <div className="coding-rules-detail-list-parameter" key={param.key}>
              <span className="key">{param.key}</span>
              <span className="sep">:&nbsp;</span>
              <span className="value" title={param.defaultValue}>
                {param.defaultValue}
              </span>
            </div>
          ))}
      </td>

      {this.props.canChange && (
        <td className="coding-rules-detail-list-actions">
          <ConfirmButton
            confirmButtonText={translate('delete')}
            confirmData={rule.key}
            isDestructive={true}
            modalBody={translateWithParameters('coding_rules.delete.custom.confirm', rule.name)}
            modalHeader={translate('coding_rules.delete_rule')}
            onConfirm={this.handleRuleDelete}>
            {({ onClick }) => (
              <button className="button-red js-delete-custom-rule" onClick={onClick}>
                {translate('delete')}
              </button>
            )}
          </ConfirmButton>
        </td>
      )}
    </tr>
  );

  render() {
    const { loading, rules = [] } = this.state;

    return (
      <div className="js-rule-custom-rules coding-rule-section">
        <div className="coding-rules-detail-custom-rules-section">
          <div className="coding-rule-section-separator" />

          <h3 className="coding-rules-detail-title">{translate('coding_rules.custom_rules')}</h3>

          {this.props.canChange && (
            <CustomRuleButton
              onDone={this.handleRuleCreate}
              organization={this.props.organization}
              templateRule={this.props.ruleDetails}>
              {({ onClick }) => (
                <button className="js-create-custom-rule spacer-left" onClick={onClick}>
                  {translate('coding_rules.create')}
                </button>
              )}
            </CustomRuleButton>
          )}

          <DeferredSpinner loading={loading}>
            {rules.length > 0 && (
              <table id="coding-rules-detail-custom-rules" className="coding-rules-detail-list">
                <tbody>{sortBy(rules, rule => rule.name).map(this.renderRule)}</tbody>
              </table>
            )}
          </DeferredSpinner>
        </div>
      </div>
    );
  }
}
