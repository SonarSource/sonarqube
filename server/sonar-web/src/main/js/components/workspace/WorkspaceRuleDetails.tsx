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
import { keyBy } from 'lodash';
import { getRuleDetails, getRulesApp } from '../../api/rules';
import { RuleDetails } from '../../app/types';
import DeferredSpinner from '../common/DeferredSpinner';
import RuleDetailsMeta from '../../apps/coding-rules/components/RuleDetailsMeta';
import RuleDetailsDescription from '../../apps/coding-rules/components/RuleDetailsDescription';
import '../../apps/coding-rules/styles.css';

interface Props {
  onLoad: (details: { name: string }) => void;
  organization: string | undefined;
  ruleKey: string;
}

interface State {
  loading: boolean;
  referencedRepositories: { [repository: string]: { key: string; language: string; name: string } };
  ruleDetails?: RuleDetails;
}

export default class WorkspaceRuleDetails extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { loading: true, referencedRepositories: {} };

  componentDidMount() {
    this.mounted = true;
    this.fetchRuleDetails();
  }

  componentDidUpdate(prevProps: Props) {
    if (
      prevProps.ruleKey !== this.props.ruleKey ||
      prevProps.organization !== this.props.organization
    ) {
      this.fetchRuleDetails();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  fetchRuleDetails = () => {
    this.setState({ loading: true });
    Promise.all([
      getRulesApp({ organization: this.props.organization }),
      getRuleDetails({ key: this.props.ruleKey, organization: this.props.organization })
    ]).then(
      ([{ repositories }, { rule }]) => {
        if (this.mounted) {
          this.setState({
            loading: false,
            referencedRepositories: keyBy(repositories, 'key'),
            ruleDetails: rule
          });
          this.props.onLoad({ name: rule.name });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ loading: false });
        }
      }
    );
  };

  noOp = () => {};

  render() {
    return (
      <DeferredSpinner loading={this.state.loading}>
        {this.state.ruleDetails && (
          <>
            <RuleDetailsMeta
              canWrite={false}
              hideSimilarRulesFilter={true}
              onFilterChange={this.noOp}
              onTagsChange={this.noOp}
              organization={this.props.organization}
              referencedRepositories={this.state.referencedRepositories}
              ruleDetails={this.state.ruleDetails}
            />
            <RuleDetailsDescription
              canWrite={false}
              onChange={this.noOp}
              organization={this.props.organization}
              ruleDetails={this.state.ruleDetails}
            />
          </>
        )}
      </DeferredSpinner>
    );
  }
}
