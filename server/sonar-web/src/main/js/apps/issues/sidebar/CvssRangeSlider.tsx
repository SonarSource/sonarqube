import styled from '@emotion/styled';
import * as React from 'react';
import tw from 'twin.macro';
import { themeColor } from '../../../design-system/helpers';

const CVSS_MIN = 0;
const CVSS_MAX = 10;
const CVSS_STEP = 0.1;

interface Props {
  disabled?: boolean;
  onChange: (min: number, max: number) => void;
  value: { min: number; max: number };
}

interface State {
  min: number;
  max: number;
  isDragging: 'min' | 'max' | null;
}

export class CvssRangeSlider extends React.PureComponent<Props, State> {
  private sliderRef = React.createRef<HTMLDivElement>();
  private minInputRef = React.createRef<HTMLInputElement>();
  private maxInputRef = React.createRef<HTMLInputElement>();

  constructor(props: Props) {
    super(props);
    this.state = {
      min: props.value.min,
      max: props.value.max,
      isDragging: null,
    };
  }

  componentDidUpdate(prevProps: Props) {
    if (
      prevProps.value.min !== this.props.value.min ||
      prevProps.value.max !== this.props.value.max
    ) {
      this.setState({
        min: this.props.value.min,
        max: this.props.value.max,
      });
    }
  }

  getPercentage = (value: number) => {
    return ((value - CVSS_MIN) / (CVSS_MAX - CVSS_MIN)) * 100;
  };

  getValueFromPosition = (clientX: number): number => {
    if (!this.sliderRef.current) {
      return CVSS_MIN;
    }

    const rect = this.sliderRef.current.getBoundingClientRect();
    const percentage = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width));
    const value = CVSS_MIN + percentage * (CVSS_MAX - CVSS_MIN);
    return Math.round(value / CVSS_STEP) * CVSS_STEP;
  };

  handleMouseDown = (handle: 'min' | 'max') => (e: React.MouseEvent) => {
    if (this.props.disabled) {
      return;
    }

    e.preventDefault();
    this.setState({ isDragging: handle });

    const handleMouseMove = (moveEvent: MouseEvent) => {
      const newValue = this.getValueFromPosition(moveEvent.clientX);
      this.updateValue(handle, newValue);
    };

    const handleMouseUp = () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
      this.setState({ isDragging: null });
      this.props.onChange(this.state.min, this.state.max);
    };

    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
  };

  handleTouchStart = (handle: 'min' | 'max') => (e: React.TouchEvent) => {
    if (this.props.disabled) {
      return;
    }

    e.preventDefault();
    this.setState({ isDragging: handle });

    const handleTouchMove = (moveEvent: TouchEvent) => {
      if (moveEvent.touches.length > 0) {
        const newValue = this.getValueFromPosition(moveEvent.touches[0].clientX);
        this.updateValue(handle, newValue);
      }
    };

    const handleTouchEnd = () => {
      document.removeEventListener('touchmove', handleTouchMove);
      document.removeEventListener('touchend', handleTouchEnd);
      this.setState({ isDragging: null });
      this.props.onChange(this.state.min, this.state.max);
    };

    document.addEventListener('touchmove', handleTouchMove, { passive: false });
    document.addEventListener('touchend', handleTouchEnd);
  };

  updateValue = (handle: 'min' | 'max', newValue: number) => {
    const clampedValue = Math.max(CVSS_MIN, Math.min(CVSS_MAX, newValue));

    if (handle === 'min') {
      const min = Math.min(clampedValue, this.state.max);
      this.setState({ min });
    } else {
      const max = Math.max(clampedValue, this.state.min);
      this.setState({ max });
    }
  };

  handleInputChange = (handle: 'min' | 'max') => (e: React.ChangeEvent<HTMLInputElement>) => {
    if (this.props.disabled) {
      return;
    }

    const newValue = parseFloat(e.target.value);
    if (!isNaN(newValue)) {
      this.updateValue(handle, newValue);
      this.props.onChange(
        handle === 'min' ? Math.min(newValue, this.state.max) : this.state.min,
        handle === 'max' ? Math.max(newValue, this.state.min) : this.state.max,
      );
    }
  };

  handleKeyDown = (handle: 'min' | 'max') => (e: React.KeyboardEvent) => {
    if (this.props.disabled) {
      return;
    }

    const step = CVSS_STEP;
    let newValue = handle === 'min' ? this.state.min : this.state.max;

    switch (e.key) {
      case 'ArrowLeft':
      case 'ArrowDown':
        e.preventDefault();
        newValue = Math.max(CVSS_MIN, newValue - step);
        break;
      case 'ArrowRight':
      case 'ArrowUp':
        e.preventDefault();
        newValue = Math.min(CVSS_MAX, newValue + step);
        break;
      case 'Home':
        e.preventDefault();
        newValue = CVSS_MIN;
        break;
      case 'End':
        e.preventDefault();
        newValue = CVSS_MAX;
        break;
      default:
        return;
    }

    this.updateValue(handle, newValue);
    this.props.onChange(
      handle === 'min' ? Math.min(newValue, this.state.max) : this.state.min,
      handle === 'max' ? Math.max(newValue, this.state.min) : this.state.max,
    );
  };

  formatValue = (value: number): string => {
    return value.toFixed(1);
  };

  render() {
    const { disabled } = this.props;
    const { min, max, isDragging } = this.state;

    const minPercent = this.getPercentage(min);
    const maxPercent = this.getPercentage(max);
    const rangePercent = maxPercent - minPercent;

    const isDefaultRange = min === CVSS_MIN && max === CVSS_MAX;
    const isAtMax = min === CVSS_MAX && max === CVSS_MAX;

    return (
      <SliderContainer>
        <SliderTrack
          ref={this.sliderRef}
          disabled={disabled}
          onClick={(e) => {
            if (disabled || isDragging) {
              return;
            }
            const clickValue = this.getValueFromPosition(e.clientX);
            const distanceToMin = Math.abs(clickValue - min);
            const distanceToMax = Math.abs(clickValue - max);

            if (distanceToMin < distanceToMax) {
              this.updateValue('min', clickValue);
              this.props.onChange(Math.min(clickValue, max), max);
            } else {
              this.updateValue('max', clickValue);
              this.props.onChange(min, Math.max(clickValue, min));
            }
          }}
        >
          <SliderRange
            style={{
              left: `${minPercent}%`,
              width: `${rangePercent}%`,
            }}
          />
          <SliderHandle
            aria-label={`CVSS minimum value: ${this.formatValue(min)}`}
            aria-valuemin={CVSS_MIN}
            aria-valuemax={CVSS_MAX}
            aria-valuenow={min}
            disabled={disabled}
            role="slider"
            style={{
              left: `${minPercent}%`,
              zIndex: isAtMax ? 3 : 2,
            }}
            tabIndex={disabled ? -1 : 0}
            onKeyDown={this.handleKeyDown('min')}
            onMouseDown={this.handleMouseDown('min')}
            onTouchStart={this.handleTouchStart('min')}
          >
            <HandleLabel>{this.formatValue(min)}</HandleLabel>
          </SliderHandle>
          <SliderHandle
            aria-label={`CVSS maximum value: ${this.formatValue(max)}`}
            aria-valuemin={CVSS_MIN}
            aria-valuemax={CVSS_MAX}
            aria-valuenow={max}
            disabled={disabled}
            role="slider"
            style={{
              left: `${maxPercent}%`,
              zIndex: isAtMax ? 2 : 2,
            }}
            tabIndex={disabled ? -1 : 0}
            onKeyDown={this.handleKeyDown('max')}
            onMouseDown={this.handleMouseDown('max')}
            onTouchStart={this.handleTouchStart('max')}
          >
            <HandleLabel>{this.formatValue(max)}</HandleLabel>
          </SliderHandle>
        </SliderTrack>
        <SliderLabels>
          <Label>{CVSS_MIN}</Label>
          <Label>{CVSS_MAX}</Label>
        </SliderLabels>
        {!isDefaultRange && (
          <HiddenInputs>
            <input
              ref={this.minInputRef}
              max={max}
              min={CVSS_MIN}
              step={CVSS_STEP}
              type="range"
              value={min}
              onChange={this.handleInputChange('min')}
            />
            <input
              ref={this.maxInputRef}
              max={CVSS_MAX}
              min={min}
              step={CVSS_STEP}
              type="range"
              value={max}
              onChange={this.handleInputChange('max')}
            />
          </HiddenInputs>
        )}
      </SliderContainer>
    );
  }
}

const SliderContainer = styled.div`
  ${tw`sw-flex sw-flex-col sw-gap-2`};
  ${tw`sw-px-2 sw-py-4`};
  ${tw`sw-pt-[2rem]`};
  position: relative;
`;

const SliderTrack = styled.div<{ disabled?: boolean }>`
  position: relative;
  height: 8px;
  width: 85%;
  margin: 0 auto;
  background: ${themeColor('progressBarBackground')};
  border-radius: 640.31px;
  cursor: ${({ disabled }) => (disabled ? 'not-allowed' : 'pointer')};
  touch-action: none;
`;

const SliderRange = styled.div`
  position: absolute;
  height: 100%;
  background: ${themeColor('primary')};
  border-radius: 2px;
  pointer-events: none;
`;

const SliderHandle = styled.div<{ disabled?: boolean }>`
  position: absolute;
  width: 14px;
  height: 14px;
  background: ${themeColor('primaryDark')};
  border: 2.5px solid #ffffff;
  border-radius: 50%;
  cursor: ${({ disabled }) => (disabled ? 'not-allowed' : 'grab')};
  transform: translate(-50%, -50%);
  top: 50%;
  z-index: 2;
  touch-action: none;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 7px rgba(0, 0, 0, 0.1);

  &:active {
    cursor: ${({ disabled }) => (disabled ? 'not-allowed' : 'grabbing')};
  }

  &:focus-visible {
    outline: var(--echoes-focus-border-width-default) solid var(--echoes-color-focus-default);
    outline-offset: 2px;
  }

  &:hover:not(:disabled) {
    transform: translate(-50%, -50%) scale(1.1);
  }
`;

const HandleLabel = styled.div`
  position: absolute;
  bottom: 18px;
  left: 50%;
  transform: translateX(-50%);
  white-space: nowrap;
  font-size: 10px;
  font-weight: 600;
  color: var(--echoes-color-text-primary);
  background: ${themeColor('progressBarBackground')};
  padding: 5px 10px;
  border-radius: 6px;
  pointer-events: none;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  text-align: center;

  &::after {
    content: '';
    position: absolute;
    bottom: -5px;
    left: 50%;
    transform: translateX(-50%);
    width: 0;
    height: 0;
    border-left: 5px solid transparent;
    border-right: 5px solid transparent;
    border-top: 5px solid ${themeColor('progressBarBackground')};
  }
`;

const SliderLabels = styled.div`
  ${tw`sw-flex sw-justify-between`};
  ${tw`sw-text-xs`};
  width: 85%;
  margin: 4px auto 0;
  color: var(--echoes-color-text-subheading);
`;

const Label = styled.span`
  font-size: 12px;
`;

const HiddenInputs = styled.div`
  position: absolute;
  opacity: 0;
  pointer-events: none;
  width: 0;
  height: 0;
  overflow: hidden;
`;
