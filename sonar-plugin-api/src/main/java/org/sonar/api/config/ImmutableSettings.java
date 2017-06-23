package org.sonar.api.config;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.trim;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.DateUtils;

import com.google.common.base.Splitter;

@ScannerSide
@ServerSide
@ComputeEngineSide
@Immutable
public abstract class ImmutableSettings {
    private final PropertyDefinitions definitions;
    private final Encryption encryption;

    protected ImmutableSettings(PropertyDefinitions definitions, Encryption encryption) {
      this.definitions = requireNonNull(definitions);
      this.encryption = requireNonNull(encryption);
    }
    
    protected void addProperties(Map<String, String> from, Map<String, String> to) {
      for (Map.Entry<String, String> entry : from.entrySet()) {
        setProperty(entry.getKey(), entry.getValue(), to);
      }
    }

    protected void setProperty(String key, @Nullable String value, Map<String, String> to) {
      String validKey = definitions.validKey(key);
      if (value == null) {
        to.remove(key);
      } else {
        to.put(validKey, trim(value));
      }
    }

    protected abstract Optional<String> get(String key);

    /**
     * Immutable map of the properties that have non-default values.
     * The default values defined by {@link PropertyDefinitions} are ignored,
     * so the returned values are not the effective values. Basically only
     * the non-empty results of {@link #getRawString(String)} are returned.
     * <p>
     * Values are not decrypted if they are encrypted with a secret key.
     * </p>
     */
    public abstract Map<String, String> getProperties();

    public Encryption getEncryption() {
      return encryption;
    }

    /**
     * The value that overrides the default value. It
     * may be encrypted with a secret key. Use {@link #getString(String)} to get
     * the effective and decrypted value.
     *
     * @since 6.1
     */
    public Optional<String> getRawString(String key) {
      return get(definitions.validKey(requireNonNull(key)));
    }

    /**
     * All the property definitions declared by core and plugins.
     */
    public PropertyDefinitions getDefinitions() {
      return definitions;
    }

    /**
     * The definition related to the specified property. It may
     * be empty.
     *
     * @since 6.1
     */
    public Optional<PropertyDefinition> getDefinition(String key) {
      return Optional.ofNullable(definitions.get(key));
    }

    /**
     * @return {@code true} if the property has a non-default value, else {@code false}.
     */
    public boolean hasKey(String key) {
      return getRawString(key).isPresent();
    }

    @CheckForNull
    public String getDefaultValue(String key) {
      return definitions.getDefaultValue(key);
    }

    public boolean hasDefaultValue(String key) {
      return StringUtils.isNotEmpty(getDefaultValue(key));
    }

    /**
     * The effective value of the specified property. Can return
     * {@code null} if the property is not set and has no
     * defined default value.
     * <p>
     * If the property is encrypted with a secret key,
     * then the returned value is decrypted.
     * </p>
     *
     * @throws IllegalStateException if value is encrypted but fails to be decrypted.
     */
    @CheckForNull
    public String getString(String key) {
      String effectiveKey = definitions.validKey(key);
      Optional<String> value = getRawString(effectiveKey);
      if (!value.isPresent()) {
        // default values cannot be encrypted, so return value as-is.
        return getDefaultValue(effectiveKey);
      }
      if (encryption.isEncrypted(value.get())) {
        try {
          return encryption.decrypt(value.get());
        } catch (Exception e) {
          throw new IllegalStateException("Fail to decrypt the property " + effectiveKey + ". Please check your secret key.", e);
        }
      }
      return value.get();
    }

    /**
     * Effective value as boolean. It is {@code false} if {@link #getString(String)}
     * does not return {@code "true"}, even if it's not a boolean representation.
     * @return {@code true} if the effective value is {@code "true"}, else {@code false}.
     */
    public boolean getBoolean(String key) {
      String value = getString(key);
      return StringUtils.isNotEmpty(value) && Boolean.parseBoolean(value);
    }

    /**
     * Effective value as {@code int}.
     * @return the value as {@code int}. If the property does not have value nor default value, then {@code 0} is returned.
     * @throws NumberFormatException if value is not empty and is not a parsable integer
     */
    public int getInt(String key) {
      String value = getString(key);
      if (StringUtils.isNotEmpty(value)) {
        return Integer.parseInt(value);
      }
      return 0;
    }

    /**
     * Effective value as {@code long}.
     * @return the value as {@code long}. If the property does not have value nor default value, then {@code 0L} is returned.
     * @throws NumberFormatException if value is not empty and is not a parsable {@code long}
     */
    public long getLong(String key) {
      String value = getString(key);
      if (StringUtils.isNotEmpty(value)) {
        return Long.parseLong(value);
      }
      return 0L;
    }

    /**
     * Effective value as {@link Date}, without time fields. Format is {@link DateUtils#DATE_FORMAT}.
     *
     * @return the value as a {@link Date}. If the property does not have value nor default value, then {@code null} is returned.
     * @throws RuntimeException if value is not empty and is not in accordance with {@link DateUtils#DATE_FORMAT}.
     */
    @CheckForNull
    public Date getDate(String key) {
      String value = getString(key);
      if (StringUtils.isNotEmpty(value)) {
        return DateUtils.parseDate(value);
      }
      return null;
    }

    /**
     * Effective value as {@link Date}, with time fields. Format is {@link DateUtils#DATETIME_FORMAT}.
     *
     * @return the value as a {@link Date}. If the property does not have value nor default value, then {@code null} is returned.
     * @throws RuntimeException if value is not empty and is not in accordance with {@link DateUtils#DATETIME_FORMAT}.
     */
    @CheckForNull
    public Date getDateTime(String key) {
      String value = getString(key);
      if (StringUtils.isNotEmpty(value)) {
        return DateUtils.parseDateTime(value);
      }
      return null;
    }

    /**
     * Effective value as {@code Float}.
     * @return the value as {@code Float}. If the property does not have value nor default value, then {@code null} is returned.
     * @throws NumberFormatException if value is not empty and is not a parsable number
     */
    @CheckForNull
    public Float getFloat(String key) {
      String value = getString(key);
      if (StringUtils.isNotEmpty(value)) {
        try {
          return Float.valueOf(value);
        } catch (NumberFormatException e) {
          throw new IllegalStateException(String.format("The property '%s' is not a float value", key));
        }
      }
      return null;
    }

    /**
     * Effective value as {@code Double}.
     * @return the value as {@code Double}. If the property does not have value nor default value, then {@code null} is returned.
     * @throws NumberFormatException if value is not empty and is not a parsable number
     */
    @CheckForNull
    public Double getDouble(String key) {
      String value = getString(key);
      if (StringUtils.isNotEmpty(value)) {
        try {
          return Double.valueOf(value);
        } catch (NumberFormatException e) {
          throw new IllegalStateException(String.format("The property '%s' is not a double value", key));
        }
      }
      return null;
    }

    /**
     * Value is split by comma and trimmed. Never returns null.
     * <br>
     * Examples :
     * <ul>
     * <li>"one,two,three " -&gt; ["one", "two", "three"]</li>
     * <li>"  one, two, three " -&gt; ["one", "two", "three"]</li>
     * <li>"one, , three" -&gt; ["one", "", "three"]</li>
     * </ul>
     */
    public String[] getStringArray(String key) {
      Optional<PropertyDefinition> def = getDefinition(key);
      if ((def.isPresent()) && (def.get().multiValues())) {
        String value = getString(key);
        if (value == null) {
          return ArrayUtils.EMPTY_STRING_ARRAY;
        }

        List<String> values = new ArrayList<>();
        for (String v : Splitter.on(",").trimResults().split(value)) {
          values.add(v.replace("%2C", ","));
        }
        return values.toArray(new String[values.size()]);
      }

      return getStringArrayBySeparator(key, ",");
    }

    /**
     * Value is split by carriage returns.
     *
     * @return non-null array of lines. The line termination characters are excluded.
     * @since 3.2
     */
    public String[] getStringLines(String key) {
      String value = getString(key);
      if (StringUtils.isEmpty(value)) {
        return new String[0];
      }
      return value.split("\r?\n|\r", -1);
    }

    /**
     * Value is split and trimmed.
     */
    public String[] getStringArrayBySeparator(String key, String separator) {
      String value = getString(key);
      if (value != null) {
        String[] strings = StringUtils.splitByWholeSeparator(value, separator);
        String[] result = new String[strings.length];
        for (int index = 0; index < strings.length; index++) {
          result[index] = trim(strings[index]);
        }
        return result;
      }
      return ArrayUtils.EMPTY_STRING_ARRAY;
    }

    public List<String> getKeysStartingWith(String prefix) {
      return getProperties().keySet().stream()
        .filter(key -> StringUtils.startsWith(key, prefix))
        .collect(Collectors.toList());
    }
}
