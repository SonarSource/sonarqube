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
import { sortBy } from 'lodash';
import * as React from 'react';
import { deleteRule, searchRules } from '../../../api/rules';
import Link from '../../../components/common/Link';
import { Button } from '../../../components/controls/buttons';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import SeverityHelper from '../../../components/shared/SeverityHelper';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getRuleUrl } from '../../../helpers/urls';
import { Rule, RuleDetails } from '../../../types/types';
import CustomRuleButton from './CustomRuleButton';

interface Props {
  canChange?: boolean;
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
      template_key: this.props.ruleDetails.key,
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
        rules: [...rules, newRuleDetails],
      }));
    }
  };

  handleRuleDelete = (ruleKey: string) => {
    return deleteRule({ key: ruleKey }).then(() => {
      if (this.mounted) {
        this.setState(({ rules = [] }) => ({
          rules: rules.filter((rule) => rule.key !== ruleKey),
        }));
      }
    });
  };

  renderRule = (rule: Rule) => (
    <tr data-rule={rule.key} key={rule.key}>
      <td className="coding-rules-detail-list-name">
        <Link to={getRuleUrl(rule.key)}>{rule.name}</Link>
      </td>

      <td className="coding-rules-detail-list-severity">
        <SeverityHelper className="display-flex-center" severity={rule.severity} />
      </td>

      <td className="coding-rules-detail-list-parameters">
        {rule.params &&
          rule.params
            .filter((param) => param.defaultValue)
            .map((param) => (
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
            onConfirm={this.handleRuleDelete}
          >
            {({ onClick }) => (
              <Button className="button-red js-delete-custom-rule" onClick={onClick}>
                {translate('delete')}
              </Button>
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
          <h2 className="coding-rules-detail-title">{translate('coding_rules.custom_rules')}</h2>

          {this.props.canChange && (
            <CustomRuleButton onDone={this.handleRuleCreate} templateRule={this.props.ruleDetails}>
              {({ onClick }) => (
                <Button className="js-create-custom-rule spacer-left" onClick={onClick}>
                  {translate('coding_rules.create')}
                </Button>
              )}
            </CustomRuleButton>
          )}

          <DeferredSpinner className="spacer-left" loading={loading}>
            {rules.length > 0 && (
              <table className="coding-rules-detail-list" id="coding-rules-detail-custom-rules">
                <tbody>{sortBy(rules, (rule) => rule.name).map(this.renderRule)}</tbody>
              </table>
            )}
          </DeferredSpinner>
        </div>
      </div>
    );
  }
}
