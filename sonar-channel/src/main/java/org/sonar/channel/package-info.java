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

