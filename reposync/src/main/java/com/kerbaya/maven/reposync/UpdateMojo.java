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

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
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

/**
 * Prepares a repository to support builds using provided dependencies
 * 
 * @author Glenn.Lane@kerbaya.com
 *
 */
@org.apache.maven.plugins.annotations.Mojo(
		name="update", 
		requiresProject=false, 
		threadSafe=true, 
		aggregator=true, 
		requiresDependencyResolution=ResolutionScope.TEST,
		requiresDependencyCollection=ResolutionScope.TEST)
public class UpdateMojo implements Mojo
{
	private Log log;

    @Parameter(defaultValue="${session}", readonly=true, required=true)
    private MavenSession session;
    
    @Parameter(property="repositoryId")
    private String repositoryId;
    
    @Parameter(property="url", required=true)
    private URL url;
    
    /**
     * The dependency artifact coordinates in the format
     * {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>}.
     * Multiple dependencies may be specified, separated by commas.
     */
    @Parameter(property="dependency", required=true)
    private String dependency;
    
    @Parameter(property="scope", defaultValue="runtime")
    private String scope;
    
    @Parameter(property="includeJavadoc", defaultValue="false")
    private boolean includeJavadoc;
    
    @Parameter(property="includeSources", defaultValue="false")
    private boolean includeSources;
    
    @Component
    private RepositorySystem repositorySystem;

    /*
     * Add an artifact with the same group-artifact-coordinate, but with 
     * a different classifier and extension
     */
    private static void addExtra(
    		Collection<? super ArtifactRequest> extras,
    		List<RemoteRepository> remoteRepos,
    		Artifact artifact, 
    		String classifier, 
    		String extension) 
    {
    	extras.add(new ArtifactRequest(
    			new DefaultArtifact(
    					artifact.getGroupId(), 
    					artifact.getArtifactId(),
    					classifier,
    					extension,
    					artifact.getVersion()),
    			remoteRepos,
    			null));
    }
    
	@Override
	public void execute() throws MojoExecutionException
	{
		ResolutionCollector collector = new ResolutionCollector();
		
		RepositorySystemSession repoSession = session.getRepositorySession();
		List<RemoteRepository> remoteRepos = 
				session.getCurrentProject().getRemoteProjectRepositories();

		/*
		 * We'll be using a collecting session during dependency resolution.  We
		 * need to not only include the final dependencies, but also the POM's 
		 * for all competing dependency candidates.  The target repository needs
		 * the POM's for these unused dependencies so that dependency analysis
		 * can arrive at the same conclusions afterward.
		 */
		RepositorySystemSession collectingSession = 
				collector.createSession(repoSession);
		
		Collection<ArtifactRequest> extras = includeJavadoc || includeSources ?
				new HashSet<ArtifactRequest>() : null;
						
		for (String dependencyItem: dependency.split(","))
		{
			/*
			 * We don't want to submit all dependencies at once: we're not 
			 * making a repository that supports a build that uses all these 
			 * dependencies simultaneously; we're making a repository that 
			 * supports builds that use any of these dependencies independently.
			 */
			final DependencyResult dependencyResult;
			try
			{
				dependencyResult = repositorySystem.resolveDependencies(
						collectingSession, 
						new DependencyRequest(
								new CollectRequest(
										Collections.singletonList(
												new Dependency(
														new DefaultArtifact(
																dependencyItem), 
														scope)), 
										null, 
										remoteRepos),
								null));
			}
			catch (DependencyResolutionException e)
			{
				throw new MojoExecutionException(
						"Failed to resolve dependencies for " + dependencyItem, 
						e);
			}
			if (extras == null)
			{
				/*
				 * No more artifacts needed
				 */
				continue;
			}
			
			/*
			 * Javadocs and/or sources were requested.  We're not looking at the
			 * collector results, because we only want Javadoc/sources for the
			 * "winning" dependencies.
			 * 
			 * The collector will include the POM's for disqualified 
			 * dependencies: they are included when updating the target 
			 * repository, to foster the conclusion to disqualify their version 
			 * during future builds.
			 */
			for (ArtifactResult ar: dependencyResult.getArtifactResults())
			{
				Artifact artifact = ar.getArtifact();
				if (!"jar".equals(artifact.getExtension()))
				{
					/*
					 * Only JAR artifacts have associated Javadoc/sources
					 */
					continue;
				}
				String classifier = artifact.getClassifier();
				if ("javadoc".equals(classifier) 
						|| "sources".equals(classifier))
				{
					/*
					 * Javadoc and source JARs don't have Javadocs or sources.
					 */
					continue;
				}
				
				if (includeJavadoc)
				{
					addExtra(extras, remoteRepos, artifact, "javadoc", "jar");
				}
				if (includeSources)
				{
					addExtra(extras, remoteRepos, artifact, "sources", "jar");
				}
			}
		}
		
		/*
		 * All dependencies are collected and resolved.  Now to resolve their 
		 * Javadoc and sources (if any)
		 */
		if (extras != null && !extras.isEmpty())
		{
			try
			{
				/*
				 * Like the dependencies above, the collector will hear about
				 * all artifact resolutions: so we don't need to look at these
				 * results
				 */
				repositorySystem.resolveArtifacts(collectingSession, extras);
			}
			catch (ArtifactResolutionException e)
			{
				/*
				 * Missing Javadoc/sources is fine
				 */
			}
		}
		
		RemoteRepository repository = new RemoteRepository.Builder(
        		repositoryId, "default", url.toString()).build();
        
		/*
		 * The collector returns the artifacts grouped by group-artifact-version
		 * coorinate
		 */
		for (Set<Artifact> artifactSet: collector.getArtifactSets())
		{
			DeployRequest dr = new DeployRequest();
			dr.setArtifacts(artifactSet);
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
