package org.janelia.saalfeldlab.n5.ui;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;

import org.janelia.saalfeldlab.n5.ui.DatasetSelectorDialog.UriValidator;
import org.junit.Test;

public class TestUriValidation {

	@Test
	public void testUriValidator() throws ParseException {

		final UriValidator urival = new UriValidator();

		final String os = System.getProperty("os.name").toLowerCase();

		final String[] names = new String[] {"b", "c"};
		final Path p = Paths.get("a", names);

		if (os.contains("windows")) {
			// windows only

			assertThrows(ParseException.class, () -> {
				urival.stringToValue(p.toString() + "?d/e");
			});
			assertThrows(ParseException.class, () -> {
				urival.stringToValue(p.toString() + "?d/e#/f/g");
			});
		}
		else {
			// not windows

			// test some weird strings that can technically be interpreted as paths or uris
			assertNotNull(urival.stringToValue("."));
			assertNotNull(urival.stringToValue("/.."));
			assertNotNull(urival.stringToValue("\\\\"));
			assertNotNull(urival.stringToValue("::"));
			assertNotNull(urival.stringToValue("/a/\\//b").toString());
			assertNotNull(urival.stringToValue("://////").toString());
			assertNotNull(urival.stringToValue("..").toString());
		}

		assertNotNull(urival.stringToValue(p.toString()));
		assertNotNull(urival.stringToValue(p.toUri().toString()));
		assertNotNull(urival.stringToValue(p.toUri().toString() + "?d/e"));
		assertNotNull(urival.stringToValue(p.toUri().toString() + "?d/e#f/g"));

		// both windows and not
		assertNotNull(urival.stringToValue("/a/b/c"));
		assertNotNull(urival.stringToValue("/a/b/c?d/e"));
		assertNotNull(urival.stringToValue("/a/b/c?d/e#f/g"));

		assertNotNull(urival.stringToValue("file:///a/b/c"));
		assertNotNull(urival.stringToValue("file:///a/b/c?d/e"));
		assertNotNull(urival.stringToValue("file:///a/b/c?d/e#f/g"));

		assertNotNull(urival.stringToValue("s3:///a/b/c"));
		assertNotNull(urival.stringToValue("s3:///a/b/c?d/e"));
		assertNotNull(urival.stringToValue("s3:///a/b/c?d/e#f/g"));

		assertNotNull(urival.stringToValue("https://s3.us-east-1.amazonaws.com/a/b/c"));
		assertNotNull(urival.stringToValue("https://s3.us-east-1.amazonaws.com/a/b/c?d/e"));
		assertNotNull(urival.stringToValue("https://s3.us-east-1.amazonaws.com/a/b/c?d/e#f/g"));

		assertNotNull(urival.stringToValue("gs://storage.googleapis.com/a/b"));
		assertNotNull(urival.stringToValue("gs://storage.googleapis.com/a/b/c?d/e"));
		assertNotNull(urival.stringToValue("gs://storage.googleapis.com/a/b/c?d/e#f/g"));

		assertNotNull(urival.stringToValue("https://storage.googleapis.com/a/b"));
		assertNotNull(urival.stringToValue("https://storage.googleapis.com/a/b/c?d/e"));
		assertNotNull(urival.stringToValue("https://storage.googleapis.com/a/b/c?d/e#f/g"));

		assertNotNull(urival.stringToValue("zarr:/a/b/c"));
		assertNotNull(urival.stringToValue("zarr:/a/b/c?d/e"));
		assertNotNull(urival.stringToValue("zarr:/a/b/c?d/e#f/g"));

		assertNotNull(urival.stringToValue("zarr:file:///a/b/c"));
		assertNotNull(urival.stringToValue("zarr:file:///a/b/c?d/e"));
		assertNotNull(urival.stringToValue("zarr:file:///a/b/c?d/e#f/g"));

	}

}
