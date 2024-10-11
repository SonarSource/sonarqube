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

interface Props {
  className?: string;
}

export function OverviewQGNotComputedIcon({ className }: Readonly<Props>) {
  const theme = useTheme();

  return (
    <svg
      className={className}
      fill="none"
      height="168"
      role="img"
      viewBox="0 0 168 168"
      width="168"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path
        clipRule="evenodd"
        d="M149.542 26.472L141.248 37.2099C140.456 38.2345 140.645 39.7068 141.67 40.4983C142.695 41.2897 144.167 41.1007 144.959 40.076L153.253 29.3382C154.044 28.3135 153.855 26.8413 152.831 26.0498C151.806 25.2583 150.334 25.4473 149.542 26.472ZM137.915 45.3598C141.625 48.2252 146.955 47.5408 149.82 43.8312L158.114 33.0934C160.98 29.3837 160.295 24.0536 156.586 21.1883C152.876 18.3229 147.546 19.0072 144.681 22.7168L136.386 33.4547C133.521 37.1643 134.205 42.4944 137.915 45.3598Z"
        fill={themeColor('illustrationPrimary')({ theme })}
        fillRule="evenodd"
      />
      <path
        clipRule="evenodd"
        d="M149.385 57.9371C149.385 46.1503 139.83 36.5952 128.043 36.5952C116.257 36.5952 106.702 46.1503 106.702 57.9371C106.702 69.7238 116.257 79.2789 128.043 79.2789C139.83 79.2789 149.385 69.7238 149.385 57.9371ZM155.528 57.9371C155.528 42.7576 143.223 30.4523 128.043 30.4523C112.864 30.4523 100.559 42.7576 100.559 57.9371C100.559 73.1165 112.864 85.4218 128.043 85.4218C143.223 85.4218 155.528 73.1165 155.528 57.9371Z"
        fill={themeColor('illustrationPrimary')({ theme })}
        fillRule="evenodd"
      />
      <path
        clipRule="evenodd"
        d="M143.6 57.937C143.6 49.3459 136.635 42.3814 128.044 42.3814C119.453 42.3814 112.489 49.3459 112.489 57.937C112.489 66.5281 119.453 73.4925 128.044 73.4925C136.635 73.4925 143.6 66.528 143.6 57.937ZM149.743 57.937C149.743 45.9532 140.028 36.2385 128.044 36.2385C116.06 36.2385 106.346 45.9532 106.346 57.937C106.346 69.9207 116.06 79.6355 128.044 79.6355C140.028 79.6355 149.743 69.9207 149.743 57.937Z"
        fill={themeColor('illustrationShade')({ theme })}
        fillRule="evenodd"
      />
      <path d="M24 40L24 135H32L32 40H24Z" fill={themeColor('illustrationSecondary')({ theme })} />
      <path
        d="M38 56L53 56L53 48L38 48L38 56Z"
        fill={themeColor('illustrationSecondary')({ theme })}
      />
      <path
        d="M61 56L76 56L76 48L61 48L61 56Z"
        fill={themeColor('illustrationSecondary')({ theme })}
      />
      <path
        clipRule="evenodd"
        d="M88 67.5746H21V61.3297H88V67.5746Z"
        fill="var(--echoes-color-icon-subdued)"
        fillRule="evenodd"
      />
      <path
        clipRule="evenodd"
        d="M18 133C18 136.866 21.134 140 25 140H153C156.866 140 160 136.866 160 133V78H154V133C154 133.552 153.552 134 153 134H25C24.4477 134 24 133.552 24 133V44C24 43.4477 24.4477 43 25 43H72V37H25C21.134 37 18 40.134 18 44V133Z"
        fill="var(--echoes-color-icon-subdued)"
        fillRule="evenodd"
      />
      <path
        clipRule="evenodd"
        d="M69.2432 103.219L78.7954 93.6672L74.5527 89.4245L60.7578 103.219L74.5527 117.014L78.7954 112.771L69.2432 103.219Z"
        fill={themeColor('illustrationSecondary')({ theme })}
        fillRule="evenodd"
      />
      <path
        clipRule="evenodd"
        d="M108.906 103.219L99.3538 93.6672L103.596 89.4246L117.391 103.219L103.596 117.014L99.3538 112.771L108.906 103.219Z"
        fill={themeColor('illustrationSecondary')({ theme })}
        fillRule="evenodd"
      />
      <path
        clipRule="evenodd"
        d="M81.7179 119.862L91.0929 84.2365L96.8953 85.7635L87.5203 121.388L81.7179 119.862Z"
        fill={themeColor('illustrationSecondary')({ theme })}
        fillRule="evenodd"
      />
      <path
        d="M51 128.953C51 141.379 40.9264 151.453 28.5 151.453C16.0736 151.453 6 141.379 6 128.953C6 116.526 16.0736 106.453 28.5 106.453C40.9264 106.453 51 116.526 51 128.953Z"
        fill={themeColor('illustrationPrimary')({ theme })}
      />
      <path
        clipRule="evenodd"
        d="M25 131.953V113.953H31V131.953H25Z"
        fill={themeColor('backgroundSecondary')({ theme })}
        fillRule="evenodd"
      />
      <path
        clipRule="evenodd"
        d="M25 142.453L25 136.453L31 136.453L31 142.453L25 142.453Z"
        fill={themeColor('backgroundSecondary')({ theme })}
        fillRule="evenodd"
      />
      <path
        d="M105.398 35.2089L90.7238 24.2245L95.8489 19.5626L105.398 35.2089Z"
        fill={themeColor('illustrationPrimary')({ theme })}
      />
      <path
        d="M99 41.5242L88.5 44.9883L88.5 38.0601L99 41.5242Z"
        fill={themeColor('illustrationPrimary')({ theme })}
      />
      <path
        d="M139.228 86.8865L147.417 92.2112L141.826 96.3028L139.228 86.8865Z"
        fill={themeColor('illustrationPrimary')({ theme })}
      />
      <path
        d="M132 88.5242L135.464 105.024H128.536L132 88.5242Z"
        fill={themeColor('illustrationPrimary')({ theme })}
      />
      <path
        d="M114 29.5242L110.536 19.7742L117.464 19.7742L114 29.5242Z"
        fill={themeColor('illustrationPrimary')({ theme })}
      />
    </svg>
  );
}
