package org.janelia.saalfeldlab.n5.metadata;

import org.janelia.saalfeldlab.n5.DataType;

/**
 * Interface for metadata that describes 
 * 
 * @author Caleb Hulbert
 * @author John Bogovic
 */
public interface IntensityMetadata extends N5DatasetMetadata {

  static double maxForDataType(DataType dataType) {

	switch (dataType) {
	case UINT8:
	  return 0xff;
	case UINT16:
	  return 0xffff;
	case UINT32:
	  return 0xffffffffL;
	case UINT64:
	  return 2.0 * Long.MAX_VALUE;
	case INT8:
	  return Byte.MAX_VALUE;
	case INT16:
	  return Short.MAX_VALUE;
	case INT32:
	  return Integer.MAX_VALUE;
	case INT64:
	  return Long.MAX_VALUE;
	case FLOAT32:
	case FLOAT64:
	case OBJECT:
	default:
	  return 1.0;
	}
  }

  /**
   * @return the minimum intensity value of the data
   */
  default double minIntensity() {

	return 0.0;
  }

  /**
   * @return the maximum intensity value of the data
   */
  default double maxIntensity() {

	return maxForDataType(getAttributes().getDataType());
  }
}
