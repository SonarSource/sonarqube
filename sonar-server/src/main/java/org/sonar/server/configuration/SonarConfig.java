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
package org.sonar.server.configuration;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.database.configuration.Property;
import org.sonar.api.measures.Metric;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.Rule;

import java.util.Collection;
import java.util.Date;

@XStreamAlias("sonar-config")
public class SonarConfig {

  private Integer version;

  private Date date;

  private Collection<Metric> metrics;

  private Collection<Property> properties;

  private Collection<RulesProfile> profiles;

  private Collection<Rule> rules;

  public SonarConfig() {
  }

  public SonarConfig(Integer version, Date date) {
    this.version = version;
    this.date = date;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public Collection<Metric> getMetrics() {
    return metrics;
  }

  public void setMetrics(Collection<Metric> metrics) {
    this.metrics = metrics;
  }

  public Collection<Property> getProperties() {
    return properties;
  }

  public void setProperties(Collection<Property> properties) {
    this.properties = properties;
  }

  public Collection<RulesProfile> getProfiles() {
    return profiles;
  }

  public void setProfiles(Collection<RulesProfile> profiles) {
    this.profiles = profiles;
  }

  public Collection<Rule> getRules() {
    return rules;
  }

  public void setRules(Collection<Rule> rules) {
    this.rules = rules;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("version", version)
        .append("date", date)
        .append("metrics", metrics)
        .append("properties", properties)
        .append("profiles", profiles)
        .toString();
  }

}
