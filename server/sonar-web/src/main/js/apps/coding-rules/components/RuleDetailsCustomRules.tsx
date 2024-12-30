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

import {
  Button,
  ButtonVariety,
  Heading,
  Link,
  ModalAlert,
  Spinner,
} from '@sonarsource/echoes-react';
import { sortBy } from 'lodash';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { ContentCell, Table, TableRow, UnorderedList } from '~design-system';
import { translate } from '../../../helpers/l10n';
import { getRuleUrl } from '../../../helpers/urls';
import { useDeleteRuleMutation, useSearchRulesQuery } from '../../../queries/rules';
import { Rule, RuleDetails } from '../../../types/types';
import CustomRuleButton from './CustomRuleButton';

interface Props {
  canChange?: boolean;
  ruleDetails: RuleDetails;
  organization: string;
}

const COLUMN_COUNT = 2;
const COLUMN_COUNT_WITH_EDIT_PERMISSIONS = 3;

export default function RuleDetailsCustomRules(props: Readonly<Props>) {
  const { ruleDetails, canChange, organization } = props;
  const rulesSearchParams = {
    organization,
    f: 'name,severity,params',
    template_key: ruleDetails.key,
  };
  const { isLoading: loadingRules, data } = useSearchRulesQuery(rulesSearchParams);
  const { mutate: deleteRules, isPending: deletingRule } = useDeleteRuleMutation(rulesSearchParams);

  const loading = loadingRules || deletingRule;
  const rules = data?.rules ?? [];

  const handleRuleDelete = React.useCallback(
    (ruleKey: string) => {
      deleteRules({ organization, key: ruleKey });
    },
    [deleteRules],
  );

  return (
    <div className="js-rule-custom-rules">
      <div>
        <Heading as="h2">{translate('coding_rules.custom_rules')}</Heading>

        {props.canChange && (
          <CustomRuleButton organization={organization} templateRule={ruleDetails}>
            {({ onClick }) => (
              <Button
                variety={ButtonVariety.Default}
                className="js-create-custom-rule sw-mt-6"
                onClick={onClick}
              >
                {translate('coding_rules.create')}
              </Button>
            )}
          </CustomRuleButton>
        )}
        {rules.length > 0 && (
          <Table
            className="sw-my-6"
            id="coding-rules-detail-custom-rules"
            columnCount={canChange ? COLUMN_COUNT_WITH_EDIT_PERMISSIONS : COLUMN_COUNT}
            columnWidths={canChange ? ['auto', 'auto', '1%'] : ['auto', 'auto']}
          >
            {sortBy(rules, (rule) => rule.name).map((rule) => (
              <RuleListItem
                organization={organization}
                key={rule.key}
                rule={rule}
                editable={canChange}
                onDelete={handleRuleDelete}
              />
            ))}
          </Table>
        )}
        <Spinner className="sw-my-6" isLoading={loading} />
      </div>
    </div>
  );
}

function RuleListItem(
  props: Readonly<{
    organization: string;
    editable?: boolean;
    onDelete: (ruleKey: string) => void;
    rule: Rule;
  }>,
) {
  const { rule, editable, organization } = props;
  const intl = useIntl();
  return (
    <TableRow data-rule={rule.key}>
      <ContentCell>
        <div>
          <Link to={getRuleUrl(rule.key, organization)}>{rule.name}</Link>
        </div>
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
          <ModalAlert
            title={translate('coding_rules.delete_rule')}
            description={intl.formatMessage(
              {
                id: 'coding_rules.delete.custom.confirm',
              },
              {
                name: rule.name,
              },
            )}
            primaryButton={
              <Button
                className="sw-ml-2 js-delete"
                id="coding-rules-detail-rule-delete"
                onClick={() => props.onDelete(rule.key)}
                variety={ButtonVariety.DangerOutline}
              >
                {translate('delete')}
              </Button>
            }
            secondaryButtonLabel={translate('close')}
          >
            <Button
              className="js-delete-custom-rule"
              aria-label={intl.formatMessage(
                { id: 'coding_rules.delete_rule_x' },
                { name: rule.name },
              )}
              variety={ButtonVariety.DangerOutline}
            >
              {translate('delete')}
            </Button>
          </ModalAlert>
        </ContentCell>
      )}
    </TableRow>
  );
}
