/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.janelia.saalfeldlab.n5.dataaccess.s3;

import org.janelia.saalfeldlab.n5.dataaccess.DataAccessException;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

public class AmazonS3ClientBuilderWithDefaultRegion
{
	private static final String credentialsDocsLink = "https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html#cli-quick-configuration";

	public static AmazonS3 create() throws DataAccessException
	{
		try
		{
			return AmazonS3ClientBuilder.standard().withRegion( Regions.US_EAST_1 ).build();
		}
		catch(Exception e)
		{
			throw new DataAccessException(
					"<html>Could not find AWS region. Please initialize them using AWS Command Line Interface:<br/>"
							+ "<a href=\"" + credentialsDocsLink + "\">" + credentialsDocsLink + "</a></html>"
					);
		}
	}

}
