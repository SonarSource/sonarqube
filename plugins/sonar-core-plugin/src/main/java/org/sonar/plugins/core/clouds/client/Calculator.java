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
package org.sonar.plugins.core.clouds.client;

import org.sonar.plugins.core.clouds.client.model.Color;

public class Calculator {

  private Float minValue;
  private Float maxValue;
  private Float minPercent;
  private Float maxPercent;

  public Calculator(Float minPercent, Float maxPercent) {
    this.minPercent = minPercent;
    this.maxPercent = maxPercent;
  }

  public void updateMaxAndMin(Float value){
    updateMaxValue(value);
    updateMinValue(value);
  }
  
  public Integer getFontSizePercent(Integer value) {
    float divisor = getMaxValue() - getMinValue();
    float size = getMinPercent();
    if (divisor != 0) {
      float multiplier = (getMaxPercent() - getMinPercent()) / divisor;
      size = getMinPercent() +
        ((getMaxValue() - (getMaxValue() - (value - getMinValue()))) * multiplier);
    }
    return Float.valueOf(size).intValue();
  }

  public String getFontColor(float value) {
    float interval = (getMaxPercent() - getMinPercent()) / 2f;
    float mean = (getMinPercent() + getMaxPercent()) / 2f;

    Color minColor = new Color(191/255f, 0f, 21/255f); // red
    Color meanColor = new Color(77/255f, 5/255f, 177/255f); // purple
    Color maxColor = new Color(23/255f, 96/255f, 191/255f); // blue

    Color color;
    if (value > mean) {
      float valuePercent = ((value - mean) / interval) * 100f;
      color = mixColorWith(maxColor, meanColor, valuePercent);
    } else {
      float valuePercent = ((mean - value) / interval) * 100f;
      color = mixColorWith(minColor, meanColor, valuePercent);
    }

    int r = Float.valueOf(color.getRed()* 255f).intValue();
    int g = Float.valueOf(color.getGreen() * 255f).intValue();
    int b = Float.valueOf(color.getBlue() * 255f).intValue();

    return ("rgb("+ r +","+ g +","+ b +")");
  }

  private Color mixColorWith(Color currentColor, Color mask, float value){
    float opacity = value / 100f;

    float r = (currentColor.getRed() * opacity) + (mask.getRed() * (1f - opacity));
    float g = (currentColor.getGreen() * opacity) + (mask.getGreen() * (1f - opacity));
    float b = (currentColor.getBlue() * opacity) + (mask.getBlue() * (1f - opacity));

    return new Color(r, g, b);
  }


  private void updateMaxValue(Float value) {
    if (maxValue == null) {
      maxValue = value;
    } else if (value > maxValue) {
      maxValue = value;
    }
  }

  private void updateMinValue(Float value) {
    if (minValue == null) {
      minValue = value;
    } else if (value < minValue) {
      minValue = value;
    }
  }


  public Float getMinValue() {
    return minValue;
  }

  public Float getMaxValue() {
    return maxValue;
  }

  public Float getMinPercent() {
    return minPercent;
  }

  public Float getMaxPercent() {
    return maxPercent;
  }
}
