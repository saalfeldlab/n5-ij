package org.janelia.saalfeldlab.n5.metadata.canonical;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class ImportedTranslations {
	
	final File jqFile;
	
	final String translation;
	
	public ImportedTranslations() {
		jqFile = new File("src/main/resources/n5.jq");	
		byte[] encoded = null;
		try {
		  encoded = Files.readAllBytes( jqFile.toPath() );
		} catch (IOException e) {
		}

		if( encoded == null )
			translation = "";
		else
			translation = new String(encoded, StandardCharsets.US_ASCII );	

		System.out.println( translation );
	}
	
	public String getTranslation()
	{
		return translation;
	}
	
	public static void main( String[] args )
	{
		new ImportedTranslations();
	}
}
