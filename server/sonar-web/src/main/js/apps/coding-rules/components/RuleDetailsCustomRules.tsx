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
import {
  ButtonSecondary,
  ContentCell,
  DangerButtonSecondary,
  HeadingDark,
  Link,
  Spinner,
  Table,
  TableRow,
  UnorderedList,
} from 'design-system';
import { sortBy } from 'lodash';
import * as React from 'react';
import { deleteRule, searchRules } from '../../../api/rules';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import IssueSeverityIcon from '../../../components/icon-mappers/IssueSeverityIcon';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getRuleUrl } from '../../../helpers/urls';
import { IssueSeverity } from '../../../types/issues';
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

const COLUMN_COUNT = 3;
const COLUMN_COUNT_WITH_EDIT_PERMISSIONS = 4;

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
      },
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
    <TableRow data-rule={rule.key} key={rule.key}>
      <ContentCell>
        <div>
          <Link to={getRuleUrl(rule.key)}>{rule.name}</Link>
        </div>
      </ContentCell>

      <ContentCell>
        <IssueSeverityIcon
          className="sw-mr-1"
          severity={rule.severity as IssueSeverity}
          aria-hidden
        />
        {translate('severity', rule.severity)}
      </ContentCell>

      <ContentCell>
        <UnorderedList className="sw-mt-0">
          {rule.params
            ?.filter((param) => param.defaultValue)
            .map((param) => (
              <li key={param.key}>
                <span className="sw-font-semibold">{param.key}</span>
                <span>:&nbsp;</span>
                <span title={param.defaultValue}>{param.defaultValue}</span>
              </li>
            ))}
        </UnorderedList>
      </ContentCell>

      {this.props.canChange && (
        <ContentCell>
          <ConfirmButton
            confirmButtonText={translate('delete')}
            confirmData={rule.key}
            isDestructive
            modalBody={translateWithParameters('coding_rules.delete.custom.confirm', rule.name)}
            modalHeader={translate('coding_rules.delete_rule')}
            onConfirm={this.handleRuleDelete}
          >
            {({ onClick }) => (
              <DangerButtonSecondary
                className="js-delete-custom-rule"
                aria-label={translateWithParameters('coding_rules.delete_rule_x', rule.name)}
                onClick={onClick}
              >
                {translate('delete')}
              </DangerButtonSecondary>
            )}
          </ConfirmButton>
        </ContentCell>
      )}
    </TableRow>
  );

  render() {
    const { loading, rules = [] } = this.state;

    return (
      <div className="js-rule-custom-rules">
        <div>
          <HeadingDark as="h2">{translate('coding_rules.custom_rules')}</HeadingDark>

          {this.props.canChange && (
            <CustomRuleButton onDone={this.handleRuleCreate} templateRule={this.props.ruleDetails}>
              {({ onClick }) => (
                <ButtonSecondary className="js-create-custom-rule sw-mt-6" onClick={onClick}>
                  {translate('coding_rules.create')}
                </ButtonSecondary>
              )}
            </CustomRuleButton>
          )}

          <Spinner className="sw-my-6" loading={loading}>
            {rules.length > 0 && (
              <Table
                className="sw-my-6"
                id="coding-rules-detail-custom-rules"
                columnCount={
                  this.props.canChange ? COLUMN_COUNT_WITH_EDIT_PERMISSIONS : COLUMN_COUNT
                }
              >
                {sortBy(rules, (rule) => rule.name).map(this.renderRule)}
              </Table>
            )}
          </Spinner>
        </div>
      </div>
    );
  }
}
