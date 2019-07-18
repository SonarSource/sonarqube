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
import { Button } from 'sonar-ui-common/components/controls/buttons';
import ModalButton from 'sonar-ui-common/components/controls/ModalButton';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { setQualityGateAsDefault } from '../../../api/quality-gates';
import BuiltInQualityGateBadge from './BuiltInQualityGateBadge';
import CopyQualityGateForm from './CopyQualityGateForm';
import DeleteQualityGateForm from './DeleteQualityGateForm';
import RenameQualityGateForm from './RenameQualityGateForm';

interface Props {
  onSetDefault: () => void;
  organization?: string;
  qualityGate: T.QualityGate;
  refreshItem: () => Promise<void>;
  refreshList: () => Promise<void>;
}

export default class DetailsHeader extends React.PureComponent<Props> {
  handleActionRefresh = () => {
    const { refreshItem, refreshList } = this.props;
    return Promise.all([refreshItem(), refreshList()]).then(() => {}, () => {});
  };

  handleSetAsDefaultClick = () => {
    const { organization, qualityGate } = this.props;
    if (!qualityGate.isDefault) {
      // Optimistic update
      this.props.onSetDefault();
      setQualityGateAsDefault({ id: qualityGate.id, organization }).then(
        this.handleActionRefresh,
        this.handleActionRefresh
      );
    }
  };

  render() {
    const { organization, qualityGate } = this.props;
    const actions = qualityGate.actions || ({} as any);
    return (
      <div className="layout-page-header-panel layout-page-main-header issues-main-header">
        <div className="layout-page-header-panel-inner layout-page-main-header-inner">
          <div className="layout-page-main-inner">
            <div className="pull-left display-flex-center">
              <h2>{qualityGate.name}</h2>
              {qualityGate.isBuiltIn && <BuiltInQualityGateBadge className="spacer-left" />}
            </div>

            <div className="pull-right">
              {actions.rename && (
                <ModalButton
                  modal={({ onClose }) => (
                    <RenameQualityGateForm
                      onClose={onClose}
                      onRename={this.handleActionRefresh}
                      organization={organization}
                      qualityGate={qualityGate}
                    />
                  )}>
                  {({ onClick }) => (
                    <Button id="quality-gate-rename" onClick={onClick}>
                      {translate('rename')}
                    </Button>
                  )}
                </ModalButton>
              )}
              {actions.copy && (
                <ModalButton
                  modal={({ onClose }) => (
                    <CopyQualityGateForm
                      onClose={onClose}
                      onCopy={this.handleActionRefresh}
                      organization={organization}
                      qualityGate={qualityGate}
                    />
                  )}>
                  {({ onClick }) => (
                    <Button className="little-spacer-left" id="quality-gate-copy" onClick={onClick}>
                      {translate('copy')}
                    </Button>
                  )}
                </ModalButton>
              )}
              {actions.setAsDefault && (
                <Button
                  className="little-spacer-left"
                  id="quality-gate-toggle-default"
                  onClick={this.handleSetAsDefaultClick}>
                  {translate('set_as_default')}
                </Button>
              )}
              {actions.delete && (
                <DeleteQualityGateForm
                  onDelete={this.props.refreshList}
                  organization={organization}
                  qualityGate={qualityGate}
                />
              )}
            </div>
          </div>
        </div>
      </div>
    );
  }
}
