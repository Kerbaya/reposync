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
import java.util.Collections;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.deploy.ArtifactDeployer;
import org.apache.maven.shared.artifact.deploy.ArtifactDeployerException;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.dependencies.resolve.DependencyResolverException;

@org.apache.maven.plugins.annotations.Mojo(
		name="update", requiresProject=false, threadSafe=true)
public class UpdateMojo implements Mojo
{
	private Log log;

    @Parameter(defaultValue="${session}", readonly=true, required=true)
    private MavenSession session;
    
    @Parameter(property="repositoryId")
    private String repositoryId;
    
    @Parameter(property="url", required=true)
    private URL url;
    
    @Parameter(property="dependency", required=true)
    private String dependency;
    
    @Parameter(property="scope", defaultValue="runtime")
    private String scope;
    
    @Parameter(property="includeJavadoc", defaultValue="false")
    private boolean includeJavadoc;
    
    @Parameter(property="includeSources", defaultValue="false")
    private boolean includeSources;
    
    @Parameter(property="includePom", defaultValue="true")
    private boolean includePom;
    
    @Parameter(property="includeParentPoms", defaultValue="true")
    private boolean includeParentPoms;
    
    @Component
    private DependencyResolver dependencyResolver;

    @Component
    private ArtifactDeployer artifactDeployer;
    
    @Component
    private ArtifactResolver artifactResolver;
    
    @Component
    private ArtifactHandlerManager artifactHandlerManager;
    
    @Component
    private ProjectBuilder projectBuilder;
    
	@Override
	public void execute() throws MojoExecutionException
	{
		ProjectBuildingRequest request = session.getProjectBuildingRequest();
		DeploymentPlan dp = new DeploymentPlan(
				artifactResolver, 
				request,
				projectBuilder, 
				includeJavadoc, 
				includeSources, 
				includePom, 
				includeParentPoms);
		for (String dependencyItem: dependency.split(","))
		{
			String[] dependencyTokens = dependencyItem.split(":");
			if (dependencyTokens.length < 3 || dependencyTokens.length > 5)
			{
				throw new MojoExecutionException(
						"Invalid dependency: " + dependencyItem);
			}
			Dependency dep = new Dependency();
			dep.setGroupId(dependencyTokens[0]);
			dep.setArtifactId(dependencyTokens[1]);
			dep.setVersion(dependencyTokens[2]);
			if (dependencyTokens.length > 3)
			{
				dep.setType(dependencyTokens[3]);
				if (dependencyTokens.length > 4)
				{
					dep.setClassifier(dependencyTokens[4]);
				}
			}
			dep.setScope(scope);
			final Iterable<ArtifactResult> dependencyArtifactResults;
			try
			{
				dependencyArtifactResults = 
						dependencyResolver.resolveDependencies(
								request, 
								Collections.singleton(dep), 
								null, 
								null);
			}
			catch (DependencyResolverException e)
			{
				throw new MojoExecutionException("Error resolving " + dep, e);
			}
			for (ArtifactResult dependencyArtifactResult: 
					dependencyArtifactResults)
			{
				dp.addArtifact(dependencyArtifactResult.getArtifact());
			}
		}
		ArtifactRepository repo = new MavenArtifactRepository(
        		repositoryId, 
        		url.toString(), 
        		new DefaultRepositoryLayout(), 
        		new ArtifactRepositoryPolicy(),
        		new ArtifactRepositoryPolicy());
		for (Set<Artifact> artifactSet: dp.getArtifactSets())
		{
			try
			{
				artifactDeployer.deploy(request, repo, artifactSet);
			}
			catch (ArtifactDeployerException e)
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
