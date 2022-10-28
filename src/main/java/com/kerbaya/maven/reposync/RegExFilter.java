/*
 * Copyright 2022 Kerbaya Software
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

import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.eclipse.aether.artifact.Artifact;

class RegExFilter implements Predicate<Artifact>
{
	private static final char SEPARATOR = ':';
	
	private final Pattern pattern;
	
	public RegExFilter(String pattern)
	{
		this.pattern = Pattern.compile(pattern);
	}
	
	@Override
	public boolean test(Artifact t)
	{
		StringBuilder sb = new StringBuilder()
				.append(t.getGroupId())
				.append(SEPARATOR)
				.append(t.getArtifactId())
				.append(SEPARATOR)
				.append(t.getExtension());
		
		String classifier = t.getClassifier();
		if (classifier != null && !classifier.isEmpty())
		{
			sb.append(SEPARATOR);
			sb.append(classifier);
		}
		
		sb.append(SEPARATOR)
				.append(t.getVersion());
				
		return pattern.matcher(sb).matches();
	}
}
