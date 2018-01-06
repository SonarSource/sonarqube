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
// @flow
import React from 'react';

const DEFAULT_COLOR = '#2D88C0';

const ICONS /*: Object */ = {
  dir: (color = '#F90') => (
    <path
      fill={color}
      strokeLinejoin="round"
      d="M14 12.286V5.703c0-.198-.058-.36-.195-.5S13.512 5 13.315 5H6.704c-.196 0-.36-.075-.5-.214-.136-.14-.203-.312-.203-.51v-.57c0-.2-.07-.363-.207-.502C5.655 3.064 5.487 3 5.29 3H2.707c-.196 0-.363.065-.5.204-.137.14-.206.302-.206.5v8.582c0 .2.07.367.206.506.137.14.304.208.5.208h10.61c.196 0 .352-.07.49-.208.137-.14.194-.307.194-.506zm1-6.598v6.65c0 .458-.152.83-.475 1.16-.324.326-.7.502-1.15.502H2.647c-.452 0-.84-.175-1.162-.503-.324-.328-.486-.7-.486-1.158V3.654c0-.457.162-.842.486-1.17C1.81 2.158 2.196 2 2.648 2h2.7c.45 0 .84.157 1.164.485.324.328.488.714.488 1.17V4h6.373c.452 0 .83.174 1.152.5.323.33.475.73.475 1.187z"
    />
  ),
  trk: (color = DEFAULT_COLOR) => (
    <path
      fill={color}
      strokeLinejoin="round"
      d="M14.985 13.988L1 14.005 1.02 5h13.966v8.988zM1.998 5.995l.006 7.02L14.022 13 14 6.004l-12.002-.01zM3 4.5V4h9.996l.004.5h1l-.005-1.497-11.98.003L2 4.5zm1-2v-.504h8.002L12 2.5h1l-.004-1.495H3.003L3 2.5z"
    />
  ),
  brc: (color = DEFAULT_COLOR) => (
    <g fill={color}>
      <path d="M16 16H6v-6h10v6zm-9-1h8v-4H7v4zM7 9h8v1H7zM8 8h6v1H8z" />
      <path d="M5 12H1V5h12v2h1V4H0v9h5zM3 1h8v.5h1V0H2v1.5h1zM2 3h10v.5h1V2H1v1.5h1z" />
    </g>
  ),
  uts: (color = DEFAULT_COLOR) => (
    <g fill={color}>
      <path d="M3 14h10V6H9V2H3zm7.012-9h3.008c-.012-.674-.78-1.258-1.27-1.752-.488-.495-.973-1.243-1.75-1.24v2.96zM14 4.995V15H2V1l7.997.02c1.013-.03 1.57.893 2.239 1.555.667.663 1.75 1.47 1.763 2.42z" />
      <path d="M7 8l-3 2.5L7 13zM8 13l3-2.5L8 8z" />
    </g>
  ),
  fil: (color = DEFAULT_COLOR) => (
    <g fill={color}>
      <path d="M3 14h10v-8h-4v-4h-6zM10.012 5h3.008c-0.012-0.674-0.78-1.258-1.27-1.752-0.488-0.495-0.973-1.243-1.75-1.24v2.96zM14 4.995v10.005h-12v-14l7.997 0.020c1.013-0.030 1.57 0.893 2.239 1.555 0.667 0.663 1.75 1.47 1.763 2.42z" />
      <path d="M4 11h8v1h-8zM4 9h8v1h-8z" />
    </g>
  ),
  lib: (color = DEFAULT_COLOR) => (
    <path
      fill={color}
      strokeLinejoin="round"
      d="M1 13h4V3H1zm3-1H2v-2h2v2zM2 4h2v4H2zM6 13h4V3H6zm3-1H7v-2h2v2zM7 4h2v4H7zM11 13h4V3h-4zm3-1h-2v-2h2v2zm-2-8h2v4h-2z"
    />
  ),
  vw: (color = DEFAULT_COLOR) => (
    <g fill={color} fillRule="evenodd" strokeLinejoin="round">
      <path d="M1.016 14.97V1.015H14.97V14.97H1.015zm1-1H13.97V2.015H2.015V13.97z" />
      <path d="M3.006 7V3.006H7V7H3.006zm1-1H6V4.006H4.006V6zM9 7V3.015h3.985V7H9zm1-1h1.985V4.015H10V6zM3.004 12.996V9H7v3.996H3.004zm1-1H6V10H4.004v1.996zM9 12.997V9h3.997v3.997H9zm1-1h1.997V10H10v1.997z" />
    </g>
  ),
  svw: (color = DEFAULT_COLOR) => (
    <g fill={color}>
      <path d="M13 7.2V1H1v12h7v1H0V0h14v7.2" />
      <path d="M2 6V2h4v4H2zm1-1h2V3H3v2zm5 1V2h4v4H8zm1-1h2V3H9v2zm-7 7V8h4v4H2zm1-1h2V9H3v2zM16 16H7V7h9v9zm-8-1h7V8H8v7z" />
      <path d="M9 9h2v2H9zM12 9h2v2h-2zM9 12h2v2H9zM12 12h2v2h-2z" />
    </g>
  ),
  dev: (color = DEFAULT_COLOR) => (
    <path
      fill={color}
      strokeLinejoin="round"
      d="M7.974 8.02c-.938 0-1.82-.36-2.482-1.017-.663-.655-1.028-1.527-1.028-2.455 0-.927.365-1.8 1.028-2.455.663-.656 1.544-1.017 2.482-1.017.937 0 1.82.36 2.482 1.017.662.656 1.027 1.528 1.027 2.455 0 .928-.365 1.8-1.027 2.455C9.793 7.66 8.91 8.02 7.974 8.02zm0-5.778c-1.286 0-2.332 1.034-2.332 2.306s1.046 2.307 2.332 2.307c1.285 0 2.332-1.035 2.332-2.307S9.258 2.242 7.974 2.242zm3.534 6.418c.127.016.243.045.348.086.17.066.302.146.406.246.132.124.253.282.36.47.126.218.226.442.3.668.08.253.15.535.206.838.056.313.095.604.113.867.02.28.03.57.03.862 0 .532-.174.758-.306.882-.142.132-.397.31-.973.31H3.948c-.233 0-.437-.03-.606-.09-.14-.05-.26-.123-.366-.222-.13-.123-.306-.35-.306-.88 0-.294.01-.584.03-.863.018-.263.056-.554.112-.867.055-.303.125-.585.207-.838.073-.226.173-.45.298-.667.108-.19.23-.347.36-.47.106-.1.238-.18.407-.247.105-.04.22-.07.348-.086.202.13.432.277.683.435.342.217.756.4 1.265.564.523.166 1.06.25 1.59.25.534 0 1.07-.084 1.592-.25.51-.164.923-.348 1.266-.565.25-.158.48-.304.682-.435zm-.244-1.18c-.055 0-.184.066-.387.196-.202.13-.43.276-.685.437-.255.16-.586.307-.994.437-.408.13-.818.196-1.23.196-.41 0-.82-.065-1.228-.196-.408-.13-.74-.276-.993-.437-.255-.16-.484-.306-.686-.437-.202-.13-.33-.196-.386-.196-.374 0-.716.06-1.026.183-.31.12-.572.283-.787.487-.213.203-.404.45-.57.737-.165.288-.297.584-.395.888-.098.303-.18.633-.244.988-.063.355-.106.685-.128.992-.02.306-.032.62-.032.942 0 .73.224 1.304.672 1.726.448.42 1.043.632 1.785.632h8.044c.743 0 1.34-.21 1.787-.633.447-.42.67-.996.67-1.725 0-.32-.01-.635-.03-.942-.022-.307-.065-.637-.13-.992-.064-.355-.146-.685-.244-.988-.098-.304-.23-.6-.395-.888-.166-.288-.356-.534-.57-.737-.216-.204-.478-.366-.788-.487-.31-.122-.652-.183-1.026-.183z"
    />
  ),
  app: (color = '#4A9ED5') => (
    <g fill="none" stroke={color} strokeMiterlimit={10}>
      <circle cx="3" cy="3" r="1.5" />
      <circle cx="8" cy="3" r="1.5" />
      <circle cx="13" cy="3" r="1.5" />
      <circle cx="3" cy="8" r="1.5" />
      <circle cx="8" cy="8" r="1.5" />
      <circle cx="13" cy="8" r="1.5" />
      <circle cx="3" cy="13" r="1.5" />
      <circle cx="8" cy="13" r="1.5" />
      <circle cx="13" cy="13" r="1.5" />
    </g>
  )
};

ICONS.pac = ICONS.dir;
ICONS.dev_prj = ICONS.trk;
ICONS.cla = ICONS.uts;

/*:: type Props = { className?: string, color?: string, qualifier: string, size?: number }; */

export default function QualifierIcon({ className, color, qualifier, size = 16 } /*: Props */) {
  const icon = ICONS[qualifier.toLowerCase()];
  if (!icon) {
    return null;
  }

  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      height={size}
      width={size}
      viewBox="0 0 16 16">
      {icon(color)}
    </svg>
  );
}
