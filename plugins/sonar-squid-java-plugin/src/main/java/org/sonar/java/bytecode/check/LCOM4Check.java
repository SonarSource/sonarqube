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

import org.sonar.api.utils.WildcardPattern;
import org.sonar.check.Cardinality;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.PatternUtils;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.measures.Metric;
import org.sonar.squid.measures.MetricDef;

@Rule(key = "LCOM4 Suspect", priority = Priority.MAJOR, cardinality = Cardinality.MULTIPLE, 
description="Detects classes that should be reviewed because of a high LCOM4 metric. It possibly violates the Single Responsibility Principle.")
public class LCOM4Check extends BytecodeVisitor {
	
	public static final int MAX_LCOM4_DEFAULT = 1;

	@RuleProperty(defaultValue = "" + MAX_LCOM4_DEFAULT,
			description="The maximum allowed LCOM4 metric for a set of selected classes. Defaults to 1.")
	private int max = MAX_LCOM4_DEFAULT;
	
	@RuleProperty(description = "Ant-style pattern to select directories or files, for which this LCOM4 rule applies.")
	private WildcardPattern[] forClasses;

	@Override
	public void leaveClass(AsmClass asmClass) {
	    String className = asmClass.getInternalName();
	    if (WildcardPattern.match(getForClasses(), className)) {
	    	addMessageWhenRuleViolated(asmClass, className, getSourceClass(asmClass).getInt(Metric.LCOM4));
	    }
	}

	private void addMessageWhenRuleViolated(AsmClass asmClass,
			String className, Integer lcom4) {
		if (lcom4!=null && lcom4 > getMax()) {
			CheckMessage message = new CheckMessage(this, "Class '" + className + "' has an LCOM4 of " + lcom4 + ", which is higher than the configured maximum of " + getMax() + ".");
			message.setLine(getSourceFile(asmClass).getStartAtLine());
			message.setCost(lcom4 - getMax());
			getSourceFile(asmClass).log(message);
		}
	}

	public WildcardPattern[] getForClasses() {
		if (forClasses == null) {
			forClasses = PatternUtils.createPatterns("**");
		}
		return forClasses;
	}

	public void setForClasses(WildcardPattern[] forClasses) {
		this.forClasses = forClasses;
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}
	
	

}
