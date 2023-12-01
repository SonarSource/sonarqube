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
import ConfirmButton from '../../../components/controls/ConfirmButton';
import IssueSeverityIcon from '../../../components/icon-mappers/IssueSeverityIcon';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getRuleUrl } from '../../../helpers/urls';
import { useDeleteRuleMutation, useSearchRulesQuery } from '../../../queries/rules';
import { IssueSeverity } from '../../../types/issues';
import { Rule, RuleDetails } from '../../../types/types';
import CustomRuleButton from './CustomRuleButton';

interface Props {
  canChange?: boolean;
  ruleDetails: RuleDetails;
}

const COLUMN_COUNT = 3;
const COLUMN_COUNT_WITH_EDIT_PERMISSIONS = 4;

export default function RuleDetailsCustomRules(props: Readonly<Props>) {
  const { ruleDetails } = props;
  const rulesSearchParams = {
    f: 'name,severity,params',
    template_key: ruleDetails.key,
  };
  const { isLoading: loadingRules, data } = useSearchRulesQuery(rulesSearchParams);
  const { mutate: deleteRules, isLoading: deletingRule } = useDeleteRuleMutation(rulesSearchParams);

  const loading = loadingRules || deletingRule;
  const rules = data?.rules ?? [];

  const handleRuleDelete = React.useCallback(
    (ruleKey: string) => {
      deleteRules({ key: ruleKey });
    },
    [deleteRules],
  );

  return (
    <div className="js-rule-custom-rules">
      <div>
        <HeadingDark as="h2">{translate('coding_rules.custom_rules')}</HeadingDark>

        {props.canChange && (
          <CustomRuleButton templateRule={ruleDetails}>
            {({ onClick }) => (
              <ButtonSecondary className="js-create-custom-rule sw-mt-6" onClick={onClick}>
                {translate('coding_rules.create')}
              </ButtonSecondary>
            )}
          </CustomRuleButton>
        )}
        {rules.length > 0 && (
          <Table
            className="sw-my-6"
            id="coding-rules-detail-custom-rules"
            columnCount={props.canChange ? COLUMN_COUNT_WITH_EDIT_PERMISSIONS : COLUMN_COUNT}
          >
            {sortBy(rules, (rule) => rule.name).map((rule) => (
              <RuleListItem
                key={rule.key}
                rule={rule}
                editable={props.canChange}
                onDelete={handleRuleDelete}
              />
            ))}
          </Table>
        )}
        <Spinner className="sw-my-6" loading={loading} />
      </div>
    </div>
  );
}

function RuleListItem(
  props: Readonly<{
    rule: Rule;
    editable?: boolean;
    onDelete: (ruleKey: string) => void;
  }>,
) {
  const { rule, editable } = props;
  return (
    <TableRow data-rule={rule.key}>
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

      {editable && (
        <ContentCell>
          <ConfirmButton
            confirmButtonText={translate('delete')}
            confirmData={rule.key}
            isDestructive
            modalBody={translateWithParameters('coding_rules.delete.custom.confirm', rule.name)}
            modalHeader={translate('coding_rules.delete_rule')}
            onConfirm={props.onDelete}
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
}
