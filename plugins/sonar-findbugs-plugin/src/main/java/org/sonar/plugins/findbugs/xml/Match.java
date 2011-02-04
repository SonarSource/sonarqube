/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.findbugs.xml;

import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("Match")
public class Match {

  @XStreamAlias("Bug")
  private Bug bug;

  @XStreamAlias("Priority")
  private Priority priority;

  @XStreamAlias("Package")
  private PackageFilter particularPackage;

  @XStreamAlias("Class")
  private ClassFilter particularClass;

  @XStreamAlias("Method")
  private MethodFilter particularMethod;

  @XStreamAlias("Field")
  private FieldFilter particularField;

  @XStreamAlias("Local")
  private LocalFilter particularLocal;

  @XStreamImplicit(itemFieldName = "Or")
  private List<OrFilter> ors;

  public Match() {
  }

  public Match(Bug bug, Priority priority) {
    this.bug = bug;
    this.priority = priority;
  }

  public Match(Bug bug) {
    this.bug = bug;
  }

  public Match(ClassFilter particularClass) {
    this.particularClass = particularClass;
  }

  public Bug getBug() {
    return bug;
  }

  public void setBug(Bug bug) {
    this.bug = bug;
  }

  public Priority getPriority() {
    return priority;
  }

  public void setPriority(Priority priority) {
    this.priority = priority;
  }

  public PackageFilter getParticularPackage() {
    return particularPackage;
  }

  public void setParticularPackage(PackageFilter particularPackage) {
    this.particularPackage = particularPackage;
  }

  public ClassFilter getParticularClass() {
    return particularClass;
  }

  public void setParticularClass(ClassFilter particularClass) {
    this.particularClass = particularClass;
  }

  public MethodFilter getParticularMethod() {
    return particularMethod;
  }

  public void setParticularMethod(MethodFilter particularMethod) {
    this.particularMethod = particularMethod;
  }

  public FieldFilter getParticularField() {
    return particularField;
  }

  public void setParticularField(FieldFilter particularField) {
    this.particularField = particularField;
  }

  public LocalFilter getParticularLocal() {
    return particularLocal;
  }

  public void setParticularLocal(LocalFilter particularLocal) {
    this.particularLocal = particularLocal;
  }

  public List<OrFilter> getOrs() {
    return ors;
  }

  public void setOrs(List<OrFilter> ors) {
    this.ors = ors;
  }

  public void addDisjunctCombine(OrFilter child) {
    ors.add(child);
  }
}
