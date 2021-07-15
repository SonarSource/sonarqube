/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import * as classNames from 'classnames';
import * as React from 'react';
import HelpIcon from '../icons/HelpIcon';
import { IconProps } from '../icons/Icon';
import { ThemeConsumer } from '../theme';
import './HelpTooltip.css';
import Tooltip, { Placement } from './Tooltip';

interface Props extends Pick<IconProps, 'size'> {
  className?: string;
  children?: React.ReactNode;
  onShow?: () => void;
  overlay: React.ReactNode;
  placement?: Placement;
}

export default function HelpTooltip({ size = 12, ...props }: Props) {
  return (
    <div className={classNames('help-tooltip', props.className)}>
      <Tooltip
        mouseLeaveDelay={0.25}
        onShow={props.onShow}
        overlay={props.overlay}
        placement={props.placement}>
        <span className="display-inline-flex-center">
          {props.children || (
            <ThemeConsumer>
              {(theme) => <HelpIcon fill={theme.colors.gray71} size={size} />}
            </ThemeConsumer>
          )}
        </span>
      </Tooltip>
    </div>
  );
}

export function DarkHelpTooltip({ size = 12, ...props }: Omit<Props, 'children'>) {
  return (
    <HelpTooltip {...props}>
      <ThemeConsumer>
        {({ colors }) => (
          <HelpIcon fill={colors.transparentBlack} fillInner={colors.white} size={size} />
        )}
      </ThemeConsumer>
    </HelpTooltip>
  );
}
