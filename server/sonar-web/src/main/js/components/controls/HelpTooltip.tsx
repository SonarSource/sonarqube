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
import classNames from 'classnames';
import * as React from 'react';
import { colors } from '../../app/theme';
import { translate } from '../../helpers/l10n';
import HelpIcon from '../icons/HelpIcon';
import { IconProps } from '../icons/Icon';
import './HelpTooltip.css';
import Tooltip, { Placement } from './Tooltip';

interface Props extends Pick<IconProps, 'size'> {
  className?: string;
  children?: React.ReactNode;
  onShow?: () => void;
  onHide?: () => void;
  overlay: React.ReactNode;
  placement?: Placement;
  isInteractive?: boolean;
  innerRef?: React.Ref<HTMLSpanElement>;
}

const DEFAULT_SIZE = 12;

export default function HelpTooltip(props: Props) {
  const { size = DEFAULT_SIZE, overlay, placement, isInteractive, innerRef, children } = props;
  return (
    <div className={classNames('help-tooltip', props.className)}>
      <Tooltip
        mouseLeaveDelay={0.25}
        onShow={props.onShow}
        onHide={props.onHide}
        overlay={overlay}
        placement={placement}
        isInteractive={isInteractive}
      >
        <span
          className="display-inline-flex-center"
          data-testid="help-tooltip-activator"
          ref={innerRef}
        >
          {children ?? (
            <HelpIcon
              fill={colors.gray60}
              size={size}
              role="img"
              aria-label={isInteractive ? translate('tooltip_is_interactive') : translate('help')}
              description={
                isInteractive ? (
                  <>
                    {translate('tooltip_is_interactive')}
                    {overlay}
                  </>
                ) : (
                  overlay
                )
              }
            />
          )}
        </span>
      </Tooltip>
    </div>
  );
}

export function DarkHelpTooltip({ size = DEFAULT_SIZE, ...props }: Omit<Props, 'children'>) {
  return (
    <HelpTooltip {...props}>
      <HelpIcon fill={colors.transparentBlack} fillInner={colors.white} size={size} />
    </HelpTooltip>
  );
}
