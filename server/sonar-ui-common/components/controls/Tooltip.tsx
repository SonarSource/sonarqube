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
import { throttle } from 'lodash';
import * as React from 'react';
import { createPortal, findDOMNode } from 'react-dom';
import ThemeContext from '../theme';
import ScreenPositionFixer from './ScreenPositionFixer';
import './Tooltip.css';

export type Placement = 'bottom' | 'right' | 'left' | 'top';

export interface TooltipProps {
  classNameSpace?: string;
  children: React.ReactElement<{}>;
  mouseEnterDelay?: number;
  mouseLeaveDelay?: number;
  onShow?: () => void;
  onHide?: () => void;
  overlay: React.ReactNode;
  placement?: Placement;
  visible?: boolean;
}

interface Measurements {
  height: number;
  left: number;
  top: number;
  width: number;
}

interface OwnState {
  flipped: boolean;
  placement?: Placement;
  visible: boolean;
}

type State = OwnState & Partial<Measurements>;

const FLIP_MAP: { [key in Placement]: Placement } = {
  left: 'right',
  right: 'left',
  top: 'bottom',
  bottom: 'top',
};

function isMeasured(state: State): state is OwnState & Measurements {
  return state.height !== undefined;
}

export default function Tooltip(props: TooltipProps) {
  // overlay is a ReactNode, so it can be `undefined` or `null`
  // this allows to easily render a tooltip conditionally
  // more generaly we avoid rendering empty tooltips
  return props.overlay != null && props.overlay !== '' ? (
    <TooltipInner {...props} />
  ) : (
    props.children
  );
}

export class TooltipInner extends React.Component<TooltipProps, State> {
  throttledPositionTooltip: () => void;
  mouseEnterTimeout?: number;
  mouseLeaveTimeout?: number;
  tooltipNode?: HTMLElement | null;
  mounted = false;
  mouseIn = false;

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
    this.throttledPositionTooltip = throttle(this.positionTooltip, 10);
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
      this.setState({ placement: this.props.placement });
      // Break. This will trigger a new componentDidUpdate() call, so the below
      // positionTooltip() call will be correct. Otherwise, it might not use
      // the new state.placement value.
      return;
    }

    if (
      // opens
      (this.props.visible === true && !prevProps.visible) ||
      (this.props.visible === undefined &&
        this.state.visible === true &&
        prevState.visible === false)
    ) {
      this.positionTooltip();
      this.addEventListeners();
    } else if (
      // closes
      (!this.props.visible && prevProps.visible === true) ||
      (this.props.visible === undefined &&
        this.state.visible === false &&
        prevState.visible === true)
    ) {
      this.clearPosition();
      this.removeEventListeners();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
    this.removeEventListeners();
    this.clearTimeouts();
  }

  static contextType = ThemeContext;

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

  isVisible = () => {
    return this.props.visible !== undefined ? this.props.visible : this.state.visible;
  };

  getPlacement = (): Placement => {
    return this.state.placement || 'bottom';
  };

  tooltipNodeRef = (node: HTMLElement | null) => {
    this.tooltipNode = node;
  };

  adjustArrowPosition = (
    placement: Placement,
    { leftFix, topFix }: { leftFix: number; topFix: number }
  ) => {
    switch (placement) {
      case 'left':
      case 'right':
        return { marginTop: -topFix };
      default:
        return { marginLeft: -leftFix };
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
      const toggleRect = toggleNode.getBoundingClientRect();
      const tooltipRect = this.tooltipNode.getBoundingClientRect();
      const { width, height } = tooltipRect;

      let left = 0;
      let top = 0;

      switch (this.getPlacement()) {
        case 'bottom':
          left = toggleRect.left + toggleRect.width / 2 - width / 2;
          top = toggleRect.top + toggleRect.height;
          break;
        case 'top':
          left = toggleRect.left + toggleRect.width / 2 - width / 2;
          top = toggleRect.top - height;
          break;
        case 'right':
          left = toggleRect.left + toggleRect.width;
          top = toggleRect.top + toggleRect.height / 2 - height / 2;
          break;
        case 'left':
          left = toggleRect.left - width;
          top = toggleRect.top + toggleRect.height / 2 - height / 2;
          break;
      }

      // save width and height (and later set in `render`) to avoid resizing the tooltip element,
      // when it's placed close to the window edge
      this.setState({
        left: window.pageXOffset + left,
        top: window.pageYOffset + top,
        width,
        height,
      });
    }
  };

  clearPosition = () => {
    this.setState({
      flipped: false,
      left: undefined,
      top: undefined,
      width: undefined,
      height: undefined,
      placement: this.props.placement,
    });
  };

  handleMouseEnter = () => {
    this.mouseEnterTimeout = window.setTimeout(() => {
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
    }, (this.props.mouseEnterDelay || 0) * 1000);

    if (this.props.onShow) {
      this.props.onShow();
    }
  };

  handleMouseLeave = () => {
    if (this.mouseEnterTimeout !== undefined) {
      window.clearTimeout(this.mouseEnterTimeout);
      this.mouseEnterTimeout = undefined;
    }

    if (!this.mouseIn) {
      this.mouseLeaveTimeout = window.setTimeout(() => {
        if (this.mounted && this.props.visible === undefined && !this.mouseIn) {
          this.setState({ visible: false });
        }
      }, (this.props.mouseLeaveDelay || 0) * 1000);

      if (this.props.onHide) {
        this.props.onHide();
      }
    }
  };

  handleOverlayMouseEnter = () => {
    this.mouseIn = true;
  };

  handleOverlayMouseLeave = () => {
    this.mouseIn = false;
    this.handleMouseLeave();
  };

  needsFlipping = (leftFix: number, topFix: number) => {
    // We can live with a tooltip that's slightly positioned over the toggle
    // node. Only trigger if it really starts overlapping, as the re-positioning
    // is quite expensive, needing 2 re-renders.
    const threshold = this.context.rawSizes.grid;
    switch (this.getPlacement()) {
      case 'left':
      case 'right':
        return Math.abs(leftFix) > threshold;
      case 'top':
      case 'bottom':
        return Math.abs(topFix) > threshold;
    }
    return false;
  };

  renderActual = ({ leftFix = 0, topFix = 0 }) => {
    if (
      !this.state.flipped &&
      (leftFix !== 0 || topFix !== 0) &&
      this.needsFlipping(leftFix, topFix)
    ) {
      // Update state in a render function... Not a good idea, but we need to
      // render in order to know if we need to flip... To prevent React from
      // complaining, we update the state using a setTimeout() call.
      setTimeout(() => {
        this.setState(
          ({ placement = 'bottom' }) => ({
            flipped: true,
            // Set height to undefined to force ScreenPositionFixer to
            // re-compute our positioning.
            height: undefined,
            placement: FLIP_MAP[placement],
          }),
          () => {
            if (this.state.visible) {
              // Force a re-positioning, as "only" updating the state doesn't
              // recompute the position, only re-renders with the previous
              // position (which is no longer correct).
              this.positionTooltip();
            }
          }
        );
      }, 1);
      return null;
    }

    const { classNameSpace = 'tooltip' } = this.props;
    const placement = this.getPlacement();
    const style = isMeasured(this.state)
      ? {
          left: this.state.left + leftFix,
          top: this.state.top + topFix,
          width: this.state.width,
          height: this.state.height,
        }
      : undefined;

    return (
      <div
        className={`${classNameSpace} ${placement}`}
        onMouseEnter={this.handleOverlayMouseEnter}
        onMouseLeave={this.handleOverlayMouseLeave}
        ref={this.tooltipNodeRef}
        style={style}>
        <div className={`${classNameSpace}-inner`}>{this.props.overlay}</div>
        <div
          className={`${classNameSpace}-arrow`}
          style={
            isMeasured(this.state)
              ? this.adjustArrowPosition(placement, { leftFix, topFix })
              : undefined
          }
        />
      </div>
    );
  };

  render() {
    return (
      <>
        {React.cloneElement(this.props.children, {
          onMouseEnter: this.handleMouseEnter,
          onMouseLeave: this.handleMouseLeave,
        })}
        {this.isVisible() && (
          <TooltipPortal>
            <ScreenPositionFixer ready={isMeasured(this.state)}>
              {this.renderActual}
            </ScreenPositionFixer>
          </TooltipPortal>
        )}
      </>
    );
  }
}

class TooltipPortal extends React.Component {
  el: HTMLElement;

  constructor(props: {}) {
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
