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
import * as React from 'react';
import { Profile } from '../../../api/quality-profiles';
import { getRuleDetails } from '../../../api/rules';
import { Button } from '../../../components/controls/buttons';
import Tooltip from '../../../components/controls/Tooltip';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { RuleDetails } from '../../../types/types';
import ActivationFormModal from '../../coding-rules/components/ActivationFormModal';

interface Props {
  onDone: () => Promise<void>;
  organization?: string;
  profile: Profile;
  ruleKey: string;
}

interface State {
  rule?: RuleDetails;
  state: 'closed' | 'opening' | 'open';
}

export default class ComparisonResultActivation extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { state: 'closed' };

  componentDidMount() {
    this.mounted = true;
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  handleButtonClick = () => {
    this.setState({ state: 'opening' });
    getRuleDetails({ key: this.props.ruleKey, organization: this.props.organization }).then(
      ({ rule }) => {
        if (this.mounted) {
          this.setState({ rule, state: 'open' });
        }
      },
      () => {
        if (this.mounted) {
          this.setState({ state: 'closed' });
        }
      }
    );
  };

  handleCloseModal = () => {
    this.setState({ state: 'closed' });
  };

  isOpen(state: State): state is { state: 'open'; rule: RuleDetails } {
    return state.state === 'open';
  }

  render() {
    const { profile } = this.props;

    const canActivate = !profile.isBuiltIn && profile.actions && profile.actions.edit;
    if (!canActivate) {
      return null;
    }

    return (
      <DeferredSpinner loading={this.state.state === 'opening'}>
        <Tooltip
          placement="bottom"
          overlay={translateWithParameters(
            'quality_profiles.comparison.activate_rule',
            profile.name
          )}
        >
          <Button
            disabled={this.state.state !== 'closed'}
            aria-label={translateWithParameters(
              'quality_profiles.comparison.activate_rule',
              profile.name
            )}
            onClick={this.handleButtonClick}
          >
            {this.props.children}
          </Button>
        </Tooltip>

        {this.isOpen(this.state) && (
          <ActivationFormModal
            modalHeader={translate('coding_rules.activate_in_quality_profile')}
            onClose={this.handleCloseModal}
            onDone={this.props.onDone}
            profiles={[profile]}
            rule={this.state.rule}
          />
        )}
      </DeferredSpinner>
    );
  }
}
