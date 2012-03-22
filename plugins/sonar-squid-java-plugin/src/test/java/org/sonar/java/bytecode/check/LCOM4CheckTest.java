/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.bytecode.check;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasToString;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.everyItem;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.sonar.api.utils.WildcardPattern;
import org.sonar.java.CheckMessages;
import org.sonar.java.PatternUtils;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.SquidTestUtils;
import org.sonar.java.bytecode.BytecodeScanner;
import org.sonar.java.squid.JavaSquidConfiguration;
import org.sonar.squid.Squid;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceFile;
import org.sonar.squid.indexer.QueryByMeasure;
import org.sonar.squid.indexer.QueryByMeasure.Operator;
import org.sonar.squid.indexer.QueryByType;
import org.sonar.squid.measures.Metric;

public class LCOM4CheckTest {

	private LCOM4Check check;

	private Squid squid;
	
	int maxLcom4 = 1;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		check = new LCOM4Check();
	}

	@Test 
	public void shouldIncludeEverything() {
		check.setForClasses(null);
		WildcardPattern[] includedClasses = check.getForClasses();
		assertThat(includedClasses, hasItemInArray(hasToString(is("**"))));
		assertThat(includedClasses.length, is(1));
	}
	
	@Test
	public void shouldAddMessageIfMaxViolated() {
		Collection<SourceCode> search = findSuspects(LCOM4Check.MAX_LCOM4_DEFAULT);
	    assertOnlyLCom4CheckMessages(search);
	    assertThat(search.size(), is(2));
	}

	@Test
	public void shouldNotAddMessageIfMaxNotViolated() {
		assertThat(findSuspects(2).size(), is(0));
	}

	@Test
	public void shouldNotAddMessageIfPatternMatchesOne() {
		Collection<SourceCode> search = findSuspects(1, "**/LCOM4Exclusions");
	    int lCom4CheckMessages = assertOnlyLCom4CheckMessages(search);
	    assertThat(lCom4CheckMessages, is(1));
	}

	@Test
	public void shouldNotAddMessageIfPatternMatchesNone() {
		Collection<SourceCode> search = findSuspects(1, "**/Haha");
		int lCom4CheckMessages = assertOnlyLCom4CheckMessages(search);
		assertThat(lCom4CheckMessages, is(0));
	}
	
	@Test
	public void shouldMessageIfMultiplePatternMatch() {
		Collection<SourceCode> search = findSuspects(1, "**/XYZ, **/LCOM4Exclusions, **/ExclusionOfFieldNamesFromLcom4Calculation");
		int lCom4CheckMessages = assertOnlyLCom4CheckMessages(search);
		assertThat(lCom4CheckMessages, is(2));
	}
	
	private Collection<SourceCode> findSuspects(int currentLcom4) {
		return findSuspects(currentLcom4, "**");
	}

	private Collection<SourceCode> findSuspects(int currentLcom4, String pattern) {
		measure(currentLcom4, pattern);
	    return queryForSuspects(currentLcom4);
	}

	private void measure(int maxLcom4, String patterns) {
			check.setMax(maxLcom4);
			check.setForClasses(PatternUtils.createPatterns(patterns));

			JavaSquidConfiguration conf = new JavaSquidConfiguration();
		    conf.addFieldToExcludeFromLcom4Calculation("LOG");
			squid = new Squid(conf);
			squid.registerVisitor(check);
		    squid.register(JavaAstScanner.class).scanDirectory(SquidTestUtils.getFile("/bytecode/lcom4/src"));
		    squid.register(BytecodeScanner.class).scanDirectory(SquidTestUtils.getFile("/bytecode/lcom4/bin"));
		}

	private int assertOnlyLCom4CheckMessages(Collection<SourceCode> search) {
		int lcomMessageCount = 0;
		for (SourceCode sourceCode : search) {
			Set<CheckMessage> messages = ((SourceFile) sourceCode).getCheckMessages();
			for (CheckMessage checkMessage : messages) {
				assertThat(checkMessage.getText(Locale.getDefault()), containsString("LCOM4"));
				lcomMessageCount++;
				assertThat(checkMessage.getLine(), is(1));
				assertThat(checkMessage.getCost(), is(1.0));
			}
	    }
		return lcomMessageCount;
	}

	private Collection<SourceCode> queryForSuspects(int currentLcom4) {
		Collection<SourceCode> search = squid.search(new QueryByType(SourceFile.class), new QueryByMeasure(Metric.LCOM4, Operator.GREATER_THAN, currentLcom4));
		return search;
	}
	
	
}
