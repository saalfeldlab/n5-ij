package org.janelia.saalfeldlab.n5.metadata.canonical;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.compress.utils.IOUtils;

public class ImportedTranslations {
	
	final String translation;
	
	public ImportedTranslations() {

		InputStream stream = this.getClass().getResourceAsStream("/n5.jq");
		byte[] encoded = null;
		try {
			encoded = IOUtils.toByteArray(stream);
		} catch (IOException e) {
		}

		if( encoded == null )
			translation = "";
		else
			translation = new String(encoded, StandardCharsets.US_ASCII );	
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
