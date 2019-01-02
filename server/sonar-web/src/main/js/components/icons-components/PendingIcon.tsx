/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import Icon, { IconProps } from './Icon';
import * as theme from '../../app/theme';

export default function PendingIcon({ className, fill = theme.gray67, size }: IconProps) {
  return (
    <Icon className={className} size={size}>
      <g transform="matrix(0.0364583,0,0,0.0364583,1,-0.166667)">
        <path
          d="M224,136L224,248C224,250.333 223.25,252.25 221.75,253.75C220.25,255.25 218.333,256 216,256L136,256C133.667,256 131.75,255.25 130.25,253.75C128.75,252.25 128,250.333 128,248L128,232C128,229.667 128.75,227.75 130.25,226.25C131.75,224.75 133.667,224 136,224L192,224L192,136C192,133.667 192.75,131.75 194.25,130.25C195.75,128.75 197.667,128 200,128L216,128C218.333,128 220.25,128.75 221.75,130.25C223.25,131.75 224,133.667 224,136ZM328,224C328,199.333 321.917,176.583 309.75,155.75C297.583,134.917 281.083,118.417 260.25,106.25C239.417,94.083 216.667,88 192,88C167.333,88 144.583,94.083 123.75,106.25C102.917,118.417 86.417,134.917 74.25,155.75C62.083,176.583 56,199.333 56,224C56,248.667 62.083,271.417 74.25,292.25C86.417,313.083 102.917,329.583 123.75,341.75C144.583,353.917 167.333,360 192,360C216.667,360 239.417,353.917 260.25,341.75C281.083,329.583 297.583,313.083 309.75,292.25C321.917,271.417 328,248.667 328,224ZM384,224C384,258.833 375.417,290.958 358.25,320.375C341.083,349.792 317.792,373.083 288.375,390.25C258.958,407.417 226.833,416 192,416C157.167,416 125.042,407.417 95.625,390.25C66.208,373.083 42.917,349.792 25.75,320.375C8.583,290.958 0,258.833 0,224C0,189.167 8.583,157.042 25.75,127.625C42.917,98.208 66.208,74.917 95.625,57.75C125.042,40.583 157.167,32 192,32C226.833,32 258.958,40.583 288.375,57.75C317.792,74.917 341.083,98.208 358.25,127.625C375.417,157.042 384,189.167 384,224Z"
          style={{ fill }}
        />
      </g>
    </Icon>
  );
}
