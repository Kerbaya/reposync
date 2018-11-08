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

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

public final class ArtifactItem
{
	private static final String DEFAULT_EXTENSION = "jar";
	
	private static final Pattern PATTERN = Pattern.compile(
			"([^:\\s]+):([^:\\s]+):([^:\\s]+)(?::([^:\\s]*)(?::([^:\\s]*))?)?");
	
	/**
	 * the group identifier of an artifact, for example "org.apache.maven"
	 * 
	 * @parameter
	 * @required
	 */
	private String groupId;
	
	/**
	 * the artifact identifier of an artifact, for example "maven-model"
	 * 
	 * @parameter
	 * @required
	 */
	private String artifactId;
	
	/**
	 * the version of an artifact, for example "1.0-20100529-1213"
	 * 
	 * @parameter
	 * @required
	 */
	private String version;
	
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
	
	
	public ArtifactItem()
	{
		extension = DEFAULT_EXTENSION;
		classifier = "";
	}
	
	private static MatchResult matchPattern(CharSequence str)
	{
		Matcher m = PATTERN.matcher(str);
		if (!m.matches())
		{
			throw new IllegalArgumentException(String.format(
					"invalid artifact \"%s\"",  str));
		}
		return m;
	}
	
	public ArtifactItem(CharSequence str)
	{
		this(matchPattern(str));
	}
	
	private ArtifactItem(MatchResult m)
	{
		this(
				m.group(1),
				m.group(2),
				m.group(3),
				Utils.nullToDefault(m.group(4), DEFAULT_EXTENSION),
				Utils.nullToEmpty(m.group(5)));
	}
	
	public ArtifactItem(
			String groupId, 
			String artifactId, 
			String version,
			String extension, 
			String classifier)
	{
		this.groupId = Objects.requireNonNull(groupId);
		this.artifactId = Objects.requireNonNull(artifactId);
		this.version = Objects.requireNonNull(version);
		this.extension = Objects.requireNonNull(extension);
		this.classifier = Objects.requireNonNull(classifier);
	}

	public ArtifactItem(Artifact artifact)
	{
		this(
				artifact.getGroupId(),
				artifact.getArtifactId(),
				artifact.getVersion(),
				artifact.getExtension(),
				artifact.getClassifier());
	}
	
	public ArtifactItem(ArtifactPath artifactPath, ExtraItem extra)
	{
		this(
				artifactPath.getGroupId(),
				artifactPath.getArtifactId(),
				artifactPath.getVersion(),
				extra.getExtension(),
				extra.getClassifier());
	}

	private static String requireNonEmpty(String str)
	{
		if (str.isEmpty())
		{
			throw new IllegalArgumentException();
		}
		return str;
	}
	
	public String getGroupId()
	{
		return groupId;
	}
	public void setGroupId(String groupId)
	{
		this.groupId = requireNonEmpty(groupId);
	}
	
	public String getArtifactId()
	{
		return artifactId;
	}
	public void setArtifactId(String artifactId)
	{
		this.artifactId = requireNonEmpty(artifactId);
	}
	
	public String getVersion()
	{
		return version;
	}
	public void setVersion(String version)
	{
		this.version = requireNonEmpty(version);
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
		return Objects.hash(
				groupId, artifactId, version, extension, classifier);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj == this)
		{
			return true;
		}
		if (obj == null || !(ArtifactItem.class.equals(obj.getClass())))
		{
			return false;
		}
		ArtifactItem other = (ArtifactItem) obj;
		return Objects.equals(groupId, other.groupId)
				&& Objects.equals(artifactId, other.artifactId)
				&& Objects.equals(version, other.version)
				&& Objects.equals(extension, other.extension)
				&& Objects.equals(classifier, other.classifier);
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder()
				.append(groupId)
				.append(':')
				.append(artifactId)
				.append(':')
				.append(version);
		if (!extension.isEmpty() || !classifier.isEmpty())
		{
			sb.append(':');
			sb.append(extension);
			if (!classifier.isEmpty())
			{
				sb.append(':');
				sb.append(classifier);
			}
		}
		return sb.toString();
	}
	
	public Artifact toArtifact()
	{
		return new DefaultArtifact(
				groupId, artifactId, classifier, extension, version);
	}
}
