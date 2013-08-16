/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.core.timemachine;

import java.util.List;

public class TendencyAnalyser {

  public static final Integer TENDENCY_BIG_UP = 2;
  public static final Integer TENDENCY_UP = 1;
  public static final Integer TENDENCY_NEUTRAL = 0;
  public static final Integer TENDENCY_DOWN = -1;
  public static final Integer TENDENCY_BIG_DOWN = -2;

  public Integer analyseLevel(List<Double> values) {
    TendencyAnalyser.SlopeData slopeData = analyse(values);
    if (slopeData != null) {
      return slopeData.getLevel();
    }
    return null;
  }

  public SlopeData analyse(List<Double> values) {
    double sumY = 0.0;
    double sumX = 0.0;
    double sumYPower2 = 0.0;
    double sumXY = 0.0;
    double sumXPower2 = 0.0;
    int nbrPoints = 0;
    boolean nullValuesYList = true;
    int i = 0;
    for (Double p : values) {
      if (p != null) {
        nullValuesYList = false;
        //SumY calculation
        sumY += p;
        // sumYPower2 calculation
        sumYPower2 += p * p;
        //sumXY calculation
        sumXY += p * (i + 1);
        //SumX calculation
        sumX += (i + 1);
        //sumXPower2 calculation
        sumXPower2 += (i + 1) * (i + 1);
        //Point number calculation
        nbrPoints++;
      }
      i++;
    }
    // no tendency if null values or only 1 value
    if (nullValuesYList || nbrPoints == 1) {
      return null;
    }
    double n0 = ((nbrPoints * sumXY) - (sumX * sumY));
    double d = ((nbrPoints * sumXPower2) - (sumX * sumX));
    double n1 = ((sumY * sumXPower2) - (sumX * sumXY));

    SlopeData result = new SlopeData();

    //yIntercept Calculation the value when X equals zero
    result.setYIntercept(n1 / d);
    // Slope Calculation
    if (n0 == 0d && d == 0d) {
      result.setSlope(0.0);
    } else {
      Double slope = n0 / d;
      if (Double.isNaN(slope) || Double.isInfinite(slope)) {
        result.setSlope(null);
      } else {
        result.setSlope(slope);
      }
    }
    result.setSumXPower2(sumXPower2);
    result.setSumXY(sumXY);
    result.setSumYPower2(sumYPower2);

    if (sumXPower2 == 0 || sumYPower2 == 0) {
      result.setCorrelationRate(0.0);
    } else {
      result.setCorrelationRate(sumXY / Math.sqrt(sumXPower2 * sumYPower2));
    }

    return result;
  }


  static class SlopeData {
    private double sumXPower2;
    private double sumYPower2;
    private double sumXY;
    private double yIntercept; // not used today
    private Double slope;
    private Double correlationRate;

    public double getSumXPower2() {
      return sumXPower2;
    }

    public void setSumXPower2(double sumXPower2) {
      this.sumXPower2 = sumXPower2;
    }

    public double getSumYPower2() {
      return sumYPower2;
    }

    public void setSumYPower2(double sumYPower2) {
      this.sumYPower2 = sumYPower2;
    }

    public double getSumXY() {
      return sumXY;
    }

    public void setSumXY(double sumXY) {
      this.sumXY = sumXY;
    }

    public double getYIntercept() {
      return yIntercept;
    }

    public void setYIntercept(double yIntercept) {
      this.yIntercept = yIntercept;
    }

    public Double getSlope() {
      return slope;
    }

    public void setSlope(Double slope) {
      this.slope = slope;
    }

    public Double getCorrelationRate() {
      return correlationRate;
    }

    public void setCorrelationRate(Double correlationRate) {
      this.correlationRate = correlationRate;
    }

    public Integer getLevel() {
      double hSlope = 0.8;
      double nSlope = 0.2;

      double vHighCorcoef = 1.0;
      double modCorcoef = 0.69;
      Double correlationCoeff = getCorrelationRate();
      boolean vHCorCoefPos = (correlationCoeff > modCorcoef) && (correlationCoeff <= vHighCorcoef);
      boolean vHCorCoefNeg = (correlationCoeff < -modCorcoef) && (correlationCoeff >= -vHighCorcoef);

      if ((vHCorCoefPos || vHCorCoefNeg) && (slope >= hSlope)) {
        return TENDENCY_BIG_UP;

      } else if ((vHCorCoefPos || vHCorCoefNeg) && (slope <= -hSlope)) {
        return TENDENCY_BIG_DOWN;

      } else if ((vHCorCoefPos || vHCorCoefNeg) && ((slope >= nSlope) && (slope < hSlope))) {
        return TENDENCY_UP;

      } else if ((vHCorCoefPos || vHCorCoefNeg) && ((slope <= -nSlope) && (slope > -hSlope))) {
        return TENDENCY_DOWN;

      } else if ((vHCorCoefPos || vHCorCoefNeg) && ((slope < nSlope) || (slope > -nSlope))) {
        return TENDENCY_NEUTRAL;

      } else if (correlationCoeff == 0 && slope == 0 && !vHCorCoefPos && !vHCorCoefNeg) {
        return TENDENCY_NEUTRAL;
      }
      return null;
    }
  }
}
