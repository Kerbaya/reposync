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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public final class ArtifactItem
{
	private static final String DEFAULT_EXTENSION = "jar";
	private static final int MAX_TOKEN_COUNT = 5;
	
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
	 * the (file) extension of an artifact, for example "jar".  If omitted, "jar" is used.
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
	
	/**
	 * the version of an artifact, for example "1.0-20100529.1213-1".  If omitted, dependency management will be used.
	 * 
	 * @parameter
	 */
	private String version;
	
	public ArtifactItem()
	{
		extension = DEFAULT_EXTENSION;
		classifier = "";
		version = "";
	}
	
	private static IllegalArgumentException invalidArtifact(String str)
	{
		return new IllegalArgumentException(String.format("invalid artifact \"%s\"", str));
	}
	
	public ArtifactItem(String str)
	{
		List<String> tokens = new ArrayList<String>(MAX_TOKEN_COUNT);
		int lastIdx = 0;
		int idx;
		while ((idx = str.indexOf(':', lastIdx)) != -1)
		{
			tokens.add(str.substring(lastIdx, idx));
			if (tokens.size() == MAX_TOKEN_COUNT)
			{
				throw invalidArtifact(str);
			}
			lastIdx = idx + 1;
		}
		tokens.add(str.substring(lastIdx));
		
		if (tokens.size() < 3)
		{
			throw invalidArtifact(str);
		}
		
		groupId = tokens.get(0);
		artifactId = tokens.get(1);
		
		if (groupId.isEmpty() || artifactId.isEmpty())
		{
			throw invalidArtifact(str);
		}
		
		if (tokens.size() == 3)
		{
			extension = DEFAULT_EXTENSION;
			classifier = "";
			version = tokens.get(2);
		}
		else
		{
			extension = tokens.get(2);
			if (extension.isEmpty())
			{
				throw invalidArtifact(str);
			}
			
			if (tokens.size() == 4)
			{
				classifier = "";
				version = tokens.get(3);
			}
			else
			{
				classifier = tokens.get(3);
				version = tokens.get(4);
			}
		}
	}
	
	public ArtifactItem(
			String groupId, 
			String artifactId, 
			String extension, 
			String classifier,
			String version)
	{
		this.groupId = Objects.requireNonNull(groupId);
		this.artifactId = Objects.requireNonNull(artifactId);
		this.extension = Objects.requireNonNull(extension);
		this.classifier = Objects.requireNonNull(classifier);
		this.version = Objects.requireNonNull(version);
	}

	public ArtifactItem(Artifact artifact)
	{
		this(
				artifact.getGroupId(),
				artifact.getArtifactId(),
				artifact.getExtension(),
				artifact.getClassifier(),
				artifact.getVersion());
	}
	
	public ArtifactItem(ArtifactPath artifactPath, ExtraItem extra)
	{
		this(
				artifactPath.getGroupId(),
				artifactPath.getArtifactId(),
				extra.getExtension(),
				extra.getClassifier(),
				artifactPath.getVersion());
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
	public String toString()
	{
		StringBuilder sb = new StringBuilder()
				.append(groupId)
				.append(':')
				.append(artifactId);
		
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
		
		sb.append(':')
			.append(version);

		return sb.toString();
	}
	
	public Artifact toArtifact()
	{
		return new DefaultArtifact(
				groupId, artifactId, classifier, extension, version);
	}
}
