package com.softmotions.qxmaven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.codehaus.plexus.PlexusTestCase;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Adamansky Anton (anton@adamansky.com)
 * @version $Id$
 */
public class MavenProjectStub extends org.apache.maven.plugin.testing.stubs.MavenProjectStub {
    private ArtifactStub artifact;

    {
        artifact = new ArtifactStub();
        artifact.setGroupId("org.qooxdoo");
        artifact.setArtifactId("qooxdoo-sdk");
        artifact.setVersion("3.0.1");
        artifact.setType("jar");
        artifact.setScope(DefaultArtifact.SCOPE_COMPILE);
        artifact.setFile(new File("src/test/resources/qooxdoo-sdk-3.0.1.jar"));
    }

    List<ArtifactRepository> remoteRepositories;


    public List<ArtifactRepository> getRemoteArtifactRepositories() {
        if (remoteRepositories == null) {
            File testRepo = new File(PlexusTestCase.getBasedir(), "src/test/remote-repo");
            ArtifactRepository repository =
                    new DefaultArtifactRepository("test-repo",
                                                  "file://" + testRepo.getAbsolutePath(),
                                                  new DefaultRepositoryLayout());
            remoteRepositories = Collections.singletonList(repository);
        }
        return remoteRepositories;
    }


    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.plugin.testing.stubs.MavenProjectStub#getArtifacts()
     */
    public Set<Artifact> getArtifacts() {
        Set<Artifact> artifacts = new HashSet();
        artifacts.add(artifact);
        return artifacts;
    }
}
