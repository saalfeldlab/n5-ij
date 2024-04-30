package org.janelia.saalfeldlab.n5.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

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

		final String[] names = new String[]{"b", "c"};
		final Path p = Paths.get("a", names);

		if (os.contains("windows")) {
			// windows only

			System.err.println("#########");
			System.err.println( p.toString());
			System.err.println( urival.stringToValue(p.toString() + "?d/e").toString());
			System.err.println( urival.stringToValue(p.toString() + "?d/e#f/g").toString());
			System.err.println("#########");

			assertThrows(ParseException.class, () -> {
				urival.stringToValue(p.toString() + "?d/e");
			});
			assertThrows(ParseException.class, () -> {
				urival.stringToValue(p.toString() + "?d/e#/f/g");
			});
		} else {
			// not windows
			assertIsPathGet("relative path", p.normalize().toString(), urival);

			// test some weird strings that can technically be interpreted as
			// paths or uris
			assertIsPathGet("weird", "\\\\", urival);
			assertIsPathGet("weird 2", "::", urival);
		}

		/**
		 * Test getting a path from the filesystem pth = Paths.get(tmpfile)
		 * assert that's the same as pth.toString + ? dataset is parsable as an
		 * n5uri
		 */

		// both windows and not
		assertIsPathGet("cwd", ".", urival);
		assertIsPathGet("parent dir", "..", urival);
		assertIsPathGet("absolute path", p.toAbsolutePath().normalize().toString(), urival);
		assertIsPathGet("relative path", p.normalize().toString(), urival);

		assertIsUriCreate("path", "file:///a/b/c", urival);
		assertIsUriCreate("path", "file:///a/b/c?d/e", urival);
		assertIsUriCreate("path", "file:///a/b/c?d/e#f/g", urival);

		assertIsUriCreate("s3 path", "s3:///a/b/c", urival);
		assertIsUriCreate("s3 path, query", "s3:///a/b/c?d/e", urival);
		assertIsUriCreate("s3 path, query, frament", "s3:///a/b/c?d/e#f/g", urival);

		assertIsUriCreate("https s3 path", "https://s3.us-east-1.amazonaws.com/a/b/c", urival);
		assertIsUriCreate("https s3 path, query", "https://s3.us-east-1.amazonaws.com/a/b/c?d/e", urival);
		assertIsUriCreate("https s3 path, query, frament", "https://s3.us-east-1.amazonaws.com/a/b/c?d/e#f/g", urival);

		assertIsUriCreate("gcs path", "gs://storage.googleapis.com/a/b/c", urival);
		assertIsUriCreate("gcs path, query", "gs://storage.googleapis.com/a/b/c?d/e", urival);
		assertIsUriCreate("gcs path, query, frament", "gs://storage.googleapis.com/a/b/c?d/e#f/g", urival);

		assertIsUriCreate("https gcs path", "https://storage.googleapis.com/a/b/c", urival);
		assertIsUriCreate("https gcs path, query", "https://storage.googleapis.com/a/b/c?d/e", urival);
		assertIsUriCreate("https gcs path, query, frament", "https://storage.googleapis.com/a/b/c?d/e#f/g", urival);

		assertIsPathGetType("zarr: path", "zarr:", "/a/b/c", urival);
		assertIsUriCreateType("zarr:file: path", "zarr:", "file:///a/b/c", urival);
		assertIsUriCreateType("zarr:file: path, query", "zarr:", "file:///a/b/c?d/e", urival);
		assertIsUriCreateType("zarr:file: path, query fragment", "zarr:", "file:///a/b/c?d/e#f/g", urival);
	}

	private void assertIsUriCreate(String message, String s, UriValidator urival) throws ParseException {

		assertEquals(message, URI.create(s).normalize(), urival.stringToValue(s));
	}

	private void assertIsUriCreateType(String message, String typeScheme, String s, UriValidator urival) throws ParseException {

		final URI val = (URI)urival.stringToValue(typeScheme + s);
		assertTrue(message + " starts with typescheme", val.toString().startsWith(typeScheme));
		final URI uriNoType = URI.create(val.toString().replaceFirst("^" + typeScheme, ""));
		assertEquals(message, URI.create(s).normalize(), uriNoType);
	}

	private void assertIsPathGet(String message, String s, UriValidator urival) throws ParseException {

		assertEquals(message, Paths.get(s).toUri().normalize(), urival.stringToValue(s));
	}

	private void assertIsPathGetType(String message, String typeScheme, String s, UriValidator urival) throws ParseException {

		final URI val = (URI)urival.stringToValue(typeScheme + s);
		assertTrue(message + " starts with typescheme", val.toString().startsWith(typeScheme));
		final URI uriNoType = URI.create(val.toString().replaceFirst("^" + typeScheme, ""));
		assertEquals(message, Paths.get(s).toUri().normalize(), uriNoType);
	}

}
