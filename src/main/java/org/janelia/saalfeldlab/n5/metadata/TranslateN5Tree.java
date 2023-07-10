package org.janelia.saalfeldlab.n5.metadata;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.universe.N5Factory;
import org.janelia.saalfeldlab.n5.universe.translation.JqUtils;
import org.janelia.saalfeldlab.n5.universe.translation.TranslatedN5Reader;

import com.google.gson.Gson;

import picocli.CommandLine;
import picocli.CommandLine.Option;

public class TranslateN5Tree implements Callable<Void> {
	
	// Consider adding options for outputing only translation, only original, etc.

	@Option( names = { "-i", "--input" }, required = true, description = "N5 container." )
	private String n5ContainerPath;

	@Option( names = { "-t", "--translation" }, required = false, description = "Translation." )
	private String translationIn;

	@Option( names = { "-f", "--translation-file" }, required = false, description = "Translation file." )
	private String translationFilePath;

	@Override
	public Void call() throws Exception {

		N5Reader n5Base = new N5Factory().openReader(n5ContainerPath);

		String translation = null;
		if ( translationIn != null ) {
			translation = translationIn;
		}
		else if( translationFilePath != null ) {
			translation = new String( 
					Files.readAllBytes(Paths.get( translationFilePath )),
					StandardCharsets.US_ASCII );
		}

		final Gson gson = JqUtils.buildGson(n5Base);
		if( translation == null ) {
			final TranslatedN5Reader n5 = new TranslatedN5Reader(n5Base, gson, ".", ".");
			System.out.println(gson.toJson(n5.getTranslation().getOrig()));
			n5.close();
		}
		else {
			final TranslatedN5Reader n5 = new TranslatedN5Reader(n5Base, gson, translation, ".");
			System.out.println(gson.toJson(n5.getTranslation().getTranslated()));
			n5.close();
		}

		return null;
	}

	public static void main(String[] args) {
		new CommandLine(new TranslateN5Tree()).execute(args);
	}

}
