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

import { CustomIcon, IconProps } from '~design-system';

/*
 * Temporary Icon. To remove when echoes gets the proper icon.
 */
export function SheildCheckIcon({ fill, ...iconProps }: Readonly<IconProps>) {
  return (
    <CustomIcon viewBox="0 0 36 36" width={20} height={20} {...iconProps}>
      <g>
        <path
          d="M12.6427 29.0627C9.55385 28.2849 7.00385 26.5127 4.99274 23.746C2.98163 20.9793 1.97607 17.9071 1.97607 14.5293V6.396L12.6427 2.396L23.3094 6.396V15.1293C23.3094 15.3293 23.2983 15.5404 23.2761 15.7627H20.5761C20.5983 15.5404 20.615 15.3293 20.6261 15.1293C20.6372 14.9293 20.6427 14.7293 20.6427 14.5293V8.22933L12.6427 5.22933L4.64274 8.22933V14.5293C4.64274 17.2182 5.3983 19.6627 6.90941 21.8627C8.42052 24.0627 10.3316 25.5293 12.6427 26.2627V29.0627Z"
          fill={fill}
        />
        <path
          fillRule="evenodd"
          clipRule="evenodd"
          d="M9.10304 14.4089C8.97046 14.5067 8.83547 14.6015 8.69818 14.6931C8.08185 15.1045 7.41893 15.4528 6.71826 15.7292C7.41893 16.0055 8.08185 16.3538 8.69817 16.7652C8.83547 16.8568 8.97046 16.9516 9.10303 17.0494C9.9496 17.674 10.6979 18.4223 11.3225 19.2689C11.4203 19.4015 11.5151 19.5364 11.6067 19.6737C12.0181 20.2901 12.3664 20.953 12.6427 21.6536C12.9191 20.953 13.2674 20.2901 13.6788 19.6737C13.7704 19.5364 13.8652 19.4015 13.963 19.2689C14.5876 18.4223 15.3359 17.674 16.1824 17.0494C16.315 16.9516 16.45 16.8568 16.5873 16.7652C17.2036 16.3538 17.8665 16.0055 18.5672 15.7292C17.8665 15.4528 17.2036 15.1045 16.5873 14.6931C16.45 14.6015 16.315 14.5067 16.1824 14.4089C15.3359 13.7843 14.5876 13.036 13.963 12.1895C13.8652 12.0569 13.7704 11.9219 13.6788 11.7846C13.2674 11.1683 12.9191 10.5054 12.6427 9.80469C12.3664 10.5054 12.0181 11.1683 11.6067 11.7846C11.5151 11.9219 11.4203 12.0569 11.3225 12.1895C10.6979 13.036 9.9496 13.7843 9.10304 14.4089ZM11.0178 15.7292C11.6072 16.2208 12.1512 16.7647 12.6427 17.3541C13.1343 16.7647 13.6782 16.2208 14.2677 15.7292C13.6782 15.2376 13.1343 14.6937 12.6427 14.1042C12.1512 14.6937 11.6072 15.2376 11.0178 15.7292Z"
          fill={fill}
        />
        <path
          d="M19.9266 29.0846L14.5933 23.7513L16.4599 21.8846L19.9266 25.3513L28.7266 16.5513L30.5933 18.4179L19.9266 29.0846Z"
          fill={fill}
        />
      </g>
    </CustomIcon>
  );
}
