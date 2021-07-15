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
import { throttle } from 'lodash';
import * as React from 'react';
import { createPortal, findDOMNode } from 'react-dom';
import ClickEventBoundary from '../controls/ClickEventBoundary';
import ScreenPositionFixer from '../controls/ScreenPositionFixer';
import './popups.css';

/**
 * Positioning rules:
 * - Bottom = below the block, horizontally centered
 * - BottomLeft = below the block, horizontally left-aligned
 * - BottomRight = below the block, horizontally right-aligned
 * - LeftTop = on the left-side of the block, vertically top-aligned
 * - RightTop = on the right-side of the block, vertically top-aligned
 * - RightBottom = on the right-side of the block, vetically bottom-aligned
 * - TopLeft = above the block, horizontally left-aligned
 */
export enum PopupPlacement {
  Bottom = 'bottom',
  BottomLeft = 'bottom-left',
  BottomRight = 'bottom-right',
  LeftTop = 'left-top',
  RightTop = 'right-top',
  RightBottom = 'right-bottom',
  TopLeft = 'top-left',
}

interface PopupProps {
  arrowStyle?: React.CSSProperties;
  children?: React.ReactNode;
  className?: string;
  noPadding?: boolean;
  placement?: PopupPlacement;
  style?: React.CSSProperties;
}

function PopupBase(props: PopupProps, ref: React.Ref<HTMLDivElement>) {
  const { placement = PopupPlacement.Bottom } = props;
  return (
    <ClickEventBoundary>
      <div
        className={classNames(
          'popup',
          `is-${placement}`,
          { 'no-padding': props.noPadding },
          props.className
        )}
        ref={ref || React.createRef()}
        style={props.style}>
        {props.children}
        <PopupArrow style={props.arrowStyle} />
      </div>
    </ClickEventBoundary>
  );
}

const PopupWithRef = React.forwardRef(PopupBase);
PopupWithRef.displayName = 'Popup';

export const Popup = PopupWithRef;

interface PopupArrowProps {
  style?: React.CSSProperties;
}

export function PopupArrow(props: PopupArrowProps) {
  return <div className="popup-arrow" style={props.style} />;
}

interface PortalPopupProps extends Omit<PopupProps, 'arrowStyle' | 'style'> {
  arrowOffset?: number;
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

export class PortalPopup extends React.Component<PortalPopupProps, State> {
  mounted = false;
  popupNode = React.createRef<HTMLDivElement>();
  throttledPositionTooltip: () => void;

  constructor(props: PortalPopupProps) {
    super(props);
    this.state = {};
    this.throttledPositionTooltip = throttle(this.positionPopup, 10);
  }

  componentDidMount() {
    this.mounted = true;
    this.positionPopup();
    this.addEventListeners();
  }

  componentDidUpdate(prevProps: PortalPopupProps) {
    if (this.props.placement !== prevProps.placement || this.props.overlay !== prevProps.overlay) {
      this.positionPopup();
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

  getPlacement = (): PopupPlacement => {
    return this.props.placement || PopupPlacement.Bottom;
  };

  adjustArrowPosition = (
    placement: PopupPlacement,
    { leftFix, topFix }: { leftFix: number; topFix: number }
  ) => {
    const { arrowOffset = 0 } = this.props;
    switch (placement) {
      case PopupPlacement.Bottom:
      case PopupPlacement.BottomLeft:
      case PopupPlacement.BottomRight:
      case PopupPlacement.TopLeft:
        return { marginLeft: -leftFix + arrowOffset };
      default:
        return { marginTop: -topFix + arrowOffset };
    }
  };

  positionPopup = () => {
    // `findDOMNode(this)` will search for the DOM node for the current component
    // first it will find a React.Fragment (see `render`),
    // so it will get the DOM node of the first child, i.e. DOM node of `this.props.children`
    // docs: https://reactjs.org/docs/refs-and-the-dom.html#exposing-dom-refs-to-parent-components

    // eslint-disable-next-line react/no-find-dom-node
    const toggleNode = findDOMNode(this);

    if (toggleNode && toggleNode instanceof Element && this.popupNode.current) {
      const toggleRect = toggleNode.getBoundingClientRect();
      const { width, height } = this.popupNode.current.getBoundingClientRect();
      let left = 0;
      let top = 0;

      switch (this.getPlacement()) {
        case PopupPlacement.Bottom:
          left = toggleRect.left + toggleRect.width / 2 - width / 2;
          top = toggleRect.top + toggleRect.height;
          break;
        case PopupPlacement.BottomLeft:
          left = toggleRect.left;
          top = toggleRect.top + toggleRect.height;
          break;
        case PopupPlacement.BottomRight:
          left = toggleRect.left + toggleRect.width - width;
          top = toggleRect.top + toggleRect.height;
          break;
        case PopupPlacement.LeftTop:
          left = toggleRect.left - width;
          top = toggleRect.top;
          break;
        case PopupPlacement.RightTop:
          left = toggleRect.left + toggleRect.width;
          top = toggleRect.top;
          break;
        case PopupPlacement.RightBottom:
          left = toggleRect.left + toggleRect.width;
          top = toggleRect.top + toggleRect.height - height;
          break;
        case PopupPlacement.TopLeft:
          left = toggleRect.left;
          top = toggleRect.top - height;
          break;
      }

      // save width and height (and later set in `render`) to avoid resizing the popup element,
      // when it's placed close to the window edge
      this.setState({
        left: window.pageXOffset + left,
        top: window.pageYOffset + top,
        width,
        height,
      });
    }
  };

  renderActual = ({ leftFix = 0, topFix = 0 }) => {
    const { className, overlay, noPadding } = this.props;
    const placement = this.getPlacement();
    let arrowStyle;
    let style;
    if (isMeasured(this.state)) {
      style = {
        left: this.state.left + leftFix,
        top: this.state.top + topFix,
        width: this.state.width,
        height: this.state.height,
      };
      arrowStyle = this.adjustArrowPosition(placement, { leftFix, topFix });
    }

    return (
      <Popup
        arrowStyle={arrowStyle}
        className={className}
        noPadding={noPadding}
        placement={placement}
        ref={this.popupNode}
        style={style}>
        {overlay}
      </Popup>
    );
  };

  render() {
    return (
      <>
        {this.props.children}
        {this.props.overlay && (
          <PortalWrapper>
            <ScreenPositionFixer ready={isMeasured(this.state)}>
              {this.renderActual}
            </ScreenPositionFixer>
          </PortalWrapper>
        )}
      </>
    );
  }
}

class PortalWrapper extends React.Component {
  el: HTMLElement;

  constructor(props: {}) {
    super(props);
    this.el = document.createElement('div');
    this.el.classList.add('popup-portal');
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
