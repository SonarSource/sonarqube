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
import { DraggableCore, DraggableData } from 'react-draggable';
import { ButtonIcon } from 'sonar-ui-common/components/controls/buttons';
import ClearIcon from 'sonar-ui-common/components/icons/ClearIcon';
import CollapseIcon from 'sonar-ui-common/components/icons/CollapseIcon';
import ExpandIcon from 'sonar-ui-common/components/icons/ExpandIcon';
import { IconProps } from 'sonar-ui-common/components/icons/Icon';
import MinimizeIcon from 'sonar-ui-common/components/icons/MinimizeIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';

export interface Props {
  children: React.ReactNode;
  maximized?: boolean;
  onClose: () => void;
  onCollapse: () => void;
  onMaximize: () => void;
  onMinimize: () => void;
  onResize: (deltaY: number) => void;
}

export default class WorkspaceHeader extends React.PureComponent<Props> {
  handleDrag = (_event: MouseEvent, data: DraggableData) => {
    this.props.onResize(data.deltaY);
  };

  render() {
    return (
      <header className="workspace-viewer-header">
        <h6 className="workspace-viewer-name">{this.props.children}</h6>

        <DraggableCore offsetParent={document.body} onDrag={this.handleDrag}>
          <div className="workspace-viewer-resize js-resize" />
        </DraggableCore>

        <div className="workspace-viewer-actions">
          <WorkspaceHeaderButton
            icon={MinimizeIcon}
            onClick={this.props.onCollapse}
            tooltip="workspace.minimize"
          />

          {this.props.maximized ? (
            <WorkspaceHeaderButton
              icon={CollapseIcon}
              onClick={this.props.onMinimize}
              tooltip="workspace.normal_size"
            />
          ) : (
            <WorkspaceHeaderButton
              icon={ExpandIcon}
              onClick={this.props.onMaximize}
              tooltip="workspace.full_window"
            />
          )}

          <WorkspaceHeaderButton
            icon={ClearIcon}
            onClick={this.props.onClose}
            tooltip="workspace.close"
          />
        </div>
      </header>
    );
  }
}

interface WorkspaceHeaderButtonProps {
  icon: React.SFC<IconProps>;
  onClick: () => void;
  tooltip: string;
}

function WorkspaceHeaderButton({ icon: Icon, onClick, tooltip }: WorkspaceHeaderButtonProps) {
  return (
    <ButtonIcon
      className="workspace-header-icon"
      color="#fff"
      onClick={onClick}
      tooltip={translate(tooltip)}>
      <Icon fill={undefined} />
    </ButtonIcon>
  );
}
