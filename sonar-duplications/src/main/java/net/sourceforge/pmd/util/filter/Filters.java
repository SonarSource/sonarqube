package net.sourceforge.pmd.util.filter;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility class for working with Filters.  Contains builder style methods,
 * apply methods, as well as mechanisms for adapting Filters and FilenameFilters.
 */
public class Filters {

	/**
	 * Filter a given Collection.
	 * @param <T> Type of the Collection.
	 * @param filter A Filter upon the Type of objects in the Collection.
	 * @param collection The Collection to filter.
	 * @return A List containing only those objects for which the Filter returned <code>true</code>.
	 */
	public static <T> List<T> filter(Filter<T> filter, Collection<T> collection) {
		List<T> list = new ArrayList<T>();
		for (T obj : collection) {
			if (filter.filter(obj)) {
				list.add(obj);
			}
		}
		return list;
	}

	/**
	 * Get a File Filter for files with the given extensions, ignoring case.
	 * @param extensions The extensions to filter.
	 * @return A File Filter.
	 */
	public static Filter<File> getFileExtensionFilter(String... extensions) {
		return new FileExtensionFilter(extensions);
	}

	/**
	 * Get a File Filter for directories.
	 * @return A File Filter.
	 */
	public static Filter<File> getDirectoryFilter() {
		return DirectoryFilter.INSTANCE;
	}

	/**
	 * Get a File Filter for directories or for files with the given extensions, ignoring case.
	 * @param extensions The extensions to filter.
	 * @return A File Filter.
	 */
	public static Filter<File> getFileExtensionOrDirectoryFilter(String... extensions) {
		return new OrFilter<File>(getFileExtensionFilter(extensions), getDirectoryFilter());
	}

	/**
	 * Given a String Filter, expose as a File Filter.  The File paths are
	 * normalized to a standard pattern using <code>/</code> as a path separator
	 * which can be used cross platform easily in a regular expression based
	 * String Filter.
	 * 
	 * @param filter A String Filter.
	 * @return A File Filter.
	 */
	public static Filter<File> toNormalizedFileFilter(final Filter<String> filter) {
		return new Filter<File>() {
			public boolean filter(File file) {
				String path = file.getPath();
				path = path.replace('\\', '/');
				return filter.filter(path);
			}

			public String toString() {
				return filter.toString();
			}
		};
	}

	/**
	 * Given a String Filter, expose as a Filter on another type.  The
	 * <code>toString()</code> method is called on the objects of the other
	 * type and delegated to the String Filter.
	 * @param <T> The desired type.
	 * @param filter The existing String Filter.
	 * @return A Filter on the desired type.
	 */
	public static <T> Filter<T> fromStringFilter(final Filter<String> filter) {
		return new Filter<T>() {
			public boolean filter(T obj) {
				return filter.filter(obj.toString());
			}

			public String toString() {
				return filter.toString();
			}
		};
	}

	/**
	 * Given a File Filter, expose as a FilenameFilter.
	 * @param filter The File Filter.
	 * @return A FilenameFilter.
	 */
	public static FilenameFilter toFilenameFilter(final Filter<File> filter) {
		return new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return filter.filter(new File(dir, name));
			}

			public String toString() {
				return filter.toString();
			}
		};
	}

	/**
	 * Given a FilenameFilter, expose as a File Filter.
	 * @param filter The FilenameFilter.
	 * @return A File Filter.
	 */
	public static Filter<File> toFileFilter(final FilenameFilter filter) {
		return new Filter<File>() {
			public boolean filter(File file) {
				return filter.accept(file.getParentFile(), file.getName());
			}

			public String toString() {
				return filter.toString();
			}
		};
	}

	/**
	 * Construct a String Filter using set of include and exclude regular
	 * expressions.  If there are no include regular expressions provide, then
	 * a regular expression is added which matches every String by default.
	 * A String is included as long as it matches an include regular expression
	 * and does not match an exclude regular expression.
	 * <p>
	 * In other words, exclude patterns override include patterns.
	 * 
	 * @param includeRegexes The include regular expressions.  May be <code>null</code>.
	 * @param excludeRegexes The exclude regular expressions.  May be <code>null</code>.
	 * @return A String Filter.
	 */
	public static Filter<String> buildRegexFilterExcludeOverInclude(List<String> includeRegexes,
			List<String> excludeRegexes) {
		OrFilter<String> includeFilter = new OrFilter<String>();
		if (includeRegexes == null || includeRegexes.size() == 0) {
			includeFilter.addFilter(new RegexStringFilter(".*"));
		} else {
			for (String includeRegex : includeRegexes) {
				includeFilter.addFilter(new RegexStringFilter(includeRegex));
			}
		}

		OrFilter<String> excludeFilter = new OrFilter<String>();
		if (excludeRegexes != null) {
			for (String excludeRegex : excludeRegexes) {
				excludeFilter.addFilter(new RegexStringFilter(excludeRegex));
			}
		}

		return new AndFilter<String>(includeFilter, new NotFilter<String>(excludeFilter));
	}

	/**
	 * Construct a String Filter using set of include and exclude regular
	 * expressions.  If there are no include regular expressions provide, then
	 * a regular expression is added which matches every String by default.
	 * A String is included as long as the case that there is an include which
	 * matches or there is not an exclude which matches.
	 * <p>
	 * In other words, include patterns override exclude patterns.
	 * 
	 * @param includeRegexes The include regular expressions.  May be <code>null</code>.
	 * @param excludeRegexes The exclude regular expressions.  May be <code>null</code>.
	 * @return A String Filter.
	 */
	public static Filter<String> buildRegexFilterIncludeOverExclude(List<String> includeRegexes,
			List<String> excludeRegexes) {
		OrFilter<String> includeFilter = new OrFilter<String>();
		if (includeRegexes != null) {
			for (String includeRegex : includeRegexes) {
				includeFilter.addFilter(new RegexStringFilter(includeRegex));
			}
		}

		OrFilter<String> excludeFilter = new OrFilter<String>();
		if (excludeRegexes != null) {
			for (String excludeRegex : excludeRegexes) {
				excludeFilter.addFilter(new RegexStringFilter(excludeRegex));
			}
		}

		return new OrFilter<String>(includeFilter, new NotFilter<String>(excludeFilter));
	}
}
