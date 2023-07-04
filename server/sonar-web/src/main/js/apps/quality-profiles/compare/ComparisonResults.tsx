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
import classNames from 'classnames';
import * as React from 'react';
import { CompareResponse, Profile } from '../../../api/quality-profiles';
import Link from '../../../components/common/Link';
import ChevronLeftIcon from '../../../components/icons/ChevronLeftIcon';
import ChevronRightIcon from '../../../components/icons/ChevronRightIcon';
import SeverityIcon from '../../../components/icons/SeverityIcon';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getRulesUrl } from '../../../helpers/urls';
import { Dict } from '../../../types/types';
import ComparisonResultActivation from './ComparisonResultActivation';

type Params = Dict<string>;

interface Props extends CompareResponse {
  leftProfile: Profile;
  refresh: () => Promise<void>;
  rightProfile?: Profile;
}

export default class ComparisonResults extends React.PureComponent<Props> {
  canActivate(profile: Profile) {
    return !profile.isBuiltIn && profile.actions && profile.actions.edit;
  }

  renderRule(rule: { key: string; name: string }, severity: string) {
    return (
      <div>
        <SeverityIcon severity={severity} />{' '}
        <Link to={getRulesUrl({ rule_key: rule.key, open: rule.key })}>{rule.name}</Link>
      </div>
    );
  }

  renderParameters(params: Params) {
    if (!params) {
      return null;
    }
    return (
      <ul>
        {Object.keys(params).map((key) => (
          <li className="spacer-top break-word" key={key}>
            <code>
              {key}
              {': '}
              {params[key]}
            </code>
          </li>
        ))}
      </ul>
    );
  }

  renderLeft() {
    if (this.props.inLeft.length === 0) {
      return null;
    }

    const renderSecondColumn = this.props.rightProfile && this.canActivate(this.props.rightProfile);

    return (
      <table className="data fixed zebra">
        <thead>
          <tr>
            <th>
              {translateWithParameters(
                'quality_profiles.x_rules_only_in',
                this.props.inLeft.length
              )}{' '}
              {this.props.left.name}
            </th>
            {renderSecondColumn && <th aria-label={translate('actions')}>&nbsp;</th>}
          </tr>
        </thead>
        <tbody>
          {this.props.inLeft.map((rule) => (
            <tr key={`left-${rule.key}`}>
              <td>{this.renderRule(rule, rule.severity)}</td>
              {renderSecondColumn && (
                <td>
                  <ComparisonResultActivation
                    key={rule.key}
                    onDone={this.props.refresh}
                    profile={this.props.rightProfile as Profile}
                    ruleKey={rule.key}
                  >
                    <ChevronRightIcon />
                  </ComparisonResultActivation>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    );
  }

  renderRight() {
    if (this.props.inRight.length === 0) {
      return null;
    }

    const renderFirstColumn = this.props.leftProfile && this.canActivate(this.props.leftProfile);

    return (
      <table
        className={classNames('data fixed zebra quality-profile-compare-right-table', {
          'has-first-column': renderFirstColumn,
        })}
      >
        <thead>
          <tr>
            {renderFirstColumn && <th aria-label={translate('actions')}>&nbsp;</th>}
            <th>
              {translateWithParameters(
                'quality_profiles.x_rules_only_in',
                this.props.inRight.length
              )}{' '}
              {this.props.right.name}
            </th>
          </tr>
        </thead>
        <tbody>
          {this.props.inRight.map((rule) => (
            <tr key={`right-${rule.key}`}>
              {renderFirstColumn && (
                <td className="text-right">
                  <ComparisonResultActivation
                    key={rule.key}
                    onDone={this.props.refresh}
                    profile={this.props.leftProfile}
                    ruleKey={rule.key}
                  >
                    <ChevronLeftIcon />
                  </ComparisonResultActivation>
                </td>
              )}
              <td>{this.renderRule(rule, rule.severity)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    );
  }

  renderModified() {
    if (this.props.modified.length === 0) {
      return null;
    }
    return (
      <table className="data fixed zebra zebra-inversed">
        <caption>
          {translateWithParameters(
            'quality_profiles.x_rules_have_different_configuration',
            this.props.modified.length
          )}
        </caption>
        <thead>
          <tr>
            <th>{this.props.left.name}</th>
            <th>{this.props.right.name}</th>
          </tr>
        </thead>
        <tbody>
          {this.props.modified.map((rule) => (
            <tr key={`modified-${rule.key}`}>
              <td>
                {this.renderRule(rule, rule.left.severity)}
                {this.renderParameters(rule.left.params)}
              </td>
              <td>
                {this.renderRule(rule, rule.right.severity)}
                {this.renderParameters(rule.right.params)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    );
  }

  render() {
    if (!this.props.inLeft.length && !this.props.inRight.length && !this.props.modified.length) {
      return <div className="big-spacer-top">{translate('quality_profile.empty_comparison')}</div>;
    }

    return (
      <>
        {this.renderLeft()}
        {this.renderRight()}
        {this.renderModified()}
      </>
    );
  }
}
