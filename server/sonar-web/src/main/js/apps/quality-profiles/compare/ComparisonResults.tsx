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
import ComparisonEmpty from './ComparisonEmpty';
import SeverityIcon from '../../../components/shared/SeverityIcon';
import { translateWithParameters } from '../../../helpers/l10n';
import { getRulesUrl } from '../../../helpers/urls';

type Params = { [p: string]: string };

interface Props {
  left: { name: string };
  right: { name: string };
  inLeft: Array<{ key: string; name: string; severity: string }>;
  inRight: Array<{ key: string; name: string; severity: string }>;
  modified: Array<{
    key: string;
    name: string;
    left: { params: Params; severity: string };
    right: { params: Params; severity: string };
  }>;
  organization: string | null;
}

export default class ComparisonResults extends React.PureComponent<Props> {
  renderRule(rule: { key: string; name: string }, severity: string) {
    return (
      <div>
        <SeverityIcon severity={severity} />{' '}
        <Link to={getRulesUrl({ rule_key: rule.key }, this.props.organization)}>{rule.name}</Link>
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
          <li key={key} className="spacer-top">
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
          <tr key={`left-${rule.key}`} className="js-comparison-in-left">
            <td>{this.renderRule(rule, rule.severity)}</td>
            <td>&nbsp;</td>
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
          <tr key={`right-${rule.key}`} className="js-comparison-in-right">
            <td>&nbsp;</td>
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
          <td colSpan={2} className="text-center">
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
          <tr key={`modified-${rule.key}`} className="js-comparison-modified">
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
