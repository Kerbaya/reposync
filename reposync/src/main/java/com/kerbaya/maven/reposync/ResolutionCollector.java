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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.util.listener.ChainedRepositoryListener;

/**
 * Builds lists of artifacts, grouped by group-artifact-version coordinate
 * 
 * @author Glenn.Lane@kerbaya.com
 *
 */
final class ResolutionCollector extends AbstractRepositoryListener
{
	private static final class Key
	{
		private final String groupId;
		private final String artifactId;
		private final String version;
		
		public Key(Artifact artifact)
		{
			groupId = artifact.getGroupId();
			artifactId = artifact.getArtifactId();
			version = artifact.getVersion();
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
			if (!(obj instanceof Key))
			{
				return false;
			}
			Key other = (Key) obj;
			return Objects.equals(groupId, other.groupId)
					&& Objects.equals(artifactId, other.artifactId)
					&& Objects.equals(version, other.version);
		}
	}

	private final Map<Key, Set<Artifact>> artifactMap = new HashMap<>();

	/**
	 * Creates a repository session where all artifact resolutions are 
	 * collected.  See {@link #getArtifactSets()} to access the collected
	 * artifacts.
	 * 
	 * @param repoSession
	 * The original repository session
	 * 
	 * @return
	 * A repository session where all artifact resolutions are collected
	 */
	public RepositorySystemSession createSession(
			RepositorySystemSession repoSession)
	{
		DefaultRepositorySystemSession newSession = 
				new DefaultRepositorySystemSession(repoSession)
						.setCache(new DefaultRepositoryCache());
		RepositoryListener listener = newSession.getRepositoryListener();
		newSession.setRepositoryListener(listener == null ?
				this
				: new ChainedRepositoryListener(this, listener));
		return newSession;
	}
	
	@Override
	public void artifactResolved(RepositoryEvent event)
	{
		Artifact artifact = event.getArtifact();
		if (artifact.getFile() != null)
		{
			Key key = new Key(artifact);
			Set<Artifact> set = artifactMap.get(new Key(artifact));
			if (set == null)
			{
				set = new HashSet<>();
				artifactMap.put(key, set);
			}
			set.add(artifact);
		}
	}
	
	/**
	 * Obtains the collected sets of artifacts, grouped by 
	 * group-artifact-version coordinate
	 * 
	 * @return
	 * The collected sets of artifacts
	 */
	public Collection<Set<Artifact>> getArtifactSets()
	{
		return artifactMap.values();
	}
}
