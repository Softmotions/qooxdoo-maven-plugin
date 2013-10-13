package com.softmotions.qxmaven;

import org.apache.maven.artifact.versioning.VersionRange;

/**
 * @author Adamansky Anton (anton@adamansky.com)
 * @version $Id$
 */
public class ArtifactStub extends org.apache.maven.plugin.testing.stubs.ArtifactStub {

    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.plugin.testing.stubs.ArtifactStub#getVersionRange()
     */
    public VersionRange getVersionRange() {
        return VersionRange.createFromVersion(getVersion());
    }
}