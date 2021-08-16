package org.janelia.saalfeldlab.n5.metadata;

import static org.junit.Assert.fail;

import java.io.IOException;

import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.metadata.NgffMultiScaleGroupAttributes.MultiscaleDataset;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class NgffTests {
	
	private N5FSReader n5;

	@Before
	  public void setUp() throws IOException {

		final String n5Root = "src/test/resources/ngff.n5";
		n5 = new N5FSReader(n5Root);
	  }

	  @Test
	  public void testNgffGroupAttributeParsing() {

		  final double eps = 1e-9;
		  try {
			NgffMultiScaleGroupAttributes[] multiscales = n5.getAttribute("ngff_grpAttributes", "multiscales", NgffMultiScaleGroupAttributes[].class );
			Assert.assertEquals("one set of multiscales", 1, multiscales.length);
			
			MultiscaleDataset[] datasets = multiscales[0].datasets;
			Assert.assertEquals("num levels", 6, datasets.length);

			double scale = 4;
			for (int i = 0; i < datasets.length; i++) {

				String pathName = String.format("s%d", i);
				Assert.assertEquals("path name " + i, pathName, datasets[i].path);
				Assert.assertEquals("scale " + i, scale, datasets[i].transform.scale[2], eps);

				scale *= 2;
			}

		} catch (IOException e) {
			fail("Ngff parsing failed");
			e.printStackTrace();
		}
	  }

}
