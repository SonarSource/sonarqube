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

import styled from '@emotion/styled';
import classNames from 'classnames';
import { throttle } from 'lodash';
import React, { AriaRole } from 'react';
import { findDOMNode } from 'react-dom';
import tw from 'twin.macro';
import { THROTTLE_SCROLL_DELAY } from '../helpers/constants';
import { PopupPlacement, PopupZLevel, popupPositioning } from '../helpers/positioning';
import { themeBorder, themeColor, themeContrast, themeShadow } from '../helpers/theme';
import ClickEventBoundary from './ClickEventBoundary';

interface PopupBaseProps {
  'aria-labelledby'?: string;
  children?: React.ReactNode;
  className?: string;
  id?: string;
  placement?: PopupPlacement;
  role?: AriaRole;
  style?: React.CSSProperties;
  zLevel?: PopupZLevel;
}

function PopupBase(props: PopupBaseProps, ref: React.Ref<HTMLDivElement>) {
  const {
    children,
    className,
    placement = PopupPlacement.Bottom,
    style,
    zLevel = PopupZLevel.Default,
    ...ariaProps
  } = props;
  return (
    <ClickEventBoundary>
      <PopupWrapper
        className={classNames(`is-${placement}`, className)}
        ref={ref ?? React.createRef()}
        style={style}
        zLevel={zLevel}
        {...ariaProps}
      >
        {children}
      </PopupWrapper>
    </ClickEventBoundary>
  );
}

const PopupWithRef = React.forwardRef(PopupBase);
PopupWithRef.displayName = 'Popup';

interface PopupProps extends Omit<PopupBaseProps, 'style'> {
  allowResizing?: boolean;
  children: React.ReactNode;
  overlay: React.ReactNode;
}

interface Measurements {
  height: number;
  left: number;
  top: number;
  width: number;
}

type State = Partial<Measurements>;

function isMeasured(state: State): state is Measurements {
  return state.height !== undefined;
}

export class Popup extends React.PureComponent<PopupProps, State> {
  mounted = false;
  popupNode = React.createRef<HTMLDivElement>();
  throttledPositionTooltip: () => void;

  constructor(props: PopupProps) {
    super(props);
    this.state = {};
    this.throttledPositionTooltip = throttle(this.positionPopup, THROTTLE_SCROLL_DELAY);
  }

  componentDidMount() {
    this.positionPopup();
    this.addEventListeners();
    this.mounted = true;
  }

  componentDidUpdate(prevProps: PopupProps) {
    if (this.props.placement !== prevProps.placement || this.props.overlay !== prevProps.overlay) {
      this.positionPopup();
    }
  }

  componentWillUnmount() {
    this.removeEventListeners();
    this.mounted = false;
  }

  addEventListeners = () => {
    window.addEventListener('resize', this.throttledPositionTooltip);
    if (this.props.zLevel !== PopupZLevel.Global) {
      window.addEventListener('scroll', this.throttledPositionTooltip);
    }
  };

  removeEventListeners = () => {
    window.removeEventListener('resize', this.throttledPositionTooltip);
    if (this.props.zLevel !== PopupZLevel.Global) {
      window.removeEventListener('scroll', this.throttledPositionTooltip);
    }
  };

  positionPopup = () => {
    if (this.mounted && this.props.zLevel !== PopupZLevel.Absolute) {
      // `findDOMNode(this)` will search for the DOM node for the current component
      // first it will find a React.Fragment (see `render`),
      // so it will get the DOM node of the first child, i.e. DOM node of `this.props.children`
      // docs: https://reactjs.org/docs/refs-and-the-dom.html#exposing-dom-refs-to-parent-components

      // eslint-disable-next-line react/no-find-dom-node
      const toggleNode = findDOMNode(this);
      if (toggleNode && toggleNode instanceof Element && this.popupNode.current) {
        const { placement, zLevel } = this.props;
        const isGlobal = zLevel === PopupZLevel.Global;
        const { height, left, top, width } = popupPositioning(
          toggleNode,
          this.popupNode.current,
          placement,
        );

        // save width and height (and later set in `render`) to avoid resizing the popup element,
        // when it's placed close to the window edge
        this.setState({
          left: left + (isGlobal ? 0 : window.scrollX),
          top: top + (isGlobal ? 0 : window.scrollY),
          width,
          height,
        });
      }
    }
  };

  render() {
    const {
      allowResizing,
      children,
      overlay,
      placement = PopupPlacement.Bottom,
      ...popupProps
    } = this.props;

    let style: React.CSSProperties | undefined;
    if (isMeasured(this.state)) {
      style = { left: this.state.left, top: this.state.top };
      if (!allowResizing) {
        style.width = this.state.width;
        style.height = this.state.height;
      }
    }
    return (
      <>
        {this.props.children}
        {this.props.overlay && (
          <PopupWithRef placement={placement} ref={this.popupNode} style={style} {...popupProps}>
            {overlay}
          </PopupWithRef>
        )}
      </>
    );
  }
}

export const PopupWrapper = styled.div<{ zLevel: PopupZLevel }>`
  position: ${({ zLevel }) => (zLevel === PopupZLevel.Global ? 'fixed' : 'absolute')};
  background-color: ${themeColor('popup')};
  color: ${themeContrast('popup')};
  border: ${themeBorder('default', 'popupBorder')};
  box-shadow: ${themeShadow('md')};

  ${tw`sw-box-border`};
  ${tw`sw-rounded-2`};
  ${tw`sw-cursor-default`};
  ${tw`sw-overflow-hidden`};
  ${({ zLevel }) =>
    ({
      [PopupZLevel.Default]: tw`sw-z-popup`,
      [PopupZLevel.Global]: tw`sw-z-global-popup`,
      [PopupZLevel.Content]: tw`sw-z-content-popup`,
      [PopupZLevel.Absolute]: tw`sw-z-global-popup`,
    })[zLevel]};

  &.is-bottom,
  &.is-bottom-left,
  &.is-bottom-right {
    ${tw`sw-mt-2`};
  }

  &.is-top,
  &.is-top-left,
  &.is-top-right {
    ${tw`sw--mt-2`};
  }

  &.is-left,
  &.is-left-top,
  &.is-left-bottom {
    ${tw`sw--ml-2`};
  }

  &.is-right,
  &.is-right-top,
  &.is-right-bottom {
    ${tw`sw-ml-2`};
  }
`;
