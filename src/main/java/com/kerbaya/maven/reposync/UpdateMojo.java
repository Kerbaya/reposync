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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.transfer.ArtifactNotFoundException;

/**
 * Prepares a repository to support builds using provided dependencies
 * 
 * @author Glenn.Lane@kerbaya.com
 *
 */
@org.apache.maven.plugins.annotations.Mojo(
		name="update", 
		requiresProject=false, 
		threadSafe=true)
public class UpdateMojo implements Mojo
{
	private Log log;

    @Parameter(defaultValue="${session}", readonly=true, required=true)
    private MavenSession session;
    
    /**
     * The ID of the repository that should be updated with dependencies.  If
     * used in combination with the {@link #repositoryUrl} parameter, the updated 
     * repository is the provided URL used in combination with proxy/login 
     * settings associated to the provided repository ID.  At least one of 
     * {@link #repositoryId} or {@link #repositoryUrl} must be provided.
     */
    @Parameter(property="repositoryId")
    private String repositoryId;
    
    /**
     * The URL of the repository that should be updated with dependencies.  If
     * used in combination with the {@link #repositoryId} parameter, the 
     * updated repository is the provided URL used in combination with 
     * proxy/login settings associated to the provided repository ID.  At least 
     * one of {@link #repositoryId} or {@link #repositoryUrl} must be provided.
     */
    @Parameter(property="repositoryUrl")
    private String repositoryUrl;
    
    /**
     * The artifacts to update when executing the plugin within a POM 
     * configuration 
     */
    @Parameter
    private ArtifactItem[] artifactItems;
    
    /**
     * The artifact to update when executing the plugin from the command-line, 
     * in the format
     * <code>&lt;groupId&gt;:&lt;artifactId&gt;[:&lt;extension&gt;[:&lt;classifier&gt;]]:&lt;version&gt;</code>.
     * Multiple artifacts may be specified, separated by a comma, or one or more 
     * whitespace characters.  Use {@link #artifactItems} when executing the 
     * plugin within a POM configuration.
     */
    @Parameter(property="artifact")
    private String artifact;
    
    /**
     * Extra artifacts to include when executing the plugin within a POM
     * configuration
     */
    @Parameter
    private ExtraItem[] extraItems;
    
    /**
     * Extra artifacts to include when executing the plugin from the command-
     * line, in the format
     * <code>&lt;extension&gt;[:&lt;classifier&gt;]</code>.  Multiple extras may
     * be specified, separated by a comma, or one or more whitespace characters.  For 
     * example, "jar:sources jar:javadoc"
     */
    @Parameter(property="extra")
    private String extra;
    
    /**
     * If <code>true</code>, artifacts are updated even when they are determined 
     * to have originated from the target repository.
     */
    @Parameter(defaultValue="false", property="force")
    private boolean force;
    
    /**
     * If <code>true</code>, the plugin execution will fail when no artifacts
     * have been specified.
     */
    @Parameter(defaultValue="true", property="failOnNoArtifact")
    private boolean failOnNoArtifact;
    
    /**
     * If <code>true</code>, snapshot artifacts using timestamps are deployed to the target repository as releases 
     */
    @Parameter(property="snapshotAsRelease", defaultValue="false")
    private boolean snapshotAsRelease;
    
    @Component
    private RepositorySystem repositorySystem;
    
	private static final String findRepositoryUrlById(
			Collection<RemoteRepository> repos, String repositoryId) 
					throws MojoExecutionException 
	{
		for (RemoteRepository repo: repos)
		{
			if (Objects.equals(repositoryId, repo.getId()))
			{
				return repo.getUrl();
			}
		}
		throw new MojoExecutionException(String.format(
				"Could not find repositoryId=%s", repositoryId));
	}

	@Override
	public void execute() throws MojoExecutionException
	{
		if (repositoryId == null && repositoryUrl == null)
		{
			throw new MojoExecutionException(
					"One of repositoryId or repositoryUrl is required");
		}
		
		
		Set<ArtifactItem> finalArtifactItems = new HashSet<>();
		if (artifactItems != null)
		{
			finalArtifactItems.addAll(Arrays.asList(artifactItems));
		}
		if (artifact != null)
		{
			for (String token: Utils.getTokens(artifact))
			{
				finalArtifactItems.add(new ArtifactItem(token));
			}
		}
		
		if (finalArtifactItems.isEmpty())
		{
			if (failOnNoArtifact)
			{
				throw new MojoExecutionException("no artifact specified");
			}
			return;
		}
		
		Set<ExtraItem> finalExtraItems = new HashSet<>();
		if (extraItems != null)
		{
			finalExtraItems.addAll(Arrays.asList(extraItems));
		}
		if (extra != null)
		{
			for (String token: Utils.getTokens(extra))
			{
				finalExtraItems.add(new ExtraItem(token));
			}
		}
		
		List<RemoteRepository> remoteRepos = 
				session.getCurrentProject().getRemoteProjectRepositories();
		
		final String ignoreRepoUrl;
		if (force)
		{
			ignoreRepoUrl = null;
		}
		else
		{
			ignoreRepoUrl = repositoryUrl == null ?
					findRepositoryUrlById(remoteRepos, repositoryId) 
					: repositoryUrl;	
		}

		ResolutionCollector collector = new ResolutionCollector(
				log, ignoreRepoUrl);
		
		RepositorySystemSession repoSession = session.getRepositorySession();
		
		
		/*
		 * We'll be using a collecting session during dependency resolution.  We
		 * need to not only include the final dependencies, but also the POM's 
		 * for all competing dependency candidates.  The target repository needs
		 * the POM's for these unused dependencies so that dependency analysis
		 * can arrive at the same conclusions afterward.
		 */
		RepositorySystemSession collectingSession = 
				collector.createSession(repoSession);
		
		final Set<ArtifactItem> artifactSetForExtras = 
				finalExtraItems.isEmpty() ? null : new HashSet<ArtifactItem>();
		
		/*
		 * We don't want to submit all dependencies at once: we're not making a 
		 * repository that supports a build that uses all these dependencies 
		 * simultaneously; we're making a repository that supports builds that 
		 * use any of these dependencies independently.
		 */
		for (ArtifactItem artifactItem: finalArtifactItems)
		{
			final DependencyResult dependencyResult;
			try
			{
				Dependency dependency = new Dependency(
						artifactItem.toArtifact(), null);
				dependencyResult = repositorySystem.resolveDependencies(
						collectingSession, 
						new DependencyRequest(
								new CollectRequest(
										Collections.singletonList(dependency),
										null, 
										remoteRepos),
								null));
			}
			catch (DependencyResolutionException e)
			{
				throw new MojoExecutionException(
						"Failed to resolve dependencies for " + artifactItem, 
						e);
			}
			
			if (artifactSetForExtras == null)
			{
				/*
				 * No extras were requested, so we can continue to the next
				 * artifact
				 */
				continue;
			}
			
			/*
			 * Extras were requested.  We're not looking at the collector, 
			 * because we only want extras for the "winning" dependencies.
			 */
			for (ArtifactResult ar: dependencyResult.getArtifactResults())
			{
				artifactSetForExtras.add(new ArtifactItem(ar.getArtifact()));
			}
		}
		
		
		/*
		 * Now resolve extras, if any
		 */
		if (artifactSetForExtras != null)
		{
			/*
			 * Extras were requested.  We'll weed-out the ones that were already
			 * resolved
			 */
			Set<ArtifactPath> processedPaths = new HashSet<>();
			Collection<ArtifactRequest> extraRequests = new ArrayList<>();
			for (ArtifactItem artifactForExtras: artifactSetForExtras)
			{
				if ("pom".equals(artifactForExtras.getExtension())
						&& "".equals(artifactForExtras.getClassifier()))
				{
					/*
					 * We don't get extras for POM's, since they are often
					 * resolved through the course of disqualifying artifacts as 
					 * dependencies
					 */
					continue;
				}
				
				ArtifactPath artifactPath = new ArtifactPath(artifactForExtras);
				if (!processedPaths.add(artifactPath))
				{
					/*
					 * We've already added the requests for this artifact path's
					 * extras
					 */
					continue;
				}
				
				for (ExtraItem extra: finalExtraItems)
				{
					ArtifactItem extraToResolve = new ArtifactItem(
							artifactPath, extra);
					if (artifactSetForExtras.contains(extraToResolve))
					{
						/*
						 * This extra was already resolved during dependency
						 * resolution
						 */
						continue;
					}
					extraRequests.add(new ArtifactRequest(
							extraToResolve.toArtifact(),
			    			remoteRepos,
			    			null));
				}
			}
			
			if (!extraRequests.isEmpty())
			{
				try
				{
					/*
					 * Like the dependencies above, the collector will hear 
					 * about all artifact resolutions: so we don't need to look 
					 * at the resolution results
					 */
					repositorySystem.resolveArtifacts(
							collectingSession, extraRequests);
				}
				catch (ArtifactResolutionException e)
				{
					for (ArtifactResult r: e.getResults())
					{
						if (r.isResolved())
						{
							continue;
						}
						for (Exception e2: r.getExceptions())
						{
							if (!(e2 instanceof ArtifactNotFoundException))
							{
								throw new MojoExecutionException(
										String.format(
												"Could not resolve extra: %s", 
												r.getRequest().getArtifact()),
										e2);
							}
						}
					}
				}
			}
		}
		
		
		/*
		 * All artifacts have been collected.  Now we push them to the remote
		 * repository.
		 */
		RemoteRepository repository = new RemoteRepository.Builder(
        		repositoryId, "default", repositoryUrl)
				.build();
        
		/*
		 * The collector returns the artifacts grouped by group-artifact-version
		 * coordinate.  We want to update all artifacts from the same path 
		 * in the same deploy request (current Maven version seems to trample 
		 * the old version meta-data).
		 */
		for (Collection<Artifact> artifactSet: collector.getArtifactSets())
		{
			DeployRequest dr = new DeployRequest();
			dr.setArtifacts(rewriteArtifacts(artifactSet));
			dr.setRepository(repository);
			try
			{
				repositorySystem.deploy(repoSession, dr);
			}
			catch (DeploymentException e)
			{
				throw new MojoExecutionException("Deployment failed", e);
			}
		}
	}
	
	private Collection<Artifact> rewriteArtifacts(Collection<Artifact> artifactSet)
	{
		if (snapshotAsRelease)
		{
			Collection<Artifact> result = new ArrayList<>(artifactSet.size());
			for (Artifact a: artifactSet)
			{
				result.add(ReleaseArtifact.timestampSnapshotAsRelease(a));
			}
			return result;
		}
		
		return artifactSet;
	}

	@Override
	public void setLog(Log log)
	{
		this.log = log;
	}

	@Override
	public Log getLog()
	{
		return log;
	}
}
