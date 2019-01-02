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
/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */
package net.sourceforge.pmd.cpd;

import java.io.IOException;

/**
 * A tokenizer is responsible to return a token list for the provided input file (see {@link SourceCode#getFileName()}.
 * Tokens are basically list of non empty words in a file but you can also do some "anonymization" to ignore litteral differences.
 * 
 * For example if you have a first file:
 * <pre>
 * public class MyClass1 {
 *   int foo1;
 * }
 * </pre>
 * and a second file:
 * <pre>
 * public class MyClass2 {
 *   int foo2;
 * }
 * </pre>
 * Then in both cases your tokenizer could return the following (line, image) list:
 * <pre>(1,public),(1,class),(1,LITERAL),(1,{),(2,int),(2,LITERAL),(2,;),(3,})</pre>
 * in this case the two files will be considered as duplicate.
 * 
 * @since 2.2
 * @deprecated since 5.5
 */
@Deprecated
public interface Tokenizer {

  void tokenize(SourceCode sourceFile, Tokens tokenEntries) throws IOException;

}
