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
import { createPortal, findDOMNode } from 'react-dom';
import { throttle } from 'lodash';
import './Tooltip.css';

export type Placement = 'bottom' | 'right' | 'left' | 'top';

interface Props {
  children: React.ReactElement<{}>;
  mouseEnterDelay?: number;
  onShow?: () => void;
  onHide?: () => void;
  overlay: React.ReactNode;
  placement?: Placement;
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
  visible: boolean;
}

type State = OwnState & Partial<Measurements>;

function isMeasured(state: State): state is OwnState & Measurements {
  return state.height !== undefined;
}

const EDGE_MARGIN = 4;

export default function Tooltip(props: Props) {
  // allows to pass `undefined` to `overlay` to avoid rendering a tooltip
  // can useful in some cases to render the tooltip conditionally
  return props.overlay !== undefined ? <TooltipInner {...props} /> : props.children;
}

export class TooltipInner extends React.Component<Props, State> {
  throttledPositionTooltip: (() => void);
  mouseEnterInterval?: number;
  tooltipNode?: HTMLElement | null;
  mounted = false;

  static defaultProps = {
    mouseEnterDelay: 0.1
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      visible: props.visible !== undefined ? props.visible : false
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

  componentDidUpdate(prevProps: Props, prevState: State) {
    if (
      // opens
      (this.props.visible === true && prevProps.visible === false) ||
      (this.props.visible === undefined &&
        this.state.visible === true &&
        prevState.visible === false)
    ) {
      this.positionTooltip();
      this.addEventListeners();
    } else if (
      // closes
      (this.props.visible === false && prevProps.visible === true) ||
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
  }

  addEventListeners = () => {
    window.addEventListener('resize', this.throttledPositionTooltip);
    window.addEventListener('scroll', this.throttledPositionTooltip);
  };

  removeEventListeners = () => {
    window.removeEventListener('resize', this.throttledPositionTooltip);
    window.removeEventListener('scroll', this.throttledPositionTooltip);
  };

  isVisible = () => {
    return this.props.visible !== undefined ? this.props.visible : this.state.visible;
  };

  getPlacement = (): Placement => {
    return this.props.placement || 'bottom';
  };

  tooltipNodeRef = (node: HTMLElement | null) => {
    this.tooltipNode = node;
  };

  positionTooltip = () => {
    // `findDOMNode(this)` will search for the DOM node for the current component
    // first it will find a React.Fragment (see `render`),
    // so it will get the DOM node of the first child, i.e. DOM node of `this.props.children`
    // docs: https://reactjs.org/docs/refs-and-the-dom.html#exposing-dom-refs-to-parent-components

    // eslint-disable-next-line react/no-find-dom-node
    const toggleNode = findDOMNode(this);

    if (toggleNode && this.tooltipNode) {
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

      // make sure the tooltip fits in the document
      // it may go out of the current viewport, if it has scrolls
      const { scrollWidth, scrollHeight } = document.documentElement;

      let leftFix = 0;
      if (left < EDGE_MARGIN) {
        leftFix = EDGE_MARGIN - left;
      } else if (left + width > scrollWidth - EDGE_MARGIN) {
        leftFix = scrollWidth - EDGE_MARGIN - left - width;
      }

      let topFix = 0;
      if (top < EDGE_MARGIN) {
        topFix = EDGE_MARGIN - top;
      } else if (top + height > scrollHeight - EDGE_MARGIN) {
        topFix = scrollHeight - EDGE_MARGIN - top - height;
      }

      // save width and height (and later set in `render`) to avoid resizing the tooltip element,
      // when it's placed close to the window edge
      const measurements: Measurements = {
        left: window.pageXOffset + left,
        leftFix,
        top: window.pageYOffset + top,
        topFix,
        width,
        height
      };
      this.setState(measurements);
    }
  };

  clearPosition = () => {
    this.setState({
      left: undefined,
      leftFix: undefined,
      top: undefined,
      topFix: undefined,
      width: undefined,
      height: undefined
    });
  };

  handleMouseEnter = () => {
    this.mouseEnterInterval = window.setTimeout(() => {
      if (this.mounted) {
        if (this.props.visible === undefined) {
          this.setState({ visible: true });
        }
      }
    }, (this.props.mouseEnterDelay || 0) * 1000);

    if (this.props.onShow) {
      this.props.onShow();
    }
  };

  handleMouseLeave = () => {
    if (this.mouseEnterInterval !== undefined) {
      window.clearInterval(this.mouseEnterInterval);
      this.mouseEnterInterval = undefined;
    }
    if (this.props.visible === undefined) {
      this.setState({ visible: false });
    }

    if (this.props.onHide) {
      this.props.onHide();
    }
  };

  render() {
    return (
      <>
        {React.cloneElement(this.props.children, {
          onMouseEnter: this.handleMouseEnter,
          onMouseLeave: this.handleMouseLeave
        })}
        {this.isVisible() && (
          <TooltipPortal>
            <div
              className={`tooltip ${this.getPlacement()}`}
              ref={this.tooltipNodeRef}
              style={
                isMeasured(this.state)
                  ? {
                      left: this.state.left + this.state.leftFix,
                      top: this.state.top + this.state.topFix,
                      width: this.state.width,
                      height: this.state.height
                    }
                  : undefined
              }>
              <div className="tooltip-inner">{this.props.overlay}</div>
              <div
                className="tooltip-arrow"
                style={
                  isMeasured(this.state)
                    ? { marginLeft: -this.state.leftFix, marginTop: -this.state.topFix }
                    : undefined
                }
              />
            </div>
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
