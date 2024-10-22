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

export default function DefenseInDepth() {
  return (
    <>
      <h3>Defense-In-Depth</h3>
      <p>
        Applications and infrastructure benefit greatly from relying on multiple security mechanisms
        layered on top of each other. If one security mechanism fails, there is a high probability
        that the subsequent layers of security will successfully defend against the attack.
      </p>

      <p>A non-exhaustive list of these code protection ramparts includes the following:</p>
      <ul>
        <li>Minimizing the attack surface of the code</li>
        <li>Application of the principle of least privilege</li>
        <li>Validation and sanitization of data</li>
        <li>Encrypting incoming, outgoing, or stored data with secure cryptography</li>
        <li>Ensuring that internal errors cannot disrupt the overall runtime</li>
        <li>Separation of tasks and access to information</li>
      </ul>

      <p>
        Note that these layers must be simple enough to use in an everyday workflow. Security
        measures should not break usability.
      </p>
    </>
  );
}
