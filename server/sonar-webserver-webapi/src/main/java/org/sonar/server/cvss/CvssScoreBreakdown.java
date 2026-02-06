/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.cvss;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * CVSS score breakdown for a single rule. Instances of this class are loaded
 * from JSON at startup and associated with rule keys.
 */
public class CvssScoreBreakdown {

    private String ruleKey;
    private double cvssScore;
    private double baseScore;
    private double temporalScore;
    private double environmentalScore;
    private String vectorString;
    private Map<CvssMetricGroup, List<CvssMetricEntry>> metrics = new EnumMap<>(CvssMetricGroup.class);

    public String getRuleKey() {
        return ruleKey;
    }

    public void setRuleKey(String ruleKey) {
        this.ruleKey = ruleKey;
    }

    public double getCvssScore() {
        return cvssScore;
    }

    public void setCvssScore(double cvssScore) {
        this.cvssScore = cvssScore;
    }

    public double getBaseScore() {
        return baseScore;
    }

    public void setBaseScore(double baseScore) {
        this.baseScore = baseScore;
    }

    public double getTemporalScore() {
        return temporalScore;
    }

    public void setTemporalScore(double temporalScore) {
        this.temporalScore = temporalScore;
    }

    public double getEnvironmentalScore() {
        return environmentalScore;
    }

    public void setEnvironmentalScore(double environmentalScore) {
        this.environmentalScore = environmentalScore;
    }

    public String getVectorString() {
        return vectorString;
    }

    public void setVectorString(String vectorString) {
        this.vectorString = vectorString;
    }

    public Map<CvssMetricGroup, List<CvssMetricEntry>> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<CvssMetricGroup, List<CvssMetricEntry>> metrics) {
        this.metrics = metrics;
    }
}


