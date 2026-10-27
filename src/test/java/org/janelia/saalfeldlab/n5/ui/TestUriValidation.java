package org.janelia.saalfeldlab.n5.ui;

import static org.junit.Assert.assertEquals;
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

		// both windows and not: p is "a\b\c" on windows, "a/b/c" otherwise
		assertIsPathGetQuery("relative path, query", p.toString(), "?d/e", urival);
		assertIsPathGetQuery("relative path, query, fragment", p.toString(), "?d/e#/f/g", urival);

		if (os.contains("windows")) {
			// windows only
			assertIsPathGet("drive letter", "C:\\a\\b", urival);
			// what URI.getPath() returns for a file URI on windows
			assertEquals("slash, drive letter", Paths.get("C:\\a\\b").toUri().normalize(), urival.stringToValue("/C:/a/b"));

		} else {
			// not windows
			assertIsPathGet("relative path", p.normalize().toString(), urival);

			// test some weird strings that can technically be interpreted as
			// paths or uris
			// NB: Unix only. On windows neither is a valid path, so Paths.get
			// throws InvalidPathException, both here and in the validator: "\\"
			// is the start of a UNC path with no server and share, and ":" is
			// not allowed in a windows file name (only after a drive letter).
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

		assertIsPathGetType("zarr: path", "zarr://", "/a/b/c", urival);
		assertIsUriCreateType("zarr:file: path", "zarr://", "file:///a/b/c", urival);
		assertIsUriCreateType("zarr:file: path, query", "zarr://", "file:///a/b/c?d/e", urival);
		assertIsUriCreateType("zarr:file: path, query fragment", "zarr://", "file:///a/b/c?d/e#f/g", urival);
	}

	private void assertIsUriCreate(String message, String s, UriValidator urival) throws ParseException {

		assertEquals(message, URI.create(s).normalize(), urival.stringToValue(s));
	}

	private void assertIsUriCreateType(String message, String typeScheme, String s, UriValidator urival) throws ParseException {

		final URI val = (URI)urival.stringToValue(typeScheme + s);
		assertTrue(message + " starts with typescheme", val.toString().startsWith(typeScheme));
		final URI uriNoType = URI.create(val.toString().replaceFirst("^" + typeScheme + "\\/*", ""));
		assertEquals(message, URI.create(s).normalize(), uriNoType);
	}

	private void assertIsPathGet(String message, String s, UriValidator urival) throws ParseException {

		assertEquals(message, Paths.get(s).toUri().normalize(), urival.stringToValue(s));
	}

	private void assertIsPathGetQuery(String message, String path, String queryAndFragment, UriValidator urival) throws ParseException {

		// NB: Concatenation is fine: Paths.get(…).toUri() is encoded, and the
		// query and fragment used here need no encoding.
		final URI expected = URI.create(Paths.get(path).toUri().normalize() + queryAndFragment);
		assertEquals(message, expected, urival.stringToValue(path + queryAndFragment));
	}

	private void assertIsPathGetType(String message, String typeScheme, String s, UriValidator urival) throws ParseException {

		final URI val = (URI)urival.stringToValue(typeScheme + s);
		assertTrue(message + " starts with typescheme", val.toString().startsWith(typeScheme));
		final URI uriNoType = URI.create(val.toString().replaceFirst("^" + typeScheme + "\\/*", ""));
		assertEquals(message, Paths.get(s).toUri().normalize(), uriNoType);
	}

}
