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

import java.util.Objects;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExtraItem
{
	private static final Pattern PATTERN = Pattern.compile(
			"([^:\\s]*)(?::([^:\\s]*))?");
	
	/**
	 * the (file) extension of an artifact, for example "jar"
	 * 
	 * @parameter
	 */
	private String extension;
	
	/**
	 * the classifier of an artifact, for example "sources"
	 * 
	 * @parameter
	 */
	private String classifier;
	
	private static MatchResult matchPattern(CharSequence str)
	{
		Matcher m = PATTERN.matcher(str);
		if (!m.matches())
		{
			throw new IllegalArgumentException(String.format(
					"invalid extra \"%s\"",  str));
		}
		return m;
	}

	public ExtraItem()
	{
		extension = "";
		classifier = "";
	}
	
	public ExtraItem(CharSequence str)
	{
		this(matchPattern(str));
	}
	
	private ExtraItem(MatchResult m)
	{
		this(m.group(1), Utils.nullToEmpty(m.group(2)));
	}
	
	public ExtraItem(String extension, String classifier)
	{
		this.extension = Objects.requireNonNull(extension);
		this.classifier = Objects.requireNonNull(classifier);
	}

	public String getExtension()
	{
		return extension;
	}
	public void setExtension(String extension)
	{
		this.extension = Objects.requireNonNull(extension);
	}
	
	public String getClassifier()
	{
		return classifier;
	}
	public void setClassifier(String classifier)
	{
		this.classifier = Objects.requireNonNull(classifier);
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(extension, classifier);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj == this)
		{
			return true;
		}
		if (obj == null || !(ExtraItem.class.equals(obj.getClass())))
		{
			return false;
		}
		ExtraItem other = (ExtraItem) obj;
		return Objects.equals(extension, other.extension)
				&& Objects.equals(classifier, other.classifier);
	}
	
	@Override
	public String toString()
	{
		return new StringBuilder()
				.append(extension)
				.append(':')
				.append(classifier)
				.toString();
	}
}
