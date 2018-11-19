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
package org.sonarqube.pageobjects;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import org.yaml.snakeyaml.error.Mark;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class MarketplacePage {

  public MarketplacePage() {
    $("#marketplace-page").should(Condition.exist);
  }

  public MarketplacePage hasPendingPlugins(String text) {
    $(".js-pending").should(Condition.exist).shouldHave(Condition.text(text));
    return this;
  }

  public MarketplacePage hasPluginsCount(int count) {
    $$("#marketplace-plugins>ul>li").shouldHaveSize(count);
    return this;
  }

  public MarketplacePage hasPluginWithText(String name, String text) {
    getPlugin(name).shouldHave(Condition.text(text));
    return this;
  }

  public MarketplacePage searchPlugin(String search) {
    $("#marketplace-search input.search-box-input").should(Condition.exist).sendKeys(search);
    return this;
  }

  public MarketplacePage uninstallPlugin(String name) {
    getPlugin(name).$("button.js-uninstall").click();
    return this;
  }

  private SelenideElement getPlugin(String name) {
    return $$(".js-plugin-name").findBy(Condition.text(name)).should(Condition.exist).parent().parent().parent();
  }
}
