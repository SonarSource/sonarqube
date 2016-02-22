// $ANTLR 2.7.2: "ValidWhenParser.g" -> "ValidWhenParser.java"$

/*
 * $Id: ValidWhenParser.java 504715 2007-02-07 22:10:26Z bayard $
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


import antlr.TokenBuffer;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.ANTLRException;
import antlr.LLkParser;
import antlr.Token;
import antlr.TokenStream;
import antlr.RecognitionException;
import antlr.NoViableAltException;
import antlr.MismatchedTokenException;
import antlr.SemanticException;
import antlr.ParserSharedInputState;
import antlr.collections.impl.BitSet;

public class ValidWhenParser extends antlr.LLkParser       implements ValidWhenParserTokenTypes
 {
Stack argStack = new Stack();
Object form;
int index;
String value;

    public void setForm(Object f) { form = f; };
    public void setIndex (int i) { index = i; };
    public void setValue (String v) { value = v; };

    public boolean getResult() {
       return ((Boolean)argStack.peek()).booleanValue();
    }

    private final int LESS_EQUAL=0;
    private final int LESS_THAN=1;
    private final int EQUAL=2;
    private final int GREATER_THAN=3;
    private final int GREATER_EQUAL=4;
    private final int NOT_EQUAL=5;
    private final int AND=6;
    private final int OR=7;

    private  boolean evaluateComparison (Object v1, Object compare, Object v2) {
        boolean intCompare = true;
	if ((v1 == null) || (v2 == null)) {
		if (String.class.isInstance(v1)) {
			if (((String) v1).length() == 0) {
				v1 = null;
			}
		}
		if (String.class.isInstance(v2)) {
			if (((String) v2).length() == 0) {
				v2 = null;
			}
		}
		switch (((Integer)compare).intValue()) {
		case LESS_EQUAL:
		case GREATER_THAN:
		case LESS_THAN:
		case GREATER_EQUAL:
			return false;
		case EQUAL:
		    return (v1 == v2);
		case NOT_EQUAL:
		    return (v1 != v2);
		}
	}
      if ((Integer.class.isInstance(v1) ||
           String.class.isInstance(v1)) &&
	    (Integer.class.isInstance(v2) ||
           String.class.isInstance(v2))) {
	    intCompare = true;
      } else {
	    intCompare = false;
	}
	if (intCompare) {
	    try {
		int v1i = 0, v2i = 0;
		if (Integer.class.isInstance(v1)) {
		    v1i = ((Integer)v1).intValue();
		} else {
		    v1i = Integer.parseInt((String) v1);
		}
		if (Integer.class.isInstance(v2)) {
		    v2i = ((Integer)v2).intValue();
		} else {
		    v2i = Integer.parseInt((String) v2);
		}
		switch (((Integer)compare).intValue()) {
		case LESS_EQUAL:
		    return (v1i <= v2i);

		case LESS_THAN:
		    return (v1i < v2i);

		case EQUAL:
		    return (v1i == v2i);

		case GREATER_THAN:
		    return (v1i > v2i);

		case GREATER_EQUAL:
		    return (v1i >= v2i);

		case NOT_EQUAL:
		    return (v1i != v2i);
		}
	    } catch (NumberFormatException ex) {};
	}
	String v1s = "", v2s = "";

	if (Integer.class.isInstance(v1)) {
	    v1s = ((Integer)v1).toString();
	} else {
	    v1s = (String) v1;
	}

	if (Integer.class.isInstance(v2)) {
	    v2s = ((Integer)v2).toString();
	} else {
	    v2s = (String) v2;
	}

	int res = v1s.compareTo(v2s);
	switch (((Integer)compare).intValue()) {
	case LESS_EQUAL:
	    return (res <= 0);

	case LESS_THAN:
	    return (res < 0);

	case EQUAL:
	    return (res == 0);

	case GREATER_THAN:
	    return (res > 0);

	case GREATER_EQUAL:
	    return (res >= 0);

	case NOT_EQUAL:
	    return (res != 0);
	}
	return true;
    }


protected ValidWhenParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public ValidWhenParser(TokenBuffer tokenBuf) {
  this(tokenBuf,6);
}

protected ValidWhenParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public ValidWhenParser(TokenStream lexer) {
  this(lexer,6);
}

public ValidWhenParser(ParserSharedInputState state) {
  super(state,6);
  tokenNames = _tokenNames;
}

	public final void integer() throws RecognitionException, TokenStreamException {
		
		Token  d = null;
		Token  h = null;
		Token  o = null;
		
		switch ( LA(1)) {
		case DECIMAL_LITERAL:
		{
			d = LT(1);
			match(DECIMAL_LITERAL);
			argStack.push(Integer.decode(d.getText()));
			break;
		}
		case HEX_LITERAL:
		{
			h = LT(1);
			match(HEX_LITERAL);
			argStack.push(Integer.decode(h.getText()));
			break;
		}
		case OCTAL_LITERAL:
		{
			o = LT(1);
			match(OCTAL_LITERAL);
			argStack.push(Integer.decode(o.getText()));
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void string() throws RecognitionException, TokenStreamException {
		
		Token  str = null;
		
		str = LT(1);
		match(STRING_LITERAL);
		argStack.push(str.getText().substring(1, str.getText().length()-1));
	}
	
	public final void identifier() throws RecognitionException, TokenStreamException {
		
		Token  str = null;
		
		str = LT(1);
		match(IDENTIFIER);
		argStack.push(str.getText());
	}
	
	public final void field() throws RecognitionException, TokenStreamException {
		
		
		if ((LA(1)==IDENTIFIER) && (LA(2)==LBRACKET) && (LA(3)==RBRACKET) && (LA(4)==IDENTIFIER)) {
			identifier();
			match(LBRACKET);
			match(RBRACKET);
			identifier();
			
			Object i2 = argStack.pop();
			Object i1 = argStack.pop();
			argStack.push(ValidatorUtils.getValueAsString(form, i1 + "[" + index + "]" + i2));
			
		}
		else if ((LA(1)==IDENTIFIER) && (LA(2)==LBRACKET) && ((LA(3) >= DECIMAL_LITERAL && LA(3) <= OCTAL_LITERAL)) && (LA(4)==RBRACKET) && (LA(5)==IDENTIFIER)) {
			identifier();
			match(LBRACKET);
			integer();
			match(RBRACKET);
			identifier();
			
			Object i5 = argStack.pop();
			Object i4 = argStack.pop();
			Object i3 = argStack.pop();
			argStack.push(ValidatorUtils.getValueAsString(form, i3 + "[" + i4 + "]" + i5));
			
		}
		else if ((LA(1)==IDENTIFIER) && (LA(2)==LBRACKET) && ((LA(3) >= DECIMAL_LITERAL && LA(3) <= OCTAL_LITERAL)) && (LA(4)==RBRACKET) && (LA(5)==LBRACKET)) {
			identifier();
			match(LBRACKET);
			integer();
			match(RBRACKET);
			match(LBRACKET);
			
			Object i7 = argStack.pop();
			Object i6 = argStack.pop();
			argStack.push(ValidatorUtils.getValueAsString(form, i6 + "[" + i7 + "]"));
			
		}
		else if ((LA(1)==IDENTIFIER) && (LA(2)==LBRACKET) && (LA(3)==RBRACKET) && (_tokenSet_0.member(LA(4)))) {
			identifier();
			match(LBRACKET);
			match(RBRACKET);
			
			Object i8 = argStack.pop();
			argStack.push(ValidatorUtils.getValueAsString(form, i8 + "[" + index + "]"));
			
		}
		else if ((LA(1)==IDENTIFIER) && (_tokenSet_0.member(LA(2)))) {
			identifier();
			
			Object i9 = argStack.pop();
			argStack.push(ValidatorUtils.getValueAsString(form, (String)i9));
			
		}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		
	}
	
	public final void literal() throws RecognitionException, TokenStreamException {
		
		
		switch ( LA(1)) {
		case DECIMAL_LITERAL:
		case HEX_LITERAL:
		case OCTAL_LITERAL:
		{
			integer();
			break;
		}
		case STRING_LITERAL:
		{
			string();
			break;
		}
		case LITERAL_null:
		{
			match(LITERAL_null);
			argStack.push(null);
			break;
		}
		case THIS:
		{
			match(THIS);
			argStack.push(value);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void value() throws RecognitionException, TokenStreamException {
		
		
		switch ( LA(1)) {
		case IDENTIFIER:
		{
			field();
			break;
		}
		case DECIMAL_LITERAL:
		case HEX_LITERAL:
		case OCTAL_LITERAL:
		case STRING_LITERAL:
		case LITERAL_null:
		case THIS:
		{
			literal();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void expression() throws RecognitionException, TokenStreamException {
		
		
		expr();
		match(Token.EOF_TYPE);
	}
	
	public final void expr() throws RecognitionException, TokenStreamException {
		
		
		if ((LA(1)==LPAREN) && (_tokenSet_1.member(LA(2)))) {
			match(LPAREN);
			comparisonExpression();
			match(RPAREN);
		}
		else if ((LA(1)==LPAREN) && (LA(2)==LPAREN)) {
			match(LPAREN);
			joinedExpression();
			match(RPAREN);
		}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		
	}
	
	public final void comparisonExpression() throws RecognitionException, TokenStreamException {
		
		
		value();
		comparison();
		value();
		
			    Object v2 = argStack.pop();
			    Object comp = argStack.pop();
		Object v1 = argStack.pop();
		argStack.push(new Boolean(evaluateComparison(v1, comp, v2)));
		
	}
	
	public final void joinedExpression() throws RecognitionException, TokenStreamException {
		
		
		expr();
		join();
		expr();
		
		Boolean v1 = (Boolean) argStack.pop();
		Integer join = (Integer) argStack.pop();
		Boolean v2 = (Boolean) argStack.pop();
		if (join.intValue() == AND) {
		argStack.push(new Boolean(v1.booleanValue() && v2.booleanValue()));
		} else {
		argStack.push(new Boolean(v1.booleanValue() || v2.booleanValue()));
		}
		
	}
	
	public final void join() throws RecognitionException, TokenStreamException {
		
		
		switch ( LA(1)) {
		case ANDSIGN:
		{
			match(ANDSIGN);
			argStack.push(new Integer(AND));
			break;
		}
		case ORSIGN:
		{
			match(ORSIGN);
			argStack.push(new Integer(OR));
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void comparison() throws RecognitionException, TokenStreamException {
		
		
		switch ( LA(1)) {
		case EQUALSIGN:
		{
			match(EQUALSIGN);
			argStack.push(new Integer(EQUAL));
			break;
		}
		case GREATERTHANSIGN:
		{
			match(GREATERTHANSIGN);
			argStack.push(new Integer(GREATER_THAN));
			break;
		}
		case GREATEREQUALSIGN:
		{
			match(GREATEREQUALSIGN);
			argStack.push(new Integer(GREATER_EQUAL));
			break;
		}
		case LESSTHANSIGN:
		{
			match(LESSTHANSIGN);
			argStack.push(new Integer(LESS_THAN));
			break;
		}
		case LESSEQUALSIGN:
		{
			match(LESSEQUALSIGN);
			argStack.push(new Integer(LESS_EQUAL));
			break;
		}
		case NOTEQUALSIGN:
		{
			match(NOTEQUALSIGN);
			argStack.push(new Integer(NOT_EQUAL));
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"DECIMAL_LITERAL",
		"HEX_LITERAL",
		"OCTAL_LITERAL",
		"STRING_LITERAL",
		"IDENTIFIER",
		"LBRACKET",
		"RBRACKET",
		"\"null\"",
		"THIS",
		"LPAREN",
		"RPAREN",
		"\"and\"",
		"\"or\"",
		"EQUALSIGN",
		"GREATERTHANSIGN",
		"GREATEREQUALSIGN",
		"LESSTHANSIGN",
		"LESSEQUALSIGN",
		"NOTEQUALSIGN",
		"WS"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 8273920L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 6640L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	
	}
