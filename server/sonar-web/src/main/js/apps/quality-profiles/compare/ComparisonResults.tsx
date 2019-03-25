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
import ComparisonEmpty from './ComparisonEmpty';
import ComparisonResultActivation from './ComparisonResultActivation';
import SeverityIcon from '../../../components/icons-components/SeverityIcon';
import { translateWithParameters } from '../../../helpers/l10n';
import { getRulesUrl } from '../../../helpers/urls';
import { CompareResponse, Profile } from '../../../api/quality-profiles';
import ChevronRightIcon from '../../../components/icons-components/ChevronRightcon';
import ChevronLeftIcon from '../../../components/icons-components/ChevronLeftIcon';

type Params = T.Dict<string>;

interface Props extends CompareResponse {
  organization?: string;
  leftProfile: Profile;
  refresh: () => Promise<void>;
  rightProfile?: Profile;
}

export default class ComparisonResults extends React.PureComponent<Props> {
  renderRule(rule: { key: string; name: string }, severity: string) {
    return (
      <div>
        <SeverityIcon severity={severity} />{' '}
        <Link to={getRulesUrl({ rule_key: rule.key, open: rule.key }, this.props.organization)}>
          {rule.name}
        </Link>
      </div>
    );
  }

  renderParameters(params: Params) {
    if (!params) {
      return null;
    }
    return (
      <ul>
        {Object.keys(params).map(key => (
          <li className="spacer-top" key={key}>
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
    return (
      <>
        <tr>
          <td>
            <h6>
              {translateWithParameters(
                'quality_profiles.x_rules_only_in',
                this.props.inLeft.length
              )}{' '}
              {this.props.left.name}
            </h6>
          </td>
          <td>&nbsp;</td>
        </tr>
        {this.props.inLeft.map(rule => (
          <tr className="js-comparison-in-left" key={`left-${rule.key}`}>
            <td>{this.renderRule(rule, rule.severity)}</td>
            <td>
              {this.props.rightProfile && (
                <ComparisonResultActivation
                  key={rule.key}
                  onDone={this.props.refresh}
                  organization={this.props.organization || undefined}
                  profile={this.props.rightProfile}
                  ruleKey={rule.key}>
                  <ChevronRightIcon />
                </ComparisonResultActivation>
              )}
            </td>
          </tr>
        ))}
      </>
    );
  }

  renderRight() {
    if (this.props.inRight.length === 0) {
      return null;
    }
    return (
      <>
        <tr>
          <td>&nbsp;</td>
          <td>
            <h6>
              {translateWithParameters(
                'quality_profiles.x_rules_only_in',
                this.props.inRight.length
              )}{' '}
              {this.props.right.name}
            </h6>
          </td>
        </tr>
        {this.props.inRight.map(rule => (
          <tr className="js-comparison-in-right" key={`right-${rule.key}`}>
            <td className="text-right">
              <ComparisonResultActivation
                key={rule.key}
                onDone={this.props.refresh}
                organization={this.props.organization || undefined}
                profile={this.props.leftProfile}
                ruleKey={rule.key}>
                <ChevronLeftIcon />
              </ComparisonResultActivation>
            </td>
            <td>{this.renderRule(rule, rule.severity)}</td>
          </tr>
        ))}
      </>
    );
  }

  renderModified() {
    if (this.props.modified.length === 0) {
      return null;
    }
    return (
      <>
        <tr>
          <td className="text-center" colSpan={2}>
            <h6>
              {translateWithParameters(
                'quality_profiles.x_rules_have_different_configuration',
                this.props.modified.length
              )}
            </h6>
          </td>
        </tr>
        <tr>
          <td>
            <h6>{this.props.left.name}</h6>
          </td>
          <td>
            <h6>{this.props.right.name}</h6>
          </td>
        </tr>
        {this.props.modified.map(rule => (
          <tr className="js-comparison-modified" key={`modified-${rule.key}`}>
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
      </>
    );
  }

  render() {
    if (!this.props.inLeft.length && !this.props.inRight.length && !this.props.modified.length) {
      return <ComparisonEmpty />;
    }

    return (
      <table className="data zebra quality-profile-comparison-table">
        <tbody>
          {this.renderLeft()}
          {this.renderRight()}
          {this.renderModified()}
        </tbody>
      </table>
    );
  }
}
