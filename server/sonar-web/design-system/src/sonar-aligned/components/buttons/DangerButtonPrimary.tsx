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
import { OPACITY_20_PERCENT, themeBorder, themeColor, themeContrast } from '../../../helpers';
import { Button } from './Button';

/**
 * @deprecated Use Button from Echoes instead with the `variety` prop set
 * to ButtonVariety.Danger to have the same look and feel.
 *
 * Some of the props have changed or been renamed:
 * - `blurAfterClick` is now `shouldBlurAfterClick`
 * - `disabled` is now `isDisabled`, note that a Echoes Tooltip won't work
 * on a disabled button, use a text notice or ToggleTip next to the disabled button instead.
 * - `icon` is now replace by `prefix` which works the same way
 * - `preventDefault` is now `shouldPreventDefault`
 * - `stopPropagation` is now `shouldStopPropagation`
 *
 * The button can't be used as a link anymore, and all props related to links have been dropped.
 * Use a real Echoes Link instead.
 *
 * See the {@link https://xtranet-sonarsource.atlassian.net/wiki/spaces/Platform/pages/3382706231/Button | Migration Guide} for more information.
 */
export const DangerButtonPrimary = styled(Button)`
  --background: ${themeColor('dangerButton')};
  --backgroundHover: ${themeColor('dangerButtonHover')};
  --color: ${themeContrast('dangerButton')};
  --focus: ${themeColor('dangerButtonFocus', OPACITY_20_PERCENT)};
  --border: ${themeBorder('default', 'transparent')};
`;
