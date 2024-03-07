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
import { IconProps, InteractiveIcon } from 'design-system';
import * as React from 'react';
import { DraggableCore, DraggableData } from 'react-draggable';
import ClearIcon from '../../components/icons/ClearIcon';
import CollapseIcon from '../../components/icons/CollapseIcon';
import ExpandIcon from '../../components/icons/ExpandIcon';
import MinimizeIcon from '../../components/icons/MinimizeIcon';
import { translate } from '../../helpers/l10n';
import Tooltip from '../controls/Tooltip';

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
            tooltipContent="workspace.minimize"
          />

          {this.props.maximized ? (
            <WorkspaceHeaderButton
              icon={CollapseIcon}
              onClick={this.props.onMinimize}
              tooltipContent="workspace.normal_size"
            />
          ) : (
            <WorkspaceHeaderButton
              icon={ExpandIcon}
              onClick={this.props.onMaximize}
              tooltipContent="workspace.full_window"
            />
          )}

          <WorkspaceHeaderButton
            icon={ClearIcon}
            onClick={this.props.onClose}
            tooltipContent="workspace.close"
          />
        </div>
      </header>
    );
  }
}

interface WorkspaceHeaderButtonProps {
  icon: React.ComponentType<React.PropsWithChildren<IconProps>>;
  onClick: () => void;
  tooltipContent: string;
}

function WorkspaceHeaderButton({ icon, onClick, tooltipContent }: WorkspaceHeaderButtonProps) {
  return (
    <Tooltip overlay={translate(tooltipContent)}>
      <InteractiveIcon
        className="workspace-header-icon"
        aria-label={translate(tooltipContent)}
        Icon={icon}
        currentColor
        onClick={onClick}
        size="small"
      />
    </Tooltip>
  );
}
