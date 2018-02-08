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
import BadgeButton from './BadgeButton';
import BadgeSnippet from './BadgeSnippet';
import BadgeParams from './BadgeParams';
import { BadgeType, BadgeOptions, getBadgeUrl } from './utils';
import { Metric } from '../../../app/types';
import Modal from '../../../components/controls/Modal';
import { translate } from '../../../helpers/l10n';
import './styles.css';

interface Props {
  branch?: string;
  metrics: { [key: string]: Metric };
  project: string;
}

interface State {
  open: boolean;
  selectedType: BadgeType;
  badgeOptions: BadgeOptions;
}

export default class BadgesModal extends React.PureComponent<Props, State> {
  state: State = {
    open: false,
    selectedType: BadgeType.measure,
    badgeOptions: { color: 'white', metric: 'alert_status' }
  };

  handleClose = () => this.setState({ open: false });

  handleOpen = () => this.setState({ open: true });

  handleSelectBadge = (selectedType: BadgeType) => this.setState({ selectedType });

  handleUpdateOptions = (options: Partial<BadgeOptions>) =>
    this.setState(state => ({
      badgeOptions: { ...state.badgeOptions, ...options }
    }));

  handleCancelClick = () => this.handleClose();

  render() {
    const { branch, project } = this.props;
    const { selectedType, badgeOptions } = this.state;
    const header = translate('overview.badges.title');
    const fullBadgeOptions = { branch, project, ...badgeOptions };
    return (
      <div className="overview-meta-card">
        <button className="js-project-badges" onClick={this.handleOpen}>
          {translate('overview.badges.get_badge')}
        </button>
        {this.state.open && (
          <Modal contentLabel={header} onRequestClose={this.handleClose}>
            <header className="modal-head">
              <h2>{header}</h2>
            </header>
            <div className="modal-body">
              <p className="huge-spacer-bottom">{translate('overview.badges.description')}</p>
              <div className="badges-list spacer-bottom">
                {[BadgeType.measure, BadgeType.qualityGate, BadgeType.marketing].map(type => (
                  <BadgeButton
                    key={type}
                    onClick={this.handleSelectBadge}
                    selected={type === selectedType}
                    type={type}
                    url={getBadgeUrl(type, fullBadgeOptions)}
                  />
                ))}
              </div>
              <p className="text-center note huge-spacer-bottom">
                {translate('overview.badges', selectedType, 'description')}
              </p>
              <BadgeParams
                className="big-spacer-bottom"
                metrics={this.props.metrics}
                options={badgeOptions}
                type={selectedType}
                updateOptions={this.handleUpdateOptions}
              />
              <BadgeSnippet snippet={getBadgeUrl(selectedType, fullBadgeOptions)} />
            </div>
            <footer className="modal-foot">
              <button className="button-link js-modal-close" onClick={this.handleCancelClick}>
                {translate('close')}
              </button>
            </footer>
          </Modal>
        )}
      </div>
    );
  }
}
