/*
 * Copyright 2018 Kerbaya Software
 * 
 * This file is part of reposync. 
 * 
 * reposync is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * reposync is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with reposync.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kerbaya.maven.reposync;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

final class Utils
{
	private static final Pattern TOKEN_SEPARATOR = Pattern.compile("\\s+|,");

	private Utils() {}
	
	/**
	 * Converts {@code null} to an empty string
	 * 
	 * @param str
	 * The string to convert
	 * 
	 * @return
	 * An empty string if {@code str} is {@code null}, otherwise {@code str}
	 */
	public static String nullToEmpty(String str)
	{
		return nullToDefault(str, "");
	}
	
	/**
	 * Converts {@code null} or empty string to a default value
	 * 
	 * @param str
	 * The string to convert
	 * 
	 * @param def
	 * The default value
	 * 
	 * @return
	 * {@code def} if {@code str} is {@code null} or empty, otherwise 
	 * {@code str}
	 */
	public static String nullToDefault(String str, String def)
	{
		return str == null ? def : str;
	}
	
	/**
	 * Separates a string that is delimited by white-spaces
	 * 
	 * @param str
	 * The string to separate
	 * 
	 * @return
	 * A list of tokens in {@code str} that were separated by white-spaces
	 */
	public static List<String> getTokens(CharSequence str)
	{
		if (str == null)
		{
			return Collections.emptyList();
		}
		List<String> tokens = Arrays.asList(TOKEN_SEPARATOR.split(str));
		if (!tokens.isEmpty() && tokens.get(0).isEmpty())
		{
			tokens = tokens.subList(1, tokens.size());
		}
		return Collections.unmodifiableList(tokens);
	}
}
