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
import { themeColor } from '../../helpers/theme';
import { CustomIcon, IconProps } from './Icon';

export function ExecutionFlowIcon({ fill = 'currentColor', ...iconProps }: IconProps) {
  const theme = useTheme();
  const fillColor = themeColor(fill)({ theme });

  return (
    <CustomIcon {...iconProps}>
      <path
        clipRule="evenodd"
        d="M8.8928 7.89282C9.10919 7.67643 9.23076 7.38294 9.23076 7.07692C9.23076 6.7709 9.10919 6.47742 8.8928 6.26103C8.67642 6.04464 8.38293 5.92308 8.07691 5.92308C7.77089 5.92308 7.47741 6.04464 7.26102 6.26103C7.04463 6.47742 6.92307 6.7709 6.92307 7.07692C6.92307 7.38294 7.04463 7.67643 7.26102 7.89282C7.47741 8.1092 7.77089 8.23077 8.07691 8.23077C8.38293 8.23077 8.67642 8.1092 8.8928 7.89282ZM8.8928 3.96974C9.10919 3.75335 9.23076 3.45987 9.23076 3.15385C9.23076 2.84783 9.10919 2.55434 8.8928 2.33795C8.67642 2.12157 8.38293 2 8.07691 2C7.77089 2 7.47741 2.12157 7.26102 2.33795C7.04463 2.55434 6.92307 2.84783 6.92307 3.15385C6.92307 3.45987 7.04463 3.75335 7.26102 3.96974C7.47741 4.18613 7.77089 4.30769 8.07691 4.30769C8.38293 4.30769 8.67642 4.18613 8.8928 3.96974ZM9.54553 13.3917C9.15603 13.7812 8.62776 14 8.07692 14C7.52609 14 6.99782 13.7812 6.60832 13.3917C6.21882 13.0022 6 12.4739 6 11.9231C6 11.3722 6.21882 10.844 6.60832 10.4545C6.99782 10.065 7.52609 9.84615 8.07692 9.84615C8.62776 9.84615 9.15603 10.065 9.54553 10.4545C9.93503 10.844 10.1538 11.3722 10.1538 11.9231C10.1538 12.4739 9.93503 13.0022 9.54553 13.3917ZM7.58739 11.4335C7.71722 11.3037 7.89331 11.2308 8.07692 11.2308C8.26053 11.2308 8.43663 11.3037 8.56646 11.4335C8.69629 11.5634 8.76923 11.7395 8.76923 11.9231C8.76923 12.1067 8.69629 12.2828 8.56646 12.4126C8.43663 12.5424 8.26053 12.6154 8.07692 12.6154C7.89331 12.6154 7.71722 12.5424 7.58739 12.4126C7.45756 12.2828 7.38462 12.1067 7.38462 11.9231C7.38462 11.7395 7.45756 11.5634 7.58739 11.4335Z"
        fill={fillColor}
        fillRule="evenodd"
      />
    </CustomIcon>
  );
}
