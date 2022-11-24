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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.maven.model.DependencyManagement;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
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

import lombok.Getter;
import lombok.Setter;

/**
 * Prepares a repository to support builds using provided dependencies
 */
@org.apache.maven.plugins.annotations.Mojo(
		name="update", 
		requiresProject=false, 
		threadSafe=true)
public class UpdateMojo implements Mojo
{
	@Getter
	@Setter
	private Log log;

    @Parameter(defaultValue="${project}", readonly=true, required=true)
    private MavenProject project;
    
	@Parameter(defaultValue="${repositorySystemSession}", required=true, readonly=true)
	private RepositorySystemSession rss;
	
    /**
     * The ID of the repository that should be updated with dependencies.  If
     * used in combination with the {@link #repositoryUrl} parameter, the updated 
     * repository is the provided URL used in combination with proxy/login 
     * settings associated to the provided repository ID.
     */
    @Parameter(property="repositoryId", defaultValue="remote-repository", required=true)
    private String repositoryId;
    
    /**
     * The URL of the repository that should be updated with dependencies.  Used 
     * in combination with the {@link #repositoryId} parameter, the 
     * updated repository is the provided URL used in combination with 
     * proxy/login settings associated to the provided repository ID.
     */
    @Parameter(property="repositoryUrl", required=true)
    private String repositoryUrl;
    
    /**
     * The artifacts to update when executing the plugin within a POM 
     * configuration 
     */
    @Parameter
    private List<ArtifactItem> artifactItems;
    
    /**
     * The artifact to update when executing the plugin from the command-line, 
     * in the format <code>&lt;groupId&gt;:&lt;artifactId&gt;[:&lt;extension&gt;[:&lt;classifier&gt;]]:[version]</code>.
     * If {@code version} is omitted, dependency management will be used to determine the version.
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
    private List<ExtraItem> extraItems;
    
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
    
    /**
     * a regular expression against which an artifact must be matched before being deployed to the target repository.
     * artifacts are matched using {@code <groupId>:<artifactId>:<extension>[:<classifier>]:<version>}
     */
    @Parameter(property="filter")
    private String filter;
    
    /**
     * a filter against which an artifact must be matched before deploying to the repository.  Values are
     * <ul>
     * <li>{@code SNAPSHOT}: only include artifacts with snapshot versions</li>
     * <li>{@code RELEASE}: only include artifacts with release versions</li>
     * </ul>
     */
    @Parameter(property="filterType")
    private Filter filterType;
    
    @Inject
    private RepositorySystem repositorySystem;
    
    private RemoteRepository buildDistRepo()
    {
        RemoteRepository remoteRepo = new RemoteRepository.Builder(repositoryId, "default", repositoryUrl).build();
        
        boolean hasAuthentication = remoteRepo.getAuthentication() != null;
        boolean hasProxy = remoteRepo.getProxy() != null;
        
        if (hasAuthentication && hasProxy)
        {
        	return remoteRepo;
        }
        
        RemoteRepository.Builder builder = new RemoteRepository.Builder(remoteRepo);
        if (!hasAuthentication)
        {
        	builder.setAuthentication(rss.getAuthenticationSelector().getAuthentication(remoteRepo));
        }
        
        if (!hasProxy)
        {
        	builder.setProxy(rss.getProxySelector().getProxy(remoteRepo));
        }
        
        return builder.build();
    }
    
    private static void removeNoVersionArtifacts(
    		Iterable<ArtifactItem> items, Consumer<? super ArtifactItem> addTo)
    {
    	Iterator<ArtifactItem> iter = items.iterator();
    	while (iter.hasNext())
    	{
    		ArtifactItem next = iter.next();
    		if ("".equals(next.getVersion()))
    		{
    			addTo.accept(next);
    			iter.remove();
    		}
    	}
    }
    
    private Map<ManagedDependencyKey, String> getManagedDependencies()
    {
    	DependencyManagement dm = project.getDependencyManagement();
    	return dm == null ?
    			Collections.emptyMap()
    			: dm.getDependencies().stream()
    					.collect(Collectors.toMap(
    							ManagedDependencyKey::new, 
    							org.apache.maven.model.Dependency::getVersion));
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
			finalArtifactItems.addAll(artifactItems);
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
		
		List<ArtifactItem> noVersionArtifacts = new ArrayList<>();
		removeNoVersionArtifacts(finalArtifactItems, noVersionArtifacts::add);
		
		if (!noVersionArtifacts.isEmpty())
		{
			Map<ManagedDependencyKey, String> dm = getManagedDependencies();
			for (ArtifactItem noVersionArtifact: noVersionArtifacts)
			{
				String version = dm.get(new ManagedDependencyKey(
						noVersionArtifact.getGroupId(),
						noVersionArtifact.getArtifactId(),
						noVersionArtifact.getExtension(),
						noVersionArtifact.getClassifier()));
				if (version == null)
				{
					throw new MojoExecutionException("could not find managed dependency for " + noVersionArtifact);
				}
				
				finalArtifactItems.add(new ArtifactItem(
						noVersionArtifact.getGroupId(),
						noVersionArtifact.getArtifactId(),
						noVersionArtifact.getExtension(),
						noVersionArtifact.getClassifier(),
						version));
			}
		}
		
		Set<ExtraItem> finalExtraItems = new HashSet<>();
		if (extraItems != null)
		{
			finalExtraItems.addAll(extraItems);
		}
		if (extra != null)
		{
			for (String token: Utils.getTokens(extra))
			{
				finalExtraItems.add(new ExtraItem(token));
			}
		}
		
		List<RemoteRepository> remoteRepos = project.getRemoteProjectRepositories();
		
		ResolutionCollector collector = new ResolutionCollector(log, force ? null : repositoryUrl);
		
		/*
		 * We'll be using a collecting session during dependency resolution.  We
		 * need to not only include the final dependencies, but also the POM's 
		 * for all competing dependency candidates.  The target repository needs
		 * the POM's for these unused dependencies so that dependency analysis
		 * can arrive at the same conclusions afterward.
		 */
		RepositorySystemSession collectingSession = collector.createSession(rss);
		
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
			DependencyRequest dr = new DependencyRequest(
					new CollectRequest(
							Collections.singletonList(new Dependency(artifactItem.toArtifact(), null)),
							null, 
							remoteRepos),
					null);
			final DependencyResult dependencyResult;
			try
			{
				dependencyResult = repositorySystem.resolveDependencies(collectingSession, dr);
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
		RemoteRepository distRepo = buildDistRepo();
        
		/*
		 * The collector returns the artifacts grouped by group-artifact-version
		 * coordinate.  We want to update all artifacts from the same path 
		 * in the same deploy request (current Maven version seems to trample 
		 * the old version meta-data).
		 */
		for (Collection<Artifact> artifactSet: collector.getArtifactSets())
		{
			Stream<Artifact> toDeploy = artifactSet.stream();
			if (filter != null)
			{
				toDeploy = toDeploy.filter(new RegExFilter(filter));
			}
			
			if (filterType != null)
			{
				toDeploy = toDeploy.filter(filterType);
			}
			
			if (snapshotAsRelease)
			{
				toDeploy = toDeploy.map(ReleaseArtifact::timestampSnapshotAsRelease);
			}
			
			DeployRequest dr = new DeployRequest();
			dr.setArtifacts(toDeploy.collect(Collectors.toList()));
			dr.setRepository(distRepo);
			try
			{
				repositorySystem.deploy(rss, dr);
			}
			catch (DeploymentException e)
			{
				throw new MojoExecutionException("Deployment failed", e);
			}
		}
	}
}
