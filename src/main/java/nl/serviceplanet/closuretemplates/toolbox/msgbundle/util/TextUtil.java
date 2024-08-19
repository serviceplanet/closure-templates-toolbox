package nl.serviceplanet.closuretemplates.toolbox.msgbundle.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public class TextUtil {
	public static String before(String full, String find) {
		return before(full, find, null);
	}

	public static String before(String full, String find, String orElse) {
		if (full == null || find == null) {
			return orElse;
		}
		int io = full.indexOf(find);
		if (io == -1) {
			return orElse;
		}
		return full.substring(0, io);
	}

	public static String after(String full, String find) {
		return after(full, find, null);
	}

	public static String after(String full, String find, String orElse) {
		if (full == null || find == null) {
			return orElse;
		}
		int io = full.indexOf(find);
		if (io == -1) {
			return orElse;
		}
		return full.substring(io + find.length());
	}

	public static String beforeLast(String full, String find) {
		return beforeLast(full, find, null);
	}

	public static String beforeLast(String full, String find, String orElse) {
		if (full == null || find == null) {
			return orElse;
		}
		int io = full.lastIndexOf(find);
		if (io == -1) {
			return orElse;
		}
		return full.substring(0, io);
	}

	public static String afterLast(String full, String find) {
		return afterLast(full, find, null);
	}

	public static String afterLast(String full, String find, String orElse) {
		if (full == null || find == null) {
			return orElse;
		}
		int io = full.lastIndexOf(find);
		if (io == -1) {
			return orElse;
		}
		return full.substring(io + find.length());
	}

	public static String between(String full, String first, String last) {
		return between(full, first, last, null);
	}

	public static String between(String full, String first, String last, String orElse) {
		String after = after(full, first, null);
		if (after == null) {
			return orElse;
		}
		return before(after, last, orElse);
	}

	public static String betweenWidest(String full, String first, String last) {
		return betweenWidest(full, first, last, null);
	}

	public static String betweenWidest(String full, String first, String last, String orElse) {
		String after = after(full, first, null);
		if (after == null) {
			return orElse;
		}
		return beforeLast(after, last, orElse);
	}

	public static String extractWrappedText(String full, String first, String last) {
		if (!full.startsWith(first)) {
			throw new IllegalArgumentException("must start with: '" + first + "', found: '" + full + "'");
		}
		full = after(full, first);
		if (!full.endsWith(last)) {
			throw new IllegalArgumentException("must end with: '" + last + "', found: '" + full + "'");
		}
		return beforeLast(full, last);
	}

	public static String camelCaseToUpperSnakeCase(String s) {
		if (!isCamelCase(s)) {
			throw new IllegalArgumentException("expected camel-case: '" + s + "'");
		}

		StringBuilder sb = new StringBuilder();
		for (char c : s.toCharArray()) {
			if (Character.isUpperCase(c)) {
				sb.append("_");
			}
			sb.append(Character.toUpperCase(c));
		}

		String upperSnakeCase = sb.toString();
		if (!isUpperSnakeCase(upperSnakeCase)) {
			throw new AssertionError();
		}
		return upperSnakeCase;
	}

	private static final String alphabet_lower = "abcdefghijklmnopqrstuvwxyz";
	private static final String alphabet_upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final String digits = "0123456789";


	public static boolean isCamelCase(String s) {
		return isFullyComprisedOfChars(s, alphabet_lower + alphabet_upper + digits) &&
				isPartlyComprisedOfChars(s, alphabet_lower);
	}

	public static boolean isUpperSnakeCase(String s) {
		return isFullyComprisedOfChars(s, alphabet_upper + digits + "_") &&
				isPartlyComprisedOfChars(s, alphabet_upper) &&
				!s.contains("__") &&
				!s.endsWith("_");
	}

	public static boolean isVariableName(String s) {
		return isCamelCase(s) || isUpperSnakeCase(s);
	}

	private static boolean isFullyComprisedOfChars(String s, String... charTables) {
		if (s.isEmpty()) {
			return false;
		}

		for (char c : s.toCharArray()) {
			if (Arrays.stream(charTables).mapToInt(e -> e.indexOf(c)).min().orElse(-1) == -1) {
				return false;
			}
		}
		return true;
	}

	private static boolean isPartlyComprisedOfChars(String s, String... charTables) {
		if (s.isEmpty()) {
			return false;
		}

		for (char c : s.toCharArray()) {
			if (Arrays.stream(charTables).mapToInt(e -> e.indexOf(c)).max().orElse(-1) != -1) {
				return true;
			}
		}
		return false;
	}

	public static List<String> splitWithoutRegex(String s, String delim) {
		return splitWithoutRegex(s, delim, Integer.MAX_VALUE);
	}

	public static List<String> splitWithoutRegex(String s, String delim, int maxPartCount) {
		// Work around the tricky API of String.split(...)
		//  - "".split("#").length == 0
		//  - "#".split("#").length == 0
		//  - "###".split("#").length == 0
		//  - "###-".split("#").length == 4
		// Issue: *empty* trailing elements are removed from the array.

		checkArgument(maxPartCount > 0, "maxPartCount must be > 0");

		List<String> parts = new ArrayList<>();
		while (parts.size() < maxPartCount - 1) {
			String before = before(s, delim, null);
			if (before == null) {
				break;
			}
			parts.add(before);
			s = after(s, delim);
		}
		parts.add(s);
		return parts;
	}
}
