/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.ArrayList;
import java.util.List;


@XStreamAlias("Or")
public class OrFilter {

  @XStreamImplicit(itemFieldName = "Bug")
  private List<Bug> bugs;

  @XStreamImplicit(itemFieldName = "Package")
  private List<PackageFilter> packages;

  @XStreamImplicit(itemFieldName = "Class")
  private List<ClassFilter> classes;

  @XStreamImplicit(itemFieldName = "Method")
  private List<MethodFilter> methods;

  @XStreamImplicit(itemFieldName = "Field")
  private List<FieldFilter> fields;

  @XStreamImplicit(itemFieldName = "Local")
  private List<LocalFilter> locals;


  public OrFilter() {
    bugs = new ArrayList<Bug>();
    packages = new ArrayList<PackageFilter>();
    classes = new ArrayList<ClassFilter>();
    methods = new ArrayList<MethodFilter>();
    fields = new ArrayList<FieldFilter>();
    locals = new ArrayList<LocalFilter>();
  }

  public List<Bug> getBugs() {
    return bugs;
  }

  public void setBugs(List<Bug> bugs) {
    this.bugs = bugs;
  }

  public List<PackageFilter> getPackages() {
    return packages;
  }

  public void setPackages(List<PackageFilter> packages) {
    this.packages = packages;
  }

  public List<ClassFilter> getClasses() {
    return classes;
  }

  public void setClasses(List<ClassFilter> classes) {
    this.classes = classes;
  }

  public List<MethodFilter> getMethods() {
    return methods;
  }

  public void setMethods(List<MethodFilter> methods) {
    this.methods = methods;
  }

  public List<FieldFilter> getFields() {
    return fields;
  }

  public void setFields(List<FieldFilter> fields) {
    this.fields = fields;
  }

  public List<LocalFilter> getLocals() {
    return locals;
  }

  public void setLocals(List<LocalFilter> locals) {
    this.locals = locals;
  }
}