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
import { setQualityGateAsDefault } from '../../../api/quality-gates';
import { Button } from '../../../components/controls/buttons';
import ModalButton from '../../../components/controls/ModalButton';
import Tooltip from '../../../components/controls/Tooltip';
import AlertWarnIcon from '../../../components/icons/AlertWarnIcon';
import { translate } from '../../../helpers/l10n';
import { CaycStatus, QualityGate } from '../../../types/types';
import BuiltInQualityGateBadge from './BuiltInQualityGateBadge';
import CaycBadgeTooltip from './CaycBadgeTooltip';
import CopyQualityGateForm from './CopyQualityGateForm';
import DeleteQualityGateForm from './DeleteQualityGateForm';
import RenameQualityGateForm from './RenameQualityGateForm';

interface Props {
  onSetDefault: () => void;
  qualityGate: QualityGate;
  refreshItem: () => Promise<void>;
  refreshList: () => Promise<void>;
}

const TOOLTIP_MOUSE_LEAVE_DELAY = 0.3;

export default class DetailsHeader extends React.PureComponent<Props> {
  handleActionRefresh = () => {
    const { refreshItem, refreshList } = this.props;
    return Promise.all([refreshItem(), refreshList()]).then(
      () => {},
      () => {}
    );
  };

  handleSetAsDefaultClick = () => {
    const { qualityGate } = this.props;
    if (!qualityGate.isDefault) {
      // Optimistic update
      this.props.onSetDefault();
      setQualityGateAsDefault({ id: qualityGate.id }).then(
        this.handleActionRefresh,
        this.handleActionRefresh
      );
    }
  };

  render() {
    const { qualityGate } = this.props;
    const actions = qualityGate.actions || ({} as any);

    return (
      <div className="layout-page-header-panel layout-page-main-header issues-main-header">
        <div className="layout-page-header-panel-inner layout-page-main-header-inner">
          <div className="layout-page-main-inner">
            <div className="pull-left display-flex-center">
              <h2>{qualityGate.name}</h2>
              {qualityGate.isBuiltIn && <BuiltInQualityGateBadge className="spacer-left" />}
              {qualityGate.caycStatus === CaycStatus.NonCompliant && (
                <Tooltip overlay={<CaycBadgeTooltip />} mouseLeaveDelay={TOOLTIP_MOUSE_LEAVE_DELAY}>
                  <AlertWarnIcon className="spacer-left" />
                </Tooltip>
              )}
            </div>

            <div className="pull-right">
              {actions.rename && (
                <ModalButton
                  modal={({ onClose }) => (
                    <RenameQualityGateForm
                      onClose={onClose}
                      onRename={this.handleActionRefresh}
                      qualityGate={qualityGate}
                    />
                  )}
                >
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
                      qualityGate={qualityGate}
                    />
                  )}
                >
                  {({ onClick }) => (
                    <Tooltip
                      overlay={
                        qualityGate.caycStatus === CaycStatus.NonCompliant
                          ? translate('quality_gates.cannot_copy_no_cayc')
                          : null
                      }
                      accessible={false}
                    >
                      <Button
                        className="little-spacer-left"
                        id="quality-gate-copy"
                        onClick={onClick}
                        disabled={qualityGate.caycStatus === CaycStatus.NonCompliant}
                      >
                        {translate('copy')}
                      </Button>
                    </Tooltip>
                  )}
                </ModalButton>
              )}
              {actions.setAsDefault && (
                <Tooltip
                  overlay={
                    qualityGate.caycStatus === CaycStatus.NonCompliant
                      ? translate('quality_gates.cannot_set_default_no_cayc')
                      : null
                  }
                  accessible={false}
                >
                  <Button
                    className="little-spacer-left"
                    disabled={qualityGate.caycStatus === CaycStatus.NonCompliant}
                    id="quality-gate-toggle-default"
                    onClick={this.handleSetAsDefaultClick}
                  >
                    {translate('set_as_default')}
                  </Button>
                </Tooltip>
              )}
              {actions.delete && (
                <DeleteQualityGateForm
                  onDelete={this.props.refreshList}
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
