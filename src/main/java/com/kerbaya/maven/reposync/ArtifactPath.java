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

import org.eclipse.aether.artifact.Artifact;

final class ArtifactPath
{
	private final String groupId;
	private final String artifactId;
	private final String version;
	
	public ArtifactPath(Artifact artifact)
	{
		this(
				artifact.getGroupId(),
				artifact.getArtifactId(),
				artifact.getVersion());
	}
	
	public ArtifactPath(ArtifactItem artifactItem)
	{
		this(
				artifactItem.getGroupId(), 
				artifactItem.getArtifactId(), 
				artifactItem.getVersion());
	}
	
	public ArtifactPath(String groupId, String artifactId, String version)
	{
		this.groupId = Objects.requireNonNull(groupId);
		this.artifactId = Objects.requireNonNull(artifactId);
		this.version = Objects.requireNonNull(version);
	}
	
	public String getGroupId()
	{
		return groupId;
	}
	
	public String getArtifactId()
	{
		return artifactId;
	}
	
	public String getVersion()
	{
		return version;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(groupId, artifactId, version);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj == this)
		{
			return true;
		}
		if (obj == null || !(ArtifactPath.class.equals(obj.getClass())))
		{
			return false;
		}
		ArtifactPath other = (ArtifactPath) obj;
		return Objects.equals(groupId, other.groupId)
				&& Objects.equals(artifactId, other.artifactId)
				&& Objects.equals(version, other.version);
	}
	
	@Override
	public String toString()
	{
		return new StringBuilder()
				.append(groupId)
				.append(':')
				.append(artifactId)
				.append(':')
				.append(version)
				.toString();
	}

}
