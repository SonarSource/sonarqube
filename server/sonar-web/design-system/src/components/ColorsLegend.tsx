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
import { useTheme } from '@emotion/react';
import styled from '@emotion/styled';
import tw from 'twin.macro';
import { themeBorder, themeColor, themeContrast } from '../helpers';
import { BubbleColorVal } from '../types/charts';
import { Tooltip } from './Tooltip';
import { Checkbox } from './input/Checkbox';

export interface ColorFilterOption {
  ariaLabel?: string;
  backgroundColor?: string;
  borderColor?: string;
  label: React.ReactNode;
  overlay?: React.ReactNode;
  selected: boolean;
  value: string | number;
}

interface ColorLegendProps {
  className?: string;
  colors: ColorFilterOption[];
  onColorClick: (color: ColorFilterOption) => void;
}

export function ColorsLegend(props: ColorLegendProps) {
  const { className, colors } = props;
  const theme = useTheme();

  return (
    <ColorsLegendWrapper className={className}>
      {colors.map((color, idx) => (
        <li className="sw-ml-4" key={color.value}>
          <Tooltip content={color.overlay}>
            <div>
              <Checkbox
                checked={color.selected}
                label={color.ariaLabel}
                onCheck={() => {
                  props.onColorClick(color);
                }}
              >
                <ColorRating
                  style={
                    color.selected
                      ? {
                          backgroundColor:
                            color.borderColor ??
                            themeColor(`bubble.${(idx + 1) as BubbleColorVal}`)({ theme }),
                          borderColor:
                            color.backgroundColor ??
                            themeContrast(`bubble.${(idx + 1) as BubbleColorVal}`)({ theme }),
                        }
                      : {}
                  }
                >
                  {color.label}
                </ColorRating>
              </Checkbox>
            </div>
          </Tooltip>
        </li>
      ))}
    </ColorsLegendWrapper>
  );
}

const ColorsLegendWrapper = styled.ul`
  ${tw`sw-flex`}
`;

const ColorRating = styled.div`
  width: 20px;
  height: 20px;
  line-height: 20px;
  border-radius: 50%;
  border: ${themeBorder()};
  ${tw`sw-flex sw-justify-center`}
  ${tw`sw-ml-1`}
`;
