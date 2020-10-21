/**
 * Copyright (c) 2018--2020, Saalfeld lab
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.saalfeldlab.n5.dataaccess.googlecloud;

import java.io.IOException;

import org.janelia.saalfeldlab.googlecloud.GoogleCloudResourceManagerClient;
import org.janelia.saalfeldlab.googlecloud.GoogleCloudStorageClient;
import org.janelia.saalfeldlab.n5.dataaccess.DataAccessException;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.resourcemanager.ResourceManager;
import com.google.cloud.storage.Storage;

import ij.IJ;

/**
 * Use {@link org.janelia.saalfeldlab.googlecloud.GoogleCloudStorageClient} instead.
 */
@Deprecated
public class GoogleCloudClientBuilderWithDefaultCredentials
{
    private static final String googleCloudSdkLink = "https://cloud.google.com/sdk/docs";

    private static final String googleCloudAuthCmd = "gcloud auth application-default login";

    public static Storage createStorage() throws DataAccessException
    {
        return createStorage( null );
    }

    public static Storage createStorage( final String projectId ) throws DataAccessException
    {
        try
        {
            if ( !verifyCredentials() )
                throw new Exception();

            return new GoogleCloudStorageClient( projectId ).create();
        }
        catch ( final Exception e )
        {
            showErrorPrompt();
            throw new DataAccessException("");
        }
    }

    public static ResourceManager createResourceManager() throws DataAccessException
    {
        try
        {
            if ( !verifyCredentials() )
                throw new Exception();

            return new GoogleCloudResourceManagerClient().create();
        }
        catch ( final Exception e )
        {
            showErrorPrompt();
            throw new DataAccessException("");
        }
    }

    private static boolean verifyCredentials() throws IOException
    {
        return GoogleCredentials.getApplicationDefault() != null;
    }

    private static void showErrorPrompt()
    {
        IJ.error(
                "N5 Viewer",
                "<html>Could not find Google Cloud credentials. Please install "
                        + "<a href=\"" + googleCloudSdkLink + "\">Google Cloud SDK</a><br/>"
                        + "and then run this command to initialize the credentials:<br/><br/>"
                        + "<pre>" + googleCloudAuthCmd + "</pre></html>"
        );
    }
}
