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
import { throttle, uniqueId } from 'lodash';
import * as React from 'react';
import { createPortal, findDOMNode } from 'react-dom';
import { rawSizes } from '../../app/theme';
import { ONE_SECOND } from '../../helpers/constants';
import { translate } from '../../helpers/l10n';
import EscKeydownHandler from './EscKeydownHandler';
import FocusOutHandler from './FocusOutHandler';
import ScreenPositionFixer from './ScreenPositionFixer';
import './Tooltip.css';

export type Placement = 'bottom' | 'right' | 'left' | 'top';

interface TooltipProps {
  classNameSpace?: string;
  children: React.ReactElement;
  mouseEnterDelay?: number;
  mouseLeaveDelay?: number;
  onShow?: () => void;
  onHide?: () => void;
  content: React.ReactNode;
  side?: Placement;
  isOpen?: boolean;
  // If tooltip overlay has interactive content (links for instance) we may set this to true to stop
  // default behavior of tabbing (other changes should be done outside of this component to make it work)
  // See example DocHelpTooltip
  isInteractive?: boolean;
  classNameInner?: string;
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

/** @deprecated Use {@link Echoes.Tooltip | Tooltip} from Echoes instead.
 *
 * Echoes Tooltip component should mainly be used on interactive element and contain very simple text based content.
 * If the content is more complex use a Popover component instead (not available yet).
 *
 * Some of the props have changed or been renamed:
 * - `children` is the trigger for the tooltip, should be an interactive Element. If not an Echoes component, make sure the component forwards the props and the ref to an interactive DOM node, it's needed by the tooltip to position itself.
 * - `overlay` is now `content`, that's the tooltip content. It's a ReactNode for convenience but should render only text based content, no interactivity is allowed inside the tooltip.
 * - ~`classNameSpace`~ doesn't exist anymore, was mostly used in situation that should be replaced by a Popover component.
 * - ~`mouseEnterDelay`~ doesn't exist anymore, was mostly used in situation that should be replaced by a Popover component.
 * - ~`mouseLeaveDelay`~ doesn't exist anymore, was mostly used in situation that should be replaced by a Popover component.
 * - ~`onHide`~ doesn't exist anymore, was mostly used in situation that should be replaced by a Popover component.
 * - ~`onShow`~ doesn't exist anymore, was mostly used in situation that should be replaced by a Popover component.
 * - `placement` is now `align` and `side`, based on the {@link Echoes.TooltipAlign | TooltipAlign} and {@link Echoes.TooltipSide | TooltipSide} enums.
 * - `visible` is now `isOpen`
 */
export default function Tooltip(props: TooltipProps) {
  // `overlay` is a ReactNode, so it can be `undefined` or `null`. This allows to easily
  // render a tooltip conditionally. More generally, we avoid rendering empty tooltips.
  return props.content != null && props.content !== '' ? (
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
  id: string;

  static defaultProps = {
    mouseEnterDelay: 0.1,
  };

  constructor(props: TooltipProps) {
    super(props);
    this.state = {
      flipped: false,
      placement: props.side,
      visible: props.isOpen ?? false,
    };
    this.id = uniqueId('tooltip-');
    this.throttledPositionTooltip = throttle(this.positionTooltip, 10);
  }

  componentDidMount() {
    this.mounted = true;
    if (this.props.isOpen === true) {
      this.positionTooltip();
      this.addEventListeners();
    }
  }

  componentDidUpdate(prevProps: TooltipProps, prevState: State) {
    if (this.props.side !== prevProps.side) {
      this.setState({ placement: this.props.side });
      // Break. This will trigger a new componentDidUpdate() call, so the below
      // positionTooltip() call will be correct. Otherwise, it might not use
      // the new state.placement value.
      return;
    }

    if (
      // opens
      (this.props.isOpen === true && !prevProps.isOpen) ||
      (this.props.isOpen === undefined && this.state.visible && !prevState.visible)
    ) {
      this.positionTooltip();
      this.addEventListeners();
    } else if (
      // closes
      (!this.props.isOpen && prevProps.isOpen === true) ||
      (this.props.isOpen === undefined && !this.state.visible && prevState.visible)
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
    return this.props.isOpen ?? this.state.visible;
  };

  getPlacement = (): Placement => {
    return this.state.placement ?? 'bottom';
  };

  tooltipNodeRef = (node: HTMLElement | null) => {
    this.tooltipNode = node;
  };

  adjustArrowPosition = (
    placement: Placement,
    { leftFix, topFix }: { leftFix: number; topFix: number },
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
    // `findDOMNode(this)` will search for the DOM node for the current component.
    // First, it will find a React.Fragment (see `render`). It will skip this, and
    // it will get the DOM node of the first child, i.e. DOM node of `this.props.children`.
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

      // Save width and height (and later set in `render`) to avoid resizing the tooltip
      // element when it's placed close to the window's edge.
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
      placement: this.props.side,
    });
  };

  handleMouseEnter = () => {
    this.mouseEnterTimeout = window.setTimeout(
      () => {
        // For some reason, even after the `this.mouseEnterTimeout` is cleared, it still
        // triggers. To workaround this issue, check that its value is not `undefined`
        // (if it's `undefined`, it means the timer has been reset).
        if (
          this.mounted &&
          this.props.isOpen === undefined &&
          this.mouseEnterTimeout !== undefined
        ) {
          this.setState({ visible: true });
        }
      },
      (this.props.mouseEnterDelay ?? 0) * ONE_SECOND,
    );

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
      this.mouseLeaveTimeout = window.setTimeout(
        () => {
          if (this.mounted && this.props.isOpen === undefined && !this.mouseIn) {
            this.setState({ visible: false });
          }
          if (this.props.onHide && !this.mouseIn) {
            this.props.onHide();
          }
        },
        (this.props.mouseLeaveDelay ?? 0) * ONE_SECOND,
      );
    }
  };

  handleFocus = () => {
    this.setState({ visible: true });
    if (this.props.onShow) {
      this.props.onShow();
    }
  };

  handleBlur = () => {
    if (!this.props.isInteractive) {
      this.closeTooltip();
    }
  };

  closeTooltip = () => {
    if (this.mounted) {
      this.setState({ visible: false });
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
    const threshold = rawSizes.grid;
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
          },
        );
      }, 1);
      return null;
    }

    const { classNameSpace = 'tooltip' } = this.props;
    const currentPlacement = this.getPlacement();
    const style = isMeasured(this.state)
      ? {
          left: this.state.left + leftFix,
          top: this.state.top + topFix,
          width: this.state.width,
          height: this.state.height,
        }
      : undefined;

    return (
      <FocusOutHandler
        onFocusOut={this.closeTooltip}
        className={`${classNameSpace} ${currentPlacement}`}
        onPointerEnter={this.handleOverlayMouseEnter}
        onPointerLeave={this.handleOverlayMouseLeave}
        innerRef={this.tooltipNodeRef}
        style={style}
      >
        {this.renderOverlay()}
        <div
          className={`${classNameSpace}-arrow`}
          style={
            isMeasured(this.state)
              ? this.adjustArrowPosition(currentPlacement, { leftFix, topFix })
              : undefined
          }
        />
      </FocusOutHandler>
    );
  };

  renderOverlay() {
    const isVisible = this.isVisible();
    const {
      classNameSpace = 'tooltip',
      isInteractive,
      content: overlay,
      classNameInner,
    } = this.props;
    return (
      <div
        className={classNames(`${classNameSpace}-inner sw-font-sans`, classNameInner, {
          'sw-hidden': !isVisible,
        })}
        id={this.id}
        role="tooltip"
        aria-hidden={!isVisible}
      >
        {isInteractive && <span className="sw-sr-only">{translate('tooltip_is_interactive')}</span>}
        {overlay}
      </div>
    );
  }

  render() {
    const isVisible = this.isVisible();
    const { isInteractive } = this.props;
    return (
      <>
        {React.cloneElement(this.props.children, {
          onPointerEnter: this.handleMouseEnter,
          onPointerLeave: this.handleMouseLeave,
          onFocus: this.handleFocus,
          onBlur: this.handleBlur,
          tabIndex: isInteractive ? 0 : undefined,
          // aria-describedby is the semantically correct property to use, but it's not
          // always well supported. We sometimes need to handle this differently, depending
          // on the triggering element. For example, we can add a child <description> element
          // if the triggering element is an SVG. See HelpTooltip for an example.
          // We should NOT use aria-labelledby, as this can have unintended effects (e.g., this
          // can mess up buttons that need a tooltip).
          'aria-describedby': this.id,
        })}
        {!isVisible && <TooltipPortal>{this.renderOverlay()}</TooltipPortal>}
        {isVisible && (
          <EscKeydownHandler onKeydown={this.closeTooltip}>
            <TooltipPortal>
              <ScreenPositionFixer ready={isMeasured(this.state)}>
                {this.renderActual}
              </ScreenPositionFixer>
            </TooltipPortal>
          </EscKeydownHandler>
        )}
      </>
    );
  }
}

class TooltipPortal extends React.Component<React.PropsWithChildren<{}>> {
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
