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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.listener.ChainedRepositoryListener;

/**
 * Builds lists of artifacts, grouped by group-artifact-version coordinate
 * 
 */
final class ResolutionCollector extends AbstractRepositoryListener
{
	private static final class MapValue
	{
		/**
		 * {@code null} indicates the artifact was found in the ignored URL
		 */
		public Artifact artifact;

		public MapValue(Artifact artifact)
		{
			this.artifact = artifact;
		}
	}
	
	private final Map<ArtifactItem, MapValue> artifactMap = new HashMap<>();
	
	private final Log log;
	private final String ignoreRepoUrl;
	
	public ResolutionCollector(Log log, String ignoreRepoUrl)
	{
		this.log = log;
		this.ignoreRepoUrl = ignoreRepoUrl;
	}

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
				new DefaultRepositorySystemSession(repoSession);
		
		/*
		 * Forget any dependencies that were previously resolved
		 */
		newSession.setCache(new DefaultRepositoryCache());
		
		RepositoryListener listener = newSession.getRepositoryListener();
		newSession.setRepositoryListener(listener == null ?
				this
				: new ChainedRepositoryListener(this, listener));
		return newSession;
	}
	
	private boolean ignore(RepositoryEvent event)
	{
		if (ignoreRepoUrl == null)
		{
			return false;
		}
		ArtifactRepository repository = event.getRepository();
		return repository instanceof RemoteRepository
				&& Objects.equals(
						ignoreRepoUrl,
						((RemoteRepository) repository).getUrl());
	}
	
	@Override
	public void artifactResolved(RepositoryEvent event)
	{
		Artifact artifact = event.getArtifact();
		if (artifact.getFile() == null)
		{
			return;
		}
		
		if (log.isDebugEnabled())
		{
			log.debug(String.format(
					"Resolved %s @ %s", artifact, event.getRepository()));
		}
		
		ArtifactItem mapKey = new ArtifactItem(artifact);
		
		MapValue mapValue = artifactMap.get(mapKey);
		if (mapValue == null)
		{
			artifactMap.put(
					mapKey, new MapValue(ignore(event) ? null : artifact));
		}
		else if (mapValue.artifact != null && ignore(event))
		{
			mapValue.artifact = null;
		}
	}
	
	/**
	 * Obtains the collected sets of artifacts, grouped by
	 * group-artifact-version coordinate
	 * 
	 * @return
	 * The collected sets of artifacts
	 */
	public Collection<Collection<Artifact>> getArtifactSets()
	{
		Map<ArtifactPath, Collection<Artifact>> pathMap = new HashMap<>();
		for (Entry<ArtifactItem, MapValue> e: artifactMap.entrySet())
		{
			MapValue mapValue = e.getValue();
			if (mapValue.artifact == null)
			{
				continue;
			}
			ArtifactPath artifactPath = new ArtifactPath(e.getKey());
			Collection<Artifact> pathArtifacts = pathMap.get(artifactPath);
			if (pathArtifacts == null)
			{
				pathArtifacts = new ArrayList<>();
				pathMap.put(artifactPath, pathArtifacts);
			}
			pathArtifacts.add(mapValue.artifact);
		}
		return pathMap.values();
	}
}
