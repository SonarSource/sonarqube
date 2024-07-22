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
import { keyframes, ThemeContext } from '@emotion/react';
import styled from '@emotion/styled';
import classNames from 'classnames';
import { throttle, uniqueId } from 'lodash';
import React from 'react';
import { createPortal, findDOMNode } from 'react-dom';
import tw from 'twin.macro';
import { THROTTLE_SCROLL_DELAY } from '../helpers/constants';
import {
  BasePlacement,
  PLACEMENT_FLIP_MAP,
  PopupPlacement,
  popupPositioning,
} from '../helpers/positioning';
import { themeColor } from '../helpers/theme';

const MILLISECONDS_IN_A_SECOND = 1000;

interface TooltipProps {
  children: React.ReactElement;
  content: React.ReactNode;
  mouseEnterDelay?: number;
  mouseLeaveDelay?: number;
  onHide?: VoidFunction;
  onShow?: VoidFunction;
  placement?: BasePlacement;
  visible?: boolean;
}

interface Measurements {
  height: number;
  left: number;
  leftFix: number;
  top: number;
  topFix: number;
  width: number;
}

interface OwnState {
  flipped: boolean;
  placement?: PopupPlacement;
  visible: boolean;
}

type State = OwnState & Partial<Measurements>;

function isMeasured(state: State): state is OwnState & Measurements {
  return state.height !== undefined;
}

/**
 * @deprecated Use Tooltip from Echoes instead.
 *
 * Echoes Tooltip component should mainly be used on interactive element and contain very simple text based content.
 * If the content is more complex use a Popover component instead (not available yet).
 *
 * Some of the props have changed or been renamed:
 * - `children` is the trigger for the tooltip, should be an interactive Element. If not an Echoes component, make sure the component forwards the props and the ref to an interactive DOM node, it's needed by the tooltip to position itself.
 * - `overlay` is now `content`, that's the tooltip content. It's a ReactNode for convenience but should render only text based content, no interactivity is allowed inside the tooltip.
 * - ~`mouseEnterDelay`~ doesn't exist anymore, was mostly used in situation that should be replaced by a Popover component.
 * - ~`mouseLeaveDelay`~ doesn't exist anymore, was mostly used in situation that should be replaced by a Popover component.
 * - ~`onHide`~ doesn't exist anymore, was mostly used in situation that should be replaced by a Popover component.
 * - ~`onShow`~ doesn't exist anymore, was mostly used in situation that should be replaced by a Popover component.
 * - `placement` is now `align` and `side`, based on the TooltipAlign and TooltipSide enums.
 * - `visible` is now `isOpen`
 */
export function Tooltip(props: TooltipProps) {
  // overlay is a ReactNode, so it can be a boolean, `undefined` or `null`
  // this allows to easily render a tooltip conditionally
  // more generaly we avoid rendering empty tooltips
  return props.content ? <TooltipInner {...props}>{props.children}</TooltipInner> : props.children;
}

export class TooltipInner extends React.Component<TooltipProps, State> {
  throttledPositionTooltip: VoidFunction;
  mouseEnterTimeout?: number;
  mouseLeaveTimeout?: number;
  tooltipNode?: HTMLElement | null;
  mounted = false;
  mouseIn = false;
  id: string;

  static defaultProps = {
    mouseEnterDelay: 0.1,
  };

  constructor(props: TooltipProps) {
    super(props);

    this.state = {
      flipped: false,
      placement: props.placement,
      visible: props.visible !== undefined ? props.visible : false,
    };
    this.id = uniqueId('tooltip-');
    this.throttledPositionTooltip = throttle(this.positionTooltip, THROTTLE_SCROLL_DELAY);
  }

  componentDidMount() {
    this.mounted = true;

    if (this.props.visible === true) {
      this.positionTooltip();
      this.addEventListeners();
    }
  }

  componentDidUpdate(prevProps: TooltipProps, prevState: State) {
    if (this.props.placement !== prevProps.placement) {
      this.setState({ placement: this.props.placement }, () => {
        this.onUpdatePlacement(this.hasVisibleChanged(prevState.visible, prevProps.visible));
      });
    } else if (this.hasVisibleChanged(prevState.visible, prevProps.visible)) {
      this.onUpdateVisible();
    } else if (!this.state.flipped && this.needsFlipping(this.state)) {
      this.setState(
        ({ placement = PopupPlacement.Bottom }) => ({
          flipped: true,
          placement: PLACEMENT_FLIP_MAP[placement],
        }),
        () => {
          if (this.state.visible) {
            // Force a re-positioning, as "only" updating the state doesn't
            // recompute the position, only re-renders with the previous
            // position (which is no longer correct).
            this.positionTooltip();
          }
        },
      );
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    this.removeEventListeners();
    this.clearTimeouts();
  }

  static contextType = ThemeContext;

  onUpdatePlacement = (visibleHasChanged: boolean) => {
    this.setState({ placement: this.props.placement }, () => {
      if (this.isVisible()) {
        this.positionTooltip();
        if (visibleHasChanged) {
          this.addEventListeners();
        }
      }
    });
  };

  onUpdateVisible = () => {
    if (this.isVisible()) {
      this.positionTooltip();
      this.addEventListeners();
    } else {
      this.clearPosition();
      this.removeEventListeners();
    }
  };

  addEventListeners = () => {
    window.addEventListener('resize', this.throttledPositionTooltip);
    window.addEventListener('scroll', this.throttledPositionTooltip);
  };

  removeEventListeners = () => {
    window.removeEventListener('resize', this.throttledPositionTooltip);
    window.removeEventListener('scroll', this.throttledPositionTooltip);
  };

  clearTimeouts = () => {
    window.clearTimeout(this.mouseEnterTimeout);
    window.clearTimeout(this.mouseLeaveTimeout);
  };

  hasVisibleChanged = (prevStateVisible: boolean, prevPropsVisible?: boolean) => {
    if (this.props.visible === undefined) {
      return prevPropsVisible ?? this.state.visible !== prevStateVisible;
    }

    return this.props.visible !== prevPropsVisible;
  };

  isVisible = () => this.props.visible ?? this.state.visible;

  getPlacement = (): PopupPlacement => this.state.placement ?? PopupPlacement.Bottom;

  tooltipNodeRef = (node: HTMLElement | null) => {
    this.tooltipNode = node;
  };

  adjustArrowPosition = (
    placement: PopupPlacement,
    { leftFix, topFix, height, width }: Measurements,
  ) => {
    switch (placement) {
      case PopupPlacement.Left:
      case PopupPlacement.Right:
        return {
          marginTop: Math.max(0, Math.min(-topFix, height / 2 - ARROW_WIDTH * 2)),
        };
      default:
        return {
          marginLeft: Math.max(0, Math.min(-leftFix, width / 2 - ARROW_WIDTH * 2)),
        };
    }
  };

  positionTooltip = () => {
    // `findDOMNode(this)` will search for the DOM node for the current component
    // first it will find a React.Fragment (see `render`),
    // so it will get the DOM node of the first child, i.e. DOM node of `this.props.children`
    // docs: https://reactjs.org/docs/refs-and-the-dom.html#exposing-dom-refs-to-parent-components

    // eslint-disable-next-line react/no-find-dom-node
    const toggleNode = findDOMNode(this);

    if (toggleNode && toggleNode instanceof Element && this.tooltipNode) {
      const { height, left, leftFix, top, topFix, width } = popupPositioning(
        toggleNode,
        this.tooltipNode,
        this.getPlacement(),
      );

      // save width and height (and later set in `render`) to avoid resizing the popup element,
      // when it's placed close to the window edge
      this.setState({
        left: window.scrollX + left,
        leftFix,
        top: window.scrollY + top,
        topFix,
        width,
        height,
      });
    }
  };

  clearPosition = () => {
    this.setState({
      flipped: false,
      left: undefined,
      leftFix: undefined,
      top: undefined,
      topFix: undefined,
      width: undefined,
      height: undefined,
      placement: this.props.placement,
    });
  };

  handlePointerEnter = () => {
    this.mouseEnterTimeout = window.setTimeout(
      () => {
        // for some reason even after the `this.mouseEnterTimeout` is cleared, it still triggers
        // to workaround this issue, check that its value is not `undefined`
        // (if it's `undefined`, it means the timer has been reset)
        if (
          this.mounted &&
          this.props.visible === undefined &&
          this.mouseEnterTimeout !== undefined
        ) {
          this.setState({ visible: true });
        }
      },
      (this.props.mouseEnterDelay ?? 0) * MILLISECONDS_IN_A_SECOND,
    );

    if (this.props.onShow) {
      this.props.onShow();
    }
  };

  handlePointerLeave = () => {
    if (this.mouseEnterTimeout !== undefined) {
      window.clearTimeout(this.mouseEnterTimeout);
      this.mouseEnterTimeout = undefined;
    }

    if (!this.mouseIn) {
      this.mouseLeaveTimeout = window.setTimeout(
        () => {
          if (this.mounted && this.props.visible === undefined && !this.mouseIn) {
            this.setState({ visible: false });
          }
        },
        (this.props.mouseLeaveDelay ?? 0) * MILLISECONDS_IN_A_SECOND,
      );

      if (this.props.onHide) {
        this.props.onHide();
      }
    }
  };

  handleFocus = () => {
    this.setState({ visible: true });
    if (this.props.onShow) {
      this.props.onShow();
    }
  };

  handleBlur = () => {
    this.setState({ visible: false });
  };

  handleOverlayPointerEnter = () => {
    this.mouseIn = true;
  };

  handleOverlayPointerLeave = () => {
    this.mouseIn = false;
    this.handlePointerLeave();
  };

  handleChildPointerEnter = () => {
    this.handlePointerEnter();

    const { children } = this.props;

    const props = children.props as { onPointerEnter?: VoidFunction };

    if (typeof props.onPointerEnter === 'function') {
      props.onPointerEnter();
    }
  };

  handleChildPointerLeave = () => {
    this.handlePointerLeave();

    const { children } = this.props;

    const props = children.props as { onPointerLeave?: VoidFunction };

    if (typeof props.onPointerLeave === 'function') {
      props.onPointerLeave();
    }
  };

  needsFlipping = ({ leftFix, topFix }: State) => {
    // We can live with a tooltip that's slightly positioned over the toggle
    // node. Only trigger if it really starts overlapping, as the re-positioning
    // is quite expensive, needing 2 re-renders.
    const repositioningThreshold = 8;
    switch (this.getPlacement()) {
      case PopupPlacement.Left:
      case PopupPlacement.Right:
        return Boolean(leftFix && Math.abs(leftFix) > repositioningThreshold);
      case PopupPlacement.Top:
      case PopupPlacement.Bottom:
        return Boolean(topFix && Math.abs(topFix) > repositioningThreshold);
      default:
        return false;
    }
  };

  render() {
    const placement = this.getPlacement();
    const style = isMeasured(this.state)
      ? {
          left: this.state.left,
          top: this.state.top,
          width: this.state.width,
          height: this.state.height,
        }
      : undefined;

    return (
      <>
        {React.cloneElement(this.props.children, {
          onPointerEnter: this.handleChildPointerEnter,
          onPointerLeave: this.handleChildPointerLeave,
          onFocus: this.handleFocus,
          onBlur: this.handleBlur,
          // aria-describedby is the semantically correct property to use, but it's not
          // always well supported. We sometimes need to handle this differently, depending
          // on the triggering element. We should NOT use aria-labelledby, as this can
          // have unintended effects (e.g., this can mess up buttons that need a tooltip).
          'aria-describedby': this.id,
        })}
        {this.isVisible() && (
          <TooltipPortal>
            <TooltipWrapper
              className={classNames(placement)}
              onPointerEnter={this.handleOverlayPointerEnter}
              onPointerLeave={this.handleOverlayPointerLeave}
              ref={this.tooltipNodeRef}
              role="tooltip"
              style={style}
            >
              <TooltipWrapperInner>{this.props.content}</TooltipWrapperInner>
              <TooltipWrapperArrow
                style={
                  isMeasured(this.state)
                    ? this.adjustArrowPosition(placement, this.state)
                    : undefined
                }
              />
            </TooltipWrapper>
          </TooltipPortal>
        )}
      </>
    );
  }
}

class TooltipPortal extends React.Component<React.PropsWithChildren> {
  el: HTMLElement;

  constructor(props: object) {
    super(props);
    this.el = document.createElement('div');
  }

  componentDidMount() {
    document.body.appendChild(this.el);
  }

  componentWillUnmount() {
    document.body.removeChild(this.el);
  }

  render() {
    return createPortal(this.props.children, this.el);
  }
}

const fadeIn = keyframes`
  from {
    opacity: 0;
  }

  to {
    opacity: 1;
  }
`;

const ARROW_WIDTH = 6;
const ARROW_HEIGHT = 7;
const ARROW_MARGIN = 3;

export const TooltipWrapper = styled.div`
  animation: ${fadeIn} 0.3s forwards;

  ${tw`sw-absolute`}
  ${tw`sw-z-tooltip`};
  ${tw`sw-block`};
  ${tw`sw-box-border`};
  ${tw`sw-h-auto`};
  ${tw`sw-body-sm`};

  &.top {
    margin-top: -${ARROW_MARGIN}px;
    padding: ${ARROW_HEIGHT}px 0;
  }

  &.right {
    margin-left: ${ARROW_MARGIN}px;
    padding: 0 ${ARROW_HEIGHT}px;
  }

  &.bottom {
    margin-top: ${ARROW_MARGIN}px;
    padding: ${ARROW_HEIGHT}px 0;
  }

  &.left {
    margin-left: -${ARROW_MARGIN}px;
    padding: 0 ${ARROW_HEIGHT}px;
  }
`;

const TooltipWrapperArrow = styled.div`
  ${tw`sw-absolute`};
  ${tw`sw-w-0`};
  ${tw`sw-h-0`};
  ${tw`sw-border-solid`};
  ${tw`sw-border-transparent`};
  ${TooltipWrapper}.top & {
    border-width: ${ARROW_HEIGHT}px ${ARROW_WIDTH}px 0;
    border-top-color: ${themeColor('tooltipBackground')};
    transform: translateX(-${ARROW_WIDTH}px);

    ${tw`sw-bottom-0`};
    ${tw`sw-left-1/2`};
  }

  ${TooltipWrapper}.right & {
    border-width: ${ARROW_WIDTH}px ${ARROW_HEIGHT}px ${ARROW_WIDTH}px 0;
    border-right-color: ${themeColor('tooltipBackground')};
    transform: translateY(-${ARROW_WIDTH}px);

    ${tw`sw-top-1/2`};
    ${tw`sw-left-0`};
  }

  ${TooltipWrapper}.left & {
    border-width: ${ARROW_WIDTH}px 0 ${ARROW_WIDTH}px ${ARROW_HEIGHT}px;
    border-left-color: ${themeColor('tooltipBackground')};
    transform: translateY(-${ARROW_WIDTH}px);

    ${tw`sw-top-1/2`};
    ${tw`sw-right-0`};
  }

  ${TooltipWrapper}.bottom & {
    border-width: 0 ${ARROW_WIDTH}px ${ARROW_HEIGHT}px;
    border-bottom-color: ${themeColor('tooltipBackground')};
    transform: translateX(-${ARROW_WIDTH}px);

    ${tw`sw-top-0`};
    ${tw`sw-left-1/2`};
  }
`;

export const TooltipWrapperInner = styled.div`
  font: var(--echoes-typography-paragraph-small-regular);
  padding: var(--echoes-dimension-space-50) var(--echoes-dimension-space-150);
  color: var(--echoes-color-text-on-color);
  background-color: var(--echoes-color-background-inverse);
  border-radius: var(--echoes-border-radius-200);

  ${tw`sw-max-w-[22rem]`}
  ${tw`sw-overflow-hidden`};
  ${tw`sw-text-left`};
  ${tw`sw-no-underline`};
  ${tw`sw-break-words`};

  hr {
    background-color: ${themeColor('tooltipSeparator')};

    ${tw`sw-mx-4`};
  }
`;
