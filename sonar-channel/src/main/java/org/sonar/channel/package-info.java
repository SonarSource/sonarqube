/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
/**
 * Provides a basic framework to sequentially read any kind of character stream in order to feed a generic OUTPUT. 
 * 
 * This framework can used for instance in order to :
 * <ul>
 *   <li>Create a lexer in charge to generate a list of tokens from a character stream</li>
 *   <li>Create a source code syntax highligther in charge to decorate a source code with HTML tags</li>
 *   <li>Create a javadoc generator</li>
 *   <li>...</li>
 * </ul> 
 * 
 * The entry point of this framework is the {@link org.sonar.channel.ChannelDispatcher} class. 
 * This class must be initialized with a {@link org.sonar.channel.CodeReader} and a list of {@link org.sonar.channel.Channel}.
 * 
 * The {@link org.sonar.channel.CodeReader} encapsulates any character stream in order to provide all mechanisms to Channels  
 * in order to look ahead and look behind the current reading cursor position. 
 * 
 * A {@link org.sonar.channel.Channel} is in charge to consume the character stream through the CodeReader in order to feed
 * the OUTPUT.
 */
package org.sonar.channel;

