package org.janelia.saalfeldlab.n5.sandbox;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.systemsx.cisd.base.mdarray.MDArray;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Writer;

public class JHdf5Strings
{

	public static void main( String[] args )
	{
		string1d();
		string2d();

		System.out.println( "done" );
	}

	/* The function below produces strings.h5 such that:
	 *
	 > h5dump -d data strings.h5 
	   HDF5 "strings.h5" {
	   DATASET "data" {
	      DATATYPE  "/__DATA_TYPES__/String_VariableLength"
	      DATASPACE  SIMPLE { ( 32 ) / ( H5S_UNLIMITED ) }
	      DATA {
	      (0): "", "1", "11", "111", "1111", "", "1", "11", "111", "1111", "", "1",
	      (12): "11", "111", "1111", "", "1", "11", "111", "1111", "", "1", "11",
	      (23): "111", "1111", "", "1", "11", "111", "1111", "", "1"
	       }
	   }
	   }
	*/
	public static void string1d()
	{
		final IHDF5Writer writer = HDF5Factory.open( "src/test/resources/strings.h5" );
		final int N = 32;

		final String[] data = new String[ N ];
		for ( int i = 0; i < N; i++ )
			data[ i ] = Stream.generate( () -> "1" ).limit( i % 5 ).collect( Collectors.joining() );

		writer.string().writeArrayVL( "data1d", data );
		writer.close();
	}
	
	/* The function below produces strings.h5 such that:
	 * 
	 > h5dump -d data2d strings.h5 
	 HDF5 "strings.h5" {
	 DATASET "data2d" {
   		DATATYPE  H5T_STRING {
      		STRSIZE 5;
      		STRPAD H5T_STR_NULLPAD;
      		CSET H5T_CSET_ASCII;
      		CTYPE H5T_C_S1;
   		}
   		DATASPACE  SIMPLE { ( 4, 4 ) / ( H5S_UNLIMITED, H5S_UNLIMITED ) }
   		DATA {
   		(0,0): "(0,0)", "(0,1)", "(0,2)", "(0,3)",
   		(1,0): "(1,0)", "(1,1)", "(1,2)", "(1,3)",
   		(2,0): "(2,0)", "(2,1)", "(2,2)", "(2,3)",
   		(3,0): "(3,0)", "(3,1)", "(3,2)", "(3,3)"
   		}
	}
	}
	*
	*/
	public static void string2d()
	{
		final IHDF5Writer writer = HDF5Factory.open( "src/test/resources/strings.h5" );
		final int nx = 4;
		final int ny = 4;

		int k = 0;
		final String[] flatData = new String[ nx * ny ];
		for ( int i = 0; i < nx; i++ )
			for ( int j = 0; j < ny; j++ )
				flatData[ k++ ] = String.format( "(%d,%d)", i, j );

		final MDArray< String > data = new MDArray<>( flatData, new int[]{ nx , ny } );
		writer.string().writeMDArray( "data2d", data );

		writer.close();
	}
	

}
