package com.softmotions.qxmaven;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.repository.RepositorySystem;

import java.io.File;

/**
 * @author Adamansky Anton (anton@adamansky.com)
 * @version $Id$
 */
public class ModulesUnpackMojoTest extends AbstractMojoTestCase {

    public void testUnpack() throws Exception {
        /*File testPom = new File(getBasedir(), "src/test/resources/sdk-unpack.pom");
        SdkUnpackMojo mojo = (SdkUnpackMojo) lookupMojo("sdk-unpack", testPom);
        assertNotNull("Failed to configure the plugin", mojo);



        File sdkDirectory = mojo.getSdkDirectory();
        FileUtils.deleteDirectory(sdkDirectory);
        mojo.execute();

        // Check that qooxdoo-sdk has been created
        assertTrue(sdkDirectory.exists());
        // Check the qooxdoo-sdk content
        assertTrue(new File(sdkDirectory, "version.txt").exists());*/

    }
}