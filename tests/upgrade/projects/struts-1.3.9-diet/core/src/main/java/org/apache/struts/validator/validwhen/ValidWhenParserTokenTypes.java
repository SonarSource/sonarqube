// $ANTLR 2.7.2: "ValidWhenParser.g" -> "ValidWhenParser.java"$

/*
 * $Id: ValidWhenParserTokenTypes.java 504715 2007-02-07 22:10:26Z bayard $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.struts.validator.validwhen;

import java.util.Stack;
import org.apache.commons.validator.util.ValidatorUtils;


public interface ValidWhenParserTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int DECIMAL_LITERAL = 4;
	int HEX_LITERAL = 5;
	int OCTAL_LITERAL = 6;
	int STRING_LITERAL = 7;
	int IDENTIFIER = 8;
	int LBRACKET = 9;
	int RBRACKET = 10;
	int LITERAL_null = 11;
	int THIS = 12;
	int LPAREN = 13;
	int RPAREN = 14;
	int ANDSIGN = 15;
	int ORSIGN = 16;
	int EQUALSIGN = 17;
	int GREATERTHANSIGN = 18;
	int GREATEREQUALSIGN = 19;
	int LESSTHANSIGN = 20;
	int LESSEQUALSIGN = 21;
	int NOTEQUALSIGN = 22;
	int WS = 23;
}
