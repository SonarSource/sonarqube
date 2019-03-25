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
import { Link } from 'react-router';
import { sortBy } from 'lodash';
import { searchRules } from '../../../api/rules';
import { translateWithParameters, translate } from '../../../helpers/l10n';
import { getRulesUrl } from '../../../helpers/urls';
import { toShortNotSoISOString } from '../../../helpers/dates';
import { formatMeasure } from '../../../helpers/measures';

const RULES_LIMIT = 10;

function parseRules(rules: T.Rule[], actives?: T.Dict<T.RuleActivation[]>): ExtendedRule[] {
  return rules.map(rule => {
    const activations = actives && actives[rule.key];
    return { ...rule, activations: activations ? activations.length : 0 };
  });
}

interface Props {
  organization: string | null;
}

interface ExtendedRule extends T.Rule {
  activations: number;
}

interface State {
  latestRules?: ExtendedRule[];
  latestRulesTotal?: number;
}

export default class EvolutionRules extends React.PureComponent<Props, State> {
  periodStartDate: string;
  mounted = false;

  constructor(props: Props) {
    super(props);
    this.state = {};
    const startDate = new Date();
    startDate.setFullYear(startDate.getFullYear() - 1);
    this.periodStartDate = toShortNotSoISOString(startDate);
  }

  componentDidMount() {
    this.mounted = true;
    this.loadLatestRules();
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadLatestRules() {
    const data = {
      asc: false,
      available_since: this.periodStartDate,
      f: 'name,langName,actives',
      organization: this.props.organization || undefined,
      ps: RULES_LIMIT,
      s: 'createdAt'
    };

    searchRules(data).then(
      ({ actives, rules, total }) => {
        if (this.mounted) {
          this.setState({
            latestRules: sortBy(parseRules(rules, actives), 'langName'),
            latestRulesTotal: total
          });
        }
      },
      () => {}
    );
  }

  render() {
    if (!this.state.latestRulesTotal || !this.state.latestRules) {
      return null;
    }

    const newRulesUrl = getRulesUrl(
      { available_since: this.periodStartDate },
      this.props.organization
    );

    return (
      <div className="boxed-group boxed-group-inner quality-profiles-evolution-rules">
        <div className="clearfix">
          <strong className="pull-left">{translate('quality_profiles.latest_new_rules')}</strong>
        </div>
        <ul>
          {this.state.latestRules.map(rule => (
            <li className="spacer-top" key={rule.key}>
              <div className="text-ellipsis">
                <Link
                  className="link-no-underline"
                  to={getRulesUrl({ rule_key: rule.key }, this.props.organization)}>
                  {' '}
                  {rule.name}
                </Link>
                <div className="note">
                  {rule.activations
                    ? translateWithParameters(
                        'quality_profiles.latest_new_rules.activated',
                        rule.langName!,
                        rule.activations
                      )
                    : translateWithParameters(
                        'quality_profiles.latest_new_rules.not_activated',
                        rule.langName!
                      )}
                </div>
              </div>
            </li>
          ))}
        </ul>
        {this.state.latestRulesTotal > RULES_LIMIT && (
          <div className="spacer-top">
            <Link className="small" to={newRulesUrl}>
              {translate('see_all')} {formatMeasure(this.state.latestRulesTotal, 'SHORT_INT', null)}
            </Link>
          </div>
        )}
      </div>
    );
  }
}
