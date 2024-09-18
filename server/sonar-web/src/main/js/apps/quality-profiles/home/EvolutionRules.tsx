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

import { Heading } from '@sonarsource/echoes-react';
import { DiscreetLink, Link, Note } from 'design-system';
import { noop, sortBy } from 'lodash';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { formatMeasure } from '~sonar-aligned/helpers/measures';
import { MetricType } from '~sonar-aligned/types/metrics';
import { listRules } from '../../../api/rules';
import { toShortISO8601String } from '../../../helpers/dates';
import { translateWithParameters } from '../../../helpers/l10n';
import { isDefined } from '../../../helpers/types';
import { getRulesUrl } from '../../../helpers/urls';
import { Rule, RuleActivation } from '../../../types/types';

const RULES_LIMIT = 10;

interface ExtendedRule extends Rule {
  activations: number;
}

export default function EvolutionRules() {
  const intl = useIntl();
  const [latestRules, setLatestRules] = React.useState<ExtendedRule[]>();
  const [latestRulesTotal, setLatestRulesTotal] = React.useState<number>();

  const periodStartDate = React.useMemo(() => {
    const startDate = new Date();
    startDate.setFullYear(startDate.getFullYear() - 1);
    return toShortISO8601String(startDate);
  }, []);

  React.useEffect(() => {
    const data = {
      asc: false,
      available_since: periodStartDate,
      f: 'name,langName,actives',
      ps: RULES_LIMIT,
      s: 'createdAt',
    };

    listRules(data).then(({ actives, rules, paging: { total } }) => {
      setLatestRules(sortBy(parseRules(rules, actives), 'langName'));
      setLatestRulesTotal(total);
    }, noop);
  }, [periodStartDate]);

  if (!(isDefined(latestRulesTotal) && latestRulesTotal !== 0) || !latestRules) {
    return null;
  }

  return (
    <section aria-label={intl.formatMessage({ id: 'quality_profiles.latest_new_rules' })}>
      <Heading as="h2" hasMarginBottom>
        {intl.formatMessage({ id: 'quality_profiles.latest_new_rules' })}
      </Heading>

      <ul className="sw-flex sw-flex-col sw-gap-4 sw-body-sm">
        {latestRules.map((rule) => (
          <li className="sw-flex sw-flex-col sw-gap-1" key={rule.key}>
            <div className="sw-truncate">
              <DiscreetLink to={getRulesUrl({ rule_key: rule.key })}>{rule.name}</DiscreetLink>
            </div>

            <Note className="sw-truncate">
              {rule.activations
                ? translateWithParameters(
                    'quality_profiles.latest_new_rules.activated',
                    rule.langName!,
                    rule.activations,
                  )
                : translateWithParameters(
                    'quality_profiles.latest_new_rules.not_activated',
                    rule.langName!,
                  )}
            </Note>
          </li>
        ))}
      </ul>

      {latestRulesTotal > RULES_LIMIT && (
        <div className="sw-mt-6 sw-body-sm-highlight">
          <Link to={getRulesUrl({ available_since: periodStartDate })}>
            {intl.formatMessage(
              { id: 'quality_profiles.latest_new_rules.see_all_x' },
              { count: formatMeasure(latestRulesTotal, MetricType.ShortInteger) },
            )}
          </Link>
        </div>
      )}
    </section>
  );
}

function parseRules(rules: Rule[], actives?: Record<string, RuleActivation[]>): ExtendedRule[] {
  return rules.map((rule) => {
    const activations = actives?.[rule.key]?.length ?? 0;

    return { ...rule, activations };
  });
}
