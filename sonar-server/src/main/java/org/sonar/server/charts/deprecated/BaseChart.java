/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.charts.deprecated;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.encoders.KeypointPNGEncoderAdapter;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public abstract class BaseChart {

  public static final Color BASE_COLOR = new Color(51, 51, 51);
  public static final Color BASE_COLOR_LIGHT = new Color(204, 204, 204);
  public static final Color SERIE_BORDER_COLOR = new Color(67, 119, 166);

  public static final Color[] COLORS = {
    new Color(5, 141, 199),
    new Color(80, 180, 50),
    new Color(237, 86, 27),
    new Color(237, 239, 0),
    new Color(36, 203, 229),
    new Color(100, 229, 114),
    new Color(255, 150, 85)
  };

  public static final int FONT_SIZE = 13;

  private int width;
  private int height;

  protected BaseChart(int width, int height) {
    this.width = width;
    this.height = height;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public void setWidth(int width) {
    this.width = width;
  }

  public void setHeight(int height) {
    this.height = height;
  }

  protected Font getFont() {
    return new Font("SansSerif", Font.PLAIN, FONT_SIZE);
  }

  protected void configureChart(JFreeChart chart, boolean displayLegend) {
    if (displayLegend) {
      configureChart(chart, RectangleEdge.BOTTOM);
    } else {
      configureChart(chart, null);
    }
  }

  protected void configureChart(JFreeChart chart, RectangleEdge legendPosition) {
    chart.setBackgroundPaint(new Color(255, 255, 255, 0));
    chart.setBackgroundImageAlpha(0.0f);
    chart.setBorderVisible(false);
    chart.setAntiAlias(true);
    chart.setTextAntiAlias(true);

    chart.removeLegend();
    if (legendPosition != null) {
      LegendTitle legend = new LegendTitle(chart.getPlot());
      legend.setPosition(legendPosition);
      legend.setItemPaint(BASE_COLOR);
      chart.addSubtitle(legend);
    }
  }

  protected void configureChartTitle(JFreeChart chart, String title) {
    if (title != null && title.length() > 0) {
      TextTitle textTitle = new TextTitle(title);
      chart.setTitle(textTitle);
    }
  }

  protected abstract BufferedImage getChartImage() throws IOException;

  protected BufferedImage getBufferedImage(JFreeChart chart) {
    return chart.createBufferedImage(getWidth(), getHeight(), Transparency.BITMASK, null);
  }

  public void exportChartAsPNG(OutputStream out) throws IOException {
    KeypointPNGEncoderAdapter encoder = new KeypointPNGEncoderAdapter();
    encoder.setEncodingAlpha(true);
    encoder.encode(getChartImage(), out);
  }

  public byte[] exportChartAsPNG() throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try {
      exportChartAsPNG(output);
    } finally {
      output.close();
    }
    return output.toByteArray();
  }

  protected BasicStroke getDashedStroke() {
    return getDashedStroke(1f);
  }

  protected BasicStroke getDashedStroke(float width) {
    return new BasicStroke(width,
        BasicStroke.CAP_BUTT,
        BasicStroke.JOIN_MITER,
        10.0f, new float[] { 5.0f }, 0.0f);
  }
}
