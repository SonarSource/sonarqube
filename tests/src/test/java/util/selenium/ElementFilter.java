/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package util.selenium;

import com.google.common.base.Function;
import java.util.Collection;
import org.openqa.selenium.WebElement;

class ElementFilter {
  private static final ElementFilter ANY = new ElementFilter("", new Function<Collection<WebElement>, Collection<WebElement>>() {
    @Override
    public Collection<WebElement> apply(Collection<WebElement> input) {
      return input;
    }
  });

  private final String description;
  private final Function<Collection<WebElement>, Collection<WebElement>> filter;

  ElementFilter(String description, Function<Collection<WebElement>, Collection<WebElement>> filter) {
    this.description = description;
    this.filter = filter;
  }

  public String getDescription() {
    return description;
  }

  public Function<Collection<WebElement>, Collection<WebElement>> getFilter() {
    return filter;
  }

  public static ElementFilter any() {
    return ANY;
  }

  public ElementFilter and(final ElementFilter second) {
    if (ANY == this) {
      return second;
    }
    if (ANY == second) {
      return this;
    }
    return new ElementFilter(description + ',' + second.description, new Function<Collection<WebElement>, Collection<WebElement>>() {
      @Override
      public Collection<WebElement> apply(Collection<WebElement> stream) {
        return second.filter.apply(filter.apply(stream));
      }
    });
  }
}
