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
package org.sonar.api.server.ws;

import com.google.common.base.Splitter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.DateUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.utils.DateUtils.parseDateQuietly;
import static org.sonar.api.utils.DateUtils.parseDateTimeQuietly;

/**
 * @since 4.2
 */
public abstract class Request {

  protected static final String MSG_PARAMETER_MISSING = "The '%s' parameter is missing";

  /**
   * Returns the name of the HTTP method with which this request was made. Possible
   * values are GET and POST. Others are not supported.
   */
  public abstract String method();

  /**
   * Returns the requested MIME type, or {@code "application/octet-stream"} if not specified.
   */
  public abstract String getMediaType();

  /**
   * Return true of the parameter is set in the request.
   * Does NOT take into account the deprecated key of a parameter.
   */
  public abstract boolean hasParam(String key);

  /**
   * Returns a non-null value. To be used when parameter is required or has a default value.
   *
   * @throws java.lang.IllegalArgumentException is value is null or blank
   */
  public String mandatoryParam(String key) {
    String value = param(key);
    checkArgument(value != null, format(MSG_PARAMETER_MISSING, key));
    return value;
  }

  /**
   * Returns a boolean value. To be used when parameter is required or has a default value.
   *
   * @throws java.lang.IllegalArgumentException is value is null or blank
   */
  public boolean mandatoryParamAsBoolean(String key) {
    String s = mandatoryParam(key);
    return parseBoolean(key, s);
  }

  /**
   * Returns an int value. To be used when parameter is required or has a default value.
   *
   * @throws java.lang.IllegalArgumentException is value is null or blank
   */
  public int mandatoryParamAsInt(String key) {
    String s = mandatoryParam(key);
    return parseInt(key, s);
  }

  /**
   * Returns a long value. To be used when parameter is required or has a default value.
   *
   * @throws java.lang.IllegalArgumentException is value is null or blank
   */
  public long mandatoryParamAsLong(String key) {
    String s = mandatoryParam(key);
    return parseLong(key, s);
  }

  public <E extends Enum<E>> E mandatoryParamAsEnum(String key, Class<E> enumClass) {
    return Enum.valueOf(enumClass, mandatoryParam(key));
  }

  public List<String> mandatoryParamAsStrings(String key) {
    List<String> values = paramAsStrings(key);
    checkArgument(values != null, format(MSG_PARAMETER_MISSING, key));
    return values;
  }

  public List<String> mandatoryMultiParam(String key) {
    List<String> values = multiParam(key);
    checkArgument(!values.isEmpty(), MSG_PARAMETER_MISSING, key);
    return values;
  }

  @CheckForNull
  public abstract List<String> paramAsStrings(String key);

  @CheckForNull
  public abstract String param(String key);

  public abstract List<String> multiParam(String key);

  @CheckForNull
  public abstract InputStream paramAsInputStream(String key);

  @CheckForNull
  public abstract Part paramAsPart(String key);

  public Part mandatoryParamAsPart(String key) {
    Part part = paramAsPart(key);
    checkArgument(part != null, MSG_PARAMETER_MISSING, key);
    return part;
  }

  /**
   * @deprecated to be dropped in 4.4. Default values are declared in ws metadata
   */
  @CheckForNull
  @Deprecated
  public String param(String key, @CheckForNull String defaultValue) {
    return StringUtils.defaultString(param(key), defaultValue);
  }

  /**
   * @deprecated to be dropped in 4.4. Default values must be declared in {@link org.sonar.api.server.ws.WebService} then
   * this method can be replaced by {@link #mandatoryParamAsBoolean(String)}.
   */
  @Deprecated
  public boolean paramAsBoolean(String key, boolean defaultValue) {
    String value = param(key);
    return value == null ? defaultValue : parseBoolean(key, value);
  }

  /**
   * @deprecated to be dropped in 4.4. Default values must be declared in {@link org.sonar.api.server.ws.WebService} then
   * this method can be replaced by {@link #mandatoryParamAsInt(String)}.
   */
  @Deprecated
  public int paramAsInt(String key, int defaultValue) {
    String s = param(key);
    return s == null ? defaultValue : parseInt(key, s);
  }

  /**
   * @deprecated to be dropped in 4.4. Default values must be declared in {@link org.sonar.api.server.ws.WebService} then
   * this method can be replaced by {@link #mandatoryParamAsLong(String)}.
   */
  @Deprecated
  public long paramAsLong(String key, long defaultValue) {
    String s = param(key);
    return s == null ? defaultValue : parseLong(key, s);
  }

  @CheckForNull
  public Boolean paramAsBoolean(String key) {
    String value = param(key);
    return value == null ? null : parseBoolean(key, value);
  }

  @CheckForNull
  public Integer paramAsInt(String key) {
    String s = param(key);
    return s == null ? null : parseInt(key, s);
  }

  @CheckForNull
  public Long paramAsLong(String key) {
    String s = param(key);
    return s == null ? null : parseLong(key, s);
  }

  @CheckForNull
  public <E extends Enum<E>> E paramAsEnum(String key, Class<E> enumClass) {
    String s = param(key);
    return s == null ? null : Enum.valueOf(enumClass, s);
  }

  @CheckForNull
  public <E extends Enum<E>> List<E> paramAsEnums(String key, Class<E> enumClass) {
    String value = param(key);
    if (value == null) {
      return null;
    }
    Iterable<String> values = Splitter.on(',').omitEmptyStrings().trimResults().split(value);
    List<E> result = new ArrayList<>();
    for (String s : values) {
      result.add(Enum.valueOf(enumClass, s));
    }

    return result;
  }

  @CheckForNull
  public Date paramAsDateTime(String key) {
    String stringDate = param(key);
    if (stringDate == null) {
      return null;
    }

    Date date = parseDateTimeQuietly(stringDate);
    if (date != null) {
      return date;
    }

    date = parseDateQuietly(stringDate);
    checkArgument(date != null, "'%s' cannot be parsed as either a date or date+time", stringDate);

    return date;
  }

  @CheckForNull
  public Date paramAsDate(String key) {
    String s = param(key);
    if (s == null) {
      return null;
    }

    try {
      return DateUtils.parseDate(s);
    } catch (RuntimeException notDateException) {
      throw new IllegalArgumentException(notDateException);
    }
  }

  private static boolean parseBoolean(String key, String value) {
    if ("true".equals(value) || "yes".equals(value)) {
      return true;
    }
    if ("false".equals(value) || "no".equals(value)) {
      return false;
    }
    throw new IllegalArgumentException(format("Property %s is not a boolean value: %s", key, value));
  }

  private static int parseInt(String key, String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException expection) {
      throw new IllegalArgumentException(format("The '%s' parameter cannot be parsed as an integer value: %s", key, value));
    }
  }

  private static long parseLong(String key, String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException(format("The '%s' parameter cannot be parsed as a long value: %s", key, value));
    }
  }

  public <T> Param<T> getParam(String key, BiFunction<Request, String, T> retrieveAndValidate) {
    String param = this.param(key);
    if (param != null) {
      return GenericParam.present(retrieveAndValidate.apply(this, key));
    }
    return AbsentParam.absent();
  }

  public StringParam getParam(String key, Consumer<String> validate) {
    String value = this.param(key);
    if (value != null) {
      validate.accept(value);
      return StringParamImpl.present(value);
    }
    return AbsentStringParam.absent();
  }

  public StringParam getParam(String key) {
    String value = this.param(key);
    if (value != null) {
      return StringParamImpl.present(value);
    }
    return AbsentStringParam.absent();
  }

  /**
   * Optional value of the HTTP header with specified name.
   * If present, the result can have an empty string value ({@code ""}).
   *
   * @since 6.6
   */
  public abstract Optional<String> header(String name);

  /**
   * Allows a web service to call another web service.
   * @see LocalConnector
   * @since 5.5
   */
  public abstract LocalConnector localConnector();

  /**
   * Return path of the request
   * @since 6.0
   */
  public abstract String getPath();

  /**
   * @since 6.0
   */
  public interface Part {
    InputStream getInputStream();

    String getFileName();
  }

  /**
   * Represents a Request parameter, provides information whether is was specified or not (check {@link #isPresent()})
   * and utility method to nicely handles cases where the parameter is not present.
   */
  public interface Param<T> {
    boolean isPresent();

    /**
     * @return the value of the parameter
     *
     * @throws IllegalStateException if param is not present.
     */
    @CheckForNull
    T getValue();

    @CheckForNull
    T or(Supplier<T> defaultValueSupplier);
  }

  /**
   * Implementation of {@link Param} where the param is not present.
   */
  private enum AbsentParam implements Param<Object> {
    INSTANCE;

    @SuppressWarnings("unchecked")
    protected static <T> Param<T> absent() {
      return (Param<T>) INSTANCE;
    }

    /**
     * Always returns true.
     */
    @Override
    public boolean isPresent() {
      return false;
    }

    /**
     * Always throws a {@link IllegalStateException}.
     */
    @Override
    public Object getValue() {
      throw createGetValueISE();
    }

    /**
     * Always returns the value supplied by {@code defaultValueSupplier}.
     */
    @Override
    @CheckForNull
    public Object or(Supplier<Object> defaultValueSupplier) {
      return checkDefaultValueSupplier(defaultValueSupplier).get();
    }
  }

  /**
   * Implementation of {@link Param} where the param is present.
   */
  private static final class GenericParam<T> implements Param<T> {
    private final T value;

    private GenericParam(T value) {
      this.value = value;
    }

    static <T> Param<T> present(T value) {
      return new GenericParam<>(value);
    }

    /**
     * Always returns true.
     */
    @Override
    public boolean isPresent() {
      return true;
    }

    /**
     * @return the value of the parameter
     *
     * @throws IllegalStateException if param is not present.
     */
    @Override
    @CheckForNull
    public T getValue() {
      return value;
    }

    /**
     * Always returns value of the parameter.
     *
     * @throws NullPointerException As per the inherited contract, {@code defaultValueSupplier} can't be null
     */
    @Override
    @CheckForNull
    public T or(Supplier<T> defaultValueSupplier) {
      checkDefaultValueSupplier(defaultValueSupplier);
      return value;
    }
  }

  /**
   * Extends {@link Param} with convenience methods specific to the type {@link String}.
   */
  public interface StringParam extends Param<String> {
    /**
     * Returns a {@link StringParam} object which methods {@link #getValue()} and {@link #or(Supplier)} will
     * return {@code null} rather than an empty String when the param is present and its value is an empty String.
     */
    StringParam emptyAsNull();
  }

  /**
   * Implementation of {@link StringParam} where the param is not present.
   */
  private enum AbsentStringParam implements StringParam {
    INSTANCE;

    protected static StringParam absent() {
      return INSTANCE;
    }

    /**
     * Always returns false.
     */
    @Override
    public boolean isPresent() {
      return false;
    }

    /**
     * Always throws a {@link IllegalStateException}.
     */
    @Override
    public String getValue() {
      throw createGetValueISE();
    }

    /**
     * Always returns the value supplied by {@code defaultValueSupplier}.
     */
    @Override
    public String or(Supplier<String> defaultValueSupplier) {
      return checkDefaultValueSupplier(defaultValueSupplier).get();
    }

    /**
     * Returns itself.
     */
    @Override
    public StringParam emptyAsNull() {
      return this;
    }
  }

  /**
   * Implementation of {@link StringParam} where the param is present.
   */
  private static final class StringParamImpl implements StringParam {
    @CheckForNull
    private final String value;
    private final boolean emptyAsNull;

    private StringParamImpl(@Nullable String value, boolean emptyAsNull) {
      this.value = value;
      this.emptyAsNull = emptyAsNull;
    }

    static StringParam present(String value) {
      return new StringParamImpl(value, false);
    }

    @Override
    public boolean isPresent() {
      return true;
    }

    @Override
    public String getValue() {
      if (emptyAsNull && value != null && value.isEmpty()) {
        return null;
      }
      return value;
    }

    @Override
    @CheckForNull
    public String or(Supplier<String> defaultValueSupplier) {
      checkDefaultValueSupplier(defaultValueSupplier);
      if (emptyAsNull && value != null && value.isEmpty()) {
        return null;
      }
      return value;
    }

    @Override
    public StringParam emptyAsNull() {
      if (emptyAsNull || (value != null && !value.isEmpty())) {
        return this;
      }
      return new StringParamImpl(value, true);
    }
  }

  private static <T> Supplier<T> checkDefaultValueSupplier(Supplier<T> defaultValueSupplier) {
    return requireNonNull(defaultValueSupplier, "default value supplier can't be null");
  }

  private static IllegalStateException createGetValueISE() {
    return new IllegalStateException("Param has no value. Use isPresent() before calling getValue()");
  }
}
