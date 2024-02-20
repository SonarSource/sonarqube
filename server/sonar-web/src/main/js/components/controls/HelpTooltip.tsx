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
import { HelperHintIcon } from 'design-system';
import * as React from 'react';
import { translate } from '../../helpers/l10n';
import Tooltip, { Placement } from './Tooltip';

interface Props {
  className?: string;
  children?: React.ReactNode;
  onShow?: () => void;
  onHide?: () => void;
  overlay: React.ReactNode;
  placement?: Placement;
  isInteractive?: boolean;
  innerRef?: React.Ref<HTMLSpanElement>;
  size?: number;
}

const DEFAULT_SIZE = 12;

export default function HelpTooltip(props: Props) {
  const { size = DEFAULT_SIZE, overlay, placement, isInteractive, innerRef, children } = props;
  return (
    <div
      className={classNames(
        'it__help-tooltip sw-inline-flex sw-items-center sw-align-middle',
        props.className,
      )}
    >
      <Tooltip
        mouseLeaveDelay={0.25}
        onShow={props.onShow}
        onHide={props.onHide}
        overlay={overlay}
        placement={placement}
        isInteractive={isInteractive}
      >
        <span
          className="sw-inline-flex sw-items-center"
          data-testid="help-tooltip-activator"
          ref={innerRef}
        >
          {children ?? (
            <HelperHintIcon
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
              height={size}
              width={size}
            />
          )}
        </span>
      </Tooltip>
    </div>
  );
}
