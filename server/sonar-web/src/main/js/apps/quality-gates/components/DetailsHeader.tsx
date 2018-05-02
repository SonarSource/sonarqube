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
import BuiltInQualityGateBadge from './BuiltInQualityGateBadge';
import RenameQualityGateForm from './RenameQualityGateForm';
import CopyQualityGateForm from './CopyQualityGateForm';
import DeleteQualityGateForm from './DeleteQualityGateForm';
import { fetchQualityGate, QualityGate, setQualityGateAsDefault } from '../../../api/quality-gates';
import { Button } from '../../../components/ui/buttons';
import { translate } from '../../../helpers/l10n';

interface Props {
  qualityGate: QualityGate;
  onRename: (qualityGate: QualityGate, newName: string) => void;
  onCopy: (newQualityGate: QualityGate) => void;
  onSetAsDefault: (qualityGate: QualityGate) => void;
  onDelete: (qualityGate: QualityGate) => void;
  organization?: string;
}

interface State {
  openPopup?: string;
}

export default class DetailsHeader extends React.PureComponent<Props, State> {
  state = { openPopup: undefined };

  handleRenameClick = () => {
    this.setState({ openPopup: 'rename' });
  };

  handleCopyClick = () => {
    this.setState({ openPopup: 'copy' });
  };

  handleSetAsDefaultClick = () => {
    const { qualityGate, onSetAsDefault, organization } = this.props;
    if (!qualityGate.isDefault) {
      setQualityGateAsDefault({ id: qualityGate.id, organization })
        .then(() => fetchQualityGate({ id: qualityGate.id, organization }))
        .then(qualityGate => onSetAsDefault(qualityGate), () => {});
    }
  };

  handleDeleteClick = () => {
    this.setState({ openPopup: 'delete' });
  };

  handleClosePopup = () => this.setState({ openPopup: undefined });

  render() {
    const { organization, qualityGate } = this.props;
    const { openPopup } = this.state;
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
                <Button id="quality-gate-rename" onClick={this.handleRenameClick}>
                  {translate('rename')}
                </Button>
              )}
              {actions.copy && (
                <Button
                  className="little-spacer-left"
                  id="quality-gate-copy"
                  onClick={this.handleCopyClick}>
                  {translate('copy')}
                </Button>
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
                <Button
                  className="little-spacer-left button-red"
                  id="quality-gate-delete"
                  onClick={this.handleDeleteClick}>
                  {translate('delete')}
                </Button>
              )}
              {openPopup === 'rename' && (
                <RenameQualityGateForm
                  onClose={this.handleClosePopup}
                  onRename={this.props.onRename}
                  organization={organization}
                  qualityGate={qualityGate}
                />
              )}

              {openPopup === 'copy' && (
                <CopyQualityGateForm
                  onClose={this.handleClosePopup}
                  onCopy={this.props.onCopy}
                  organization={organization}
                  qualityGate={qualityGate}
                />
              )}

              {openPopup === 'delete' && (
                <DeleteQualityGateForm
                  onClose={this.handleClosePopup}
                  onDelete={this.props.onDelete}
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
