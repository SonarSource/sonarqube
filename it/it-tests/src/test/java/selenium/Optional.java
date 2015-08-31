package selenium;

import java.util.NoSuchElementException;

public final class Optional<T> {
  private static final Optional<?> EMPTY = new Optional<>();

  private final T value;

  private Optional() {
    this.value = null;
  }

  public static <T> Optional<T> empty() {
    @SuppressWarnings("unchecked")
    Optional<T> t = (Optional<T>) EMPTY;
    return t;
  }

  private Optional(T value) {
    this.value = value;
  }

  public static <T> Optional<T> of(T value) {
    return new Optional<>(value);
  }

  public T get() {
    if (value == null) {
      throw new NoSuchElementException("No value present");
    }
    return value;
  }

  public boolean isPresent() {
    return value != null;
  }
}
