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
import * as React from 'react';
import {
  getProjectBadgesToken,
  renewProjectBadgesToken,
} from '../../../../../../api/project-badges';
import CodeSnippet from '../../../../../../components/common/CodeSnippet';
import { Button } from '../../../../../../components/controls/buttons';
import { Alert } from '../../../../../../components/ui/Alert';
import DeferredSpinner from '../../../../../../components/ui/DeferredSpinner';
import { getBranchLikeQuery } from '../../../../../../helpers/branch-like';
import { translate } from '../../../../../../helpers/l10n';
import { BranchLike } from '../../../../../../types/branch-like';
import { MetricKey } from '../../../../../../types/metrics';
import { Component } from '../../../../../../types/types';
import BadgeButton from './BadgeButton';
import BadgeParams from './BadgeParams';
import './styles.css';
import { BadgeOptions, BadgeType, getBadgeSnippet, getBadgeUrl } from './utils';

interface Props {
  branchLike?: BranchLike;
  component: Component;
}

interface State {
  isRenewing: boolean;
  token: string;
  selectedType: BadgeType;
  badgeOptions: BadgeOptions;
}

export default class ProjectBadges extends React.PureComponent<Props, State> {
  mounted = false;
  headingNodeRef = React.createRef<HTMLHeadingElement>();
  state: State = {
    isRenewing: false,
    token: '',
    selectedType: BadgeType.measure,
    badgeOptions: { metric: MetricKey.alert_status },
  };

  componentDidMount() {
    this.mounted = true;
    this.fetchToken();
    if (this.headingNodeRef.current) {
      this.headingNodeRef.current.focus();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  async fetchToken() {
    const {
      component: { key },
    } = this.props;
    const token = await getProjectBadgesToken(key).catch(() => '');
    if (this.mounted) {
      this.setState({ token });
    }
  }

  handleSelectBadge = (selectedType: BadgeType) => {
    this.setState({ selectedType });
  };

  handleUpdateOptions = (options: Partial<BadgeOptions>) => {
    this.setState((state) => ({
      badgeOptions: { ...state.badgeOptions, ...options },
    }));
  };

  handleRenew = async () => {
    const {
      component: { key },
    } = this.props;

    this.setState({ isRenewing: true });
    await renewProjectBadgesToken(key).catch(() => {});
    await this.fetchToken();
    if (this.mounted) {
      this.setState({ isRenewing: false });
    }
  };

  render() {
    const {
      branchLike,
      component: { key: project, qualifier, configuration },
    } = this.props;
    const { isRenewing, selectedType, badgeOptions, token } = this.state;
    const fullBadgeOptions = {
      project,
      ...badgeOptions,
      ...getBranchLikeQuery(branchLike),
    };
    const canRenew = configuration?.showSettings;

    return (
      <div className="display-flex-column">
        <h3 tabIndex={-1} ref={this.headingNodeRef}>
          {translate('overview.badges.get_badge', qualifier)}
        </h3>
        <p className="big-spacer-bottom">{translate('overview.badges.description', qualifier)}</p>
        <BadgeButton
          onClick={this.handleSelectBadge}
          selected={BadgeType.measure === selectedType}
          type={BadgeType.measure}
          url={getBadgeUrl(BadgeType.measure, fullBadgeOptions, token)}
        />
        <p className="huge-spacer-bottom spacer-top">
          {translate('overview.badges', BadgeType.measure, 'description', qualifier)}
        </p>
        <BadgeButton
          onClick={this.handleSelectBadge}
          selected={BadgeType.qualityGate === selectedType}
          type={BadgeType.qualityGate}
          url={getBadgeUrl(BadgeType.qualityGate, fullBadgeOptions, token)}
        />
        <p className="huge-spacer-bottom spacer-top">
          {translate('overview.badges', BadgeType.qualityGate, 'description', qualifier)}
        </p>
        <BadgeParams
          className="big-spacer-bottom display-flex-column"
          options={badgeOptions}
          type={selectedType}
          updateOptions={this.handleUpdateOptions}
        />
        {isRenewing ? (
          <div className="spacer-top spacer-bottom display-flex-row display-flex-justify-center">
            <DeferredSpinner className="spacer-top spacer-bottom" loading={isRenewing} />
          </div>
        ) : (
          <CodeSnippet
            isOneLine={true}
            snippet={getBadgeSnippet(selectedType, fullBadgeOptions, token)}
          />
        )}

        <Alert variant="warning">
          <p>
            {translate('overview.badges.leak_warning')}{' '}
            {canRenew && translate('overview.badges.renew.description')}
          </p>
          {canRenew && (
            <Button
              disabled={isRenewing}
              className="spacer-top it__project-info-renew-badge"
              onClick={this.handleRenew}
            >
              {translate('overview.badges.renew')}
            </Button>
          )}
        </Alert>
      </div>
    );
  }
}
