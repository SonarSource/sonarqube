package selenium;

import com.google.common.base.Function;
import org.openqa.selenium.WebElement;

import java.util.Collection;

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
