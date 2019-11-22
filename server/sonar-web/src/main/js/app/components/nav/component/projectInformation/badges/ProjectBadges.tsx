/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { translate } from 'sonar-ui-common/helpers/l10n';
import CodeSnippet from '../../../../../../components/common/CodeSnippet';
import { getBranchLikeQuery } from '../../../../../../helpers/branch-like';
import { BranchLike } from '../../../../../../types/branch-like';
import { MetricKey } from '../../../../../../types/metrics';
import BadgeButton from './BadgeButton';
import BadgeParams from './BadgeParams';
import './styles.css';
import { BadgeOptions, BadgeType, getBadgeSnippet, getBadgeUrl } from './utils';

interface Props {
  branchLike?: BranchLike;
  metrics: T.Dict<T.Metric>;
  project: string;
  qualifier: string;
}

interface State {
  selectedType: BadgeType;
  badgeOptions: BadgeOptions;
}

export default class ProjectBadges extends React.PureComponent<Props, State> {
  state: State = {
    selectedType: BadgeType.measure,
    badgeOptions: { color: 'white', metric: MetricKey.alert_status }
  };

  handleSelectBadge = (selectedType: BadgeType) => {
    this.setState({ selectedType });
  };

  handleUpdateOptions = (options: Partial<BadgeOptions>) => {
    this.setState(state => ({ badgeOptions: { ...state.badgeOptions, ...options } }));
  };

  render() {
    const { branchLike, project, qualifier } = this.props;
    const { selectedType, badgeOptions } = this.state;
    const fullBadgeOptions = { project, ...badgeOptions, ...getBranchLikeQuery(branchLike) };

    return (
      <div className="display-flex-column">
        <h3>{translate('overview.badges.get_badge', qualifier)}</h3>
        <p className="big-spacer-bottom">{translate('overview.badges.description', qualifier)}</p>
        <BadgeButton
          onClick={this.handleSelectBadge}
          selected={BadgeType.measure === selectedType}
          type={BadgeType.measure}
          url={getBadgeUrl(BadgeType.measure, fullBadgeOptions)}
        />
        <p className="huge-spacer-bottom spacer-top">
          {translate('overview.badges', BadgeType.measure, 'description', qualifier)}
        </p>
        <BadgeButton
          onClick={this.handleSelectBadge}
          selected={BadgeType.qualityGate === selectedType}
          type={BadgeType.qualityGate}
          url={getBadgeUrl(BadgeType.qualityGate, fullBadgeOptions)}
        />
        <p className="huge-spacer-bottom spacer-top">
          {translate('overview.badges', BadgeType.qualityGate, 'description', qualifier)}
        </p>
        <BadgeParams
          className="big-spacer-bottom display-flex-column"
          metrics={this.props.metrics}
          options={badgeOptions}
          type={selectedType}
          updateOptions={this.handleUpdateOptions}
        />
        <CodeSnippet isOneLine={true} snippet={getBadgeSnippet(selectedType, fullBadgeOptions)} />
      </div>
    );
  }
}
