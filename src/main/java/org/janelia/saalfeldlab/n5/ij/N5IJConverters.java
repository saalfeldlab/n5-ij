/**
 * Copyright (c) 2018--2020, Saalfeld lab
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.janelia.saalfeldlab.n5.ij;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.type.numeric.integer.AbstractIntegerType;
import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.type.numeric.integer.GenericIntType;
import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.type.numeric.integer.GenericShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Util;

/**
 *
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 * @author John Bogovic &lt;bogovicj@janelia.hhmi.org&gt;
 *
 */
public interface N5IJConverters {

	public static final byte[][] PEARSON_HASH_LUT = new byte[][]{
		{(byte)134, (byte)127, (byte)255, (byte)24, (byte)77, (byte)249, (byte)79, (byte)109, (byte)104, (byte)112, (byte)216, (byte)241, (byte)208, (byte)21, (byte)63, (byte)97, (byte)123, (byte)201, (byte)196, (byte)236, (byte)251, (byte)18, (byte)117, (byte)146, (byte)232, (byte)197, (byte)46, (byte)135, (byte)74, (byte)248, (byte)214, (byte)110, (byte)37, (byte)156, (byte)159, (byte)22, (byte)181, (byte)94, (byte)30, (byte)19, (byte)163, (byte)115, (byte)51, (byte)225, (byte)96, (byte)7, (byte)243, (byte)6, (byte)86, (byte)91, (byte)229, (byte)231, (byte)17, (byte)185, (byte)68, (byte)148, (byte)158, (byte)131, (byte)89, (byte)16, (byte)92, (byte)27, (byte)62, (byte)233, (byte)59, (byte)3, (byte)235, (byte)183, (byte)188, (byte)41, (byte)102, (byte)141, (byte)138, (byte)240, (byte)217, (byte)195, (byte)230, (byte)118, (byte)99, (byte)31, (byte)237, (byte)164, (byte)45, (byte)254, (byte)69, (byte)140, (byte)221, (byte)121, (byte)142, (byte)93, (byte)175, (byte)124, (byte)67, (byte)88, (byte)144, (byte)204, (byte)180, (byte)168, (byte)64, (byte)76, (byte)213, (byte)132, (byte)85, (byte)5, (byte)194, (byte)28, (byte)75, (byte)36, (byte)122, (byte)239, (byte)191, (byte)107, (byte)116, (byte)38, (byte)206, (byte)149, (byte)244, (byte)228, (byte)199, (byte)128, (byte)81, (byte)177, (byte)184, (byte)2, (byte)222, (byte)80, (byte)53, (byte)136, (byte)26, (byte)167, (byte)95, (byte)162, (byte)119, (byte)187, (byte)218, (byte)147, (byte)103, (byte)234, (byte)98, (byte)48, (byte)160, (byte)190, (byte)42, (byte)87, (byte)8, (byte)126, (byte)198, (byte)14, (byte)35, (byte)203, (byte)252, (byte)113, (byte)210, (byte)145, (byte)13, (byte)170, (byte)182, (byte)157, (byte)247, (byte)34, (byte)137, (byte)178, (byte)151, (byte)111, (byte)174, (byte)10, (byte)1, (byte)11, (byte)169, (byte)227, (byte)154, (byte)205, (byte)20, (byte)161, (byte)82, (byte)143, (byte)12, (byte)186, (byte)71, (byte)245, (byte)58, (byte)133, (byte)101, (byte)171, (byte)238, (byte)207, (byte)73, (byte)125, (byte)120, (byte)165, (byte)219, (byte)78, (byte)4, (byte)90, (byte)55, (byte)83, (byte)106, (byte)49, (byte)176, (byte)129, (byte)150, (byte)32, (byte)202, (byte)223, (byte)43, (byte)179, (byte)23, (byte)47, (byte)65, (byte)172, (byte)130, (byte)189, (byte)39, (byte)155, (byte)152, (byte)84, (byte)15, (byte)25, (byte)200, (byte)54, (byte)193, (byte)72, (byte)40, (byte)192, (byte)0, (byte)52, (byte)250, (byte)246, (byte)215, (byte)220, (byte)173, (byte)100, (byte)57, (byte)226, (byte)9, (byte)153, (byte)60, (byte)253, (byte)44, (byte)242, (byte)105, (byte)209, (byte)50, (byte)33, (byte)139, (byte)108, (byte)56, (byte)224, (byte)211, (byte)114, (byte)70, (byte)29, (byte)212, (byte)66, (byte)61, (byte)166},
		{(byte)93, (byte)209, (byte)212, (byte)33, (byte)54, (byte)36, (byte)152, (byte)25, (byte)2, (byte)107, (byte)50, (byte)40, (byte)73, (byte)149, (byte)205, (byte)21, (byte)8, (byte)102, (byte)46, (byte)115, (byte)179, (byte)128, (byte)198, (byte)197, (byte)29, (byte)63, (byte)84, (byte)15, (byte)202, (byte)188, (byte)161, (byte)3, (byte)255, (byte)208, (byte)141, (byte)24, (byte)194, (byte)28, (byte)181, (byte)61, (byte)230, (byte)59, (byte)143, (byte)83, (byte)0, (byte)96, (byte)214, (byte)85, (byte)144, (byte)35, (byte)160, (byte)228, (byte)225, (byte)79, (byte)245, (byte)113, (byte)223, (byte)185, (byte)234, (byte)120, (byte)244, (byte)145, (byte)246, (byte)215, (byte)189, (byte)253, (byte)196, (byte)159, (byte)89, (byte)88, (byte)37, (byte)216, (byte)101, (byte)219, (byte)51, (byte)187, (byte)100, (byte)109, (byte)11, (byte)118, (byte)192, (byte)213, (byte)176, (byte)201, (byte)136, (byte)154, (byte)55, (byte)224, (byte)158, (byte)87, (byte)39, (byte)170, (byte)242, (byte)66, (byte)137, (byte)41, (byte)183, (byte)195, (byte)65, (byte)200, (byte)168, (byte)166, (byte)58, (byte)62, (byte)48, (byte)163, (byte)226, (byte)240, (byte)220, (byte)81, (byte)249, (byte)105, (byte)111, (byte)175, (byte)38, (byte)121, (byte)14, (byte)9, (byte)248, (byte)254, (byte)60, (byte)12, (byte)68, (byte)82, (byte)227, (byte)169, (byte)1, (byte)116, (byte)123, (byte)172, (byte)247, (byte)131, (byte)174, (byte)72, (byte)47, (byte)26, (byte)98, (byte)17, (byte)18, (byte)52, (byte)204, (byte)146, (byte)148, (byte)190, (byte)6, (byte)117, (byte)153, (byte)134, (byte)206, (byte)20, (byte)114, (byte)91, (byte)150, (byte)110, (byte)45, (byte)53, (byte)239, (byte)5, (byte)43, (byte)235, (byte)23, (byte)135, (byte)193, (byte)222, (byte)92, (byte)64, (byte)203, (byte)186, (byte)75, (byte)104, (byte)127, (byte)199, (byte)42, (byte)142, (byte)132, (byte)182, (byte)122, (byte)7, (byte)139, (byte)140, (byte)251, (byte)218, (byte)241, (byte)173, (byte)129, (byte)16, (byte)57, (byte)155, (byte)126, (byte)165, (byte)184, (byte)103, (byte)112, (byte)237, (byte)44, (byte)157, (byte)78, (byte)77, (byte)70, (byte)207, (byte)217, (byte)191, (byte)32, (byte)19, (byte)164, (byte)106, (byte)124, (byte)69, (byte)167, (byte)252, (byte)243, (byte)10, (byte)138, (byte)27, (byte)162, (byte)133, (byte)95, (byte)94, (byte)97, (byte)231, (byte)67, (byte)171, (byte)250, (byte)99, (byte)30, (byte)156, (byte)211, (byte)34, (byte)125, (byte)4, (byte)177, (byte)86, (byte)130, (byte)221, (byte)31, (byte)119, (byte)180, (byte)178, (byte)108, (byte)56, (byte)151, (byte)49, (byte)80, (byte)74, (byte)232, (byte)238, (byte)236, (byte)229, (byte)22, (byte)147, (byte)13, (byte)76, (byte)71, (byte)90, (byte)210, (byte)233},
		{(byte)48, (byte)139, (byte)143, (byte)141, (byte)23, (byte)15, (byte)239, (byte)12, (byte)197, (byte)238, (byte)255, (byte)226, (byte)93, (byte)228, (byte)77, (byte)19, (byte)95, (byte)253, (byte)79, (byte)216, (byte)87, (byte)3, (byte)169, (byte)109, (byte)242, (byte)40, (byte)10, (byte)249, (byte)162, (byte)85, (byte)191, (byte)42, (byte)83, (byte)112, (byte)119, (byte)73, (byte)86, (byte)51, (byte)224, (byte)234, (byte)129, (byte)67, (byte)89, (byte)153, (byte)57, (byte)196, (byte)163, (byte)84, (byte)211, (byte)98, (byte)154, (byte)213, (byte)53, (byte)195, (byte)148, (byte)55, (byte)124, (byte)70, (byte)47, (byte)208, (byte)2, (byte)20, (byte)230, (byte)26, (byte)54, (byte)59, (byte)201, (byte)161, (byte)99, (byte)215, (byte)72, (byte)30, (byte)34, (byte)24, (byte)76, (byte)82, (byte)18, (byte)128, (byte)221, (byte)245, (byte)157, (byte)32, (byte)229, (byte)142, (byte)60, (byte)186, (byte)240, (byte)127, (byte)36, (byte)58, (byte)134, (byte)138, (byte)38, (byte)74, (byte)144, (byte)116, (byte)171, (byte)88, (byte)217, (byte)159, (byte)243, (byte)16, (byte)200, (byte)81, (byte)185, (byte)194, (byte)94, (byte)192, (byte)207, (byte)45, (byte)206, (byte)105, (byte)244, (byte)180, (byte)210, (byte)50, (byte)212, (byte)56, (byte)41, (byte)68, (byte)118, (byte)173, (byte)121, (byte)92, (byte)111, (byte)250, (byte)209, (byte)22, (byte)252, (byte)182, (byte)248, (byte)35, (byte)198, (byte)158, (byte)123, (byte)27, (byte)31, (byte)33, (byte)172, (byte)146, (byte)132, (byte)11, (byte)62, (byte)49, (byte)220, (byte)155, (byte)0, (byte)167, (byte)168, (byte)7, (byte)219, (byte)25, (byte)69, (byte)115, (byte)247, (byte)106, (byte)75, (byte)131, (byte)117, (byte)166, (byte)110, (byte)188, (byte)232, (byte)46, (byte)164, (byte)156, (byte)204, (byte)181, (byte)71, (byte)170, (byte)177, (byte)6, (byte)28, (byte)137, (byte)37, (byte)122, (byte)44, (byte)187, (byte)66, (byte)203, (byte)126, (byte)135, (byte)65, (byte)90, (byte)214, (byte)199, (byte)225, (byte)29, (byte)152, (byte)184, (byte)223, (byte)101, (byte)21, (byte)189, (byte)61, (byte)136, (byte)140, (byte)176, (byte)64, (byte)133, (byte)13, (byte)246, (byte)160, (byte)4, (byte)202, (byte)125, (byte)97, (byte)165, (byte)227, (byte)108, (byte)80, (byte)14, (byte)175, (byte)103, (byte)96, (byte)151, (byte)5, (byte)241, (byte)235, (byte)43, (byte)222, (byte)179, (byte)254, (byte)233, (byte)8, (byte)145, (byte)147, (byte)149, (byte)107, (byte)150, (byte)104, (byte)52, (byte)178, (byte)190, (byte)218, (byte)100, (byte)113, (byte)130, (byte)251, (byte)183, (byte)231, (byte)102, (byte)236, (byte)114, (byte)237, (byte)193, (byte)1, (byte)17, (byte)120, (byte)78, (byte)205, (byte)174, (byte)39, (byte)91, (byte)9, (byte)63},
		{(byte)109, (byte)207, (byte)49, (byte)242, (byte)171, (byte)214, (byte)200, (byte)69, (byte)13, (byte)153, (byte)44, (byte)96, (byte)39, (byte)173, (byte)203, (byte)51, (byte)20, (byte)80, (byte)164, (byte)27, (byte)55, (byte)170, (byte)210, (byte)41, (byte)232, (byte)40, (byte)155, (byte)188, (byte)240, (byte)229, (byte)151, (byte)145, (byte)246, (byte)168, (byte)60, (byte)12, (byte)251, (byte)0, (byte)159, (byte)98, (byte)254, (byte)206, (byte)146, (byte)11, (byte)189, (byte)231, (byte)233, (byte)250, (byte)35, (byte)236, (byte)1, (byte)53, (byte)243, (byte)255, (byte)127, (byte)36, (byte)31, (byte)220, (byte)219, (byte)123, (byte)3, (byte)150, (byte)148, (byte)70, (byte)180, (byte)117, (byte)227, (byte)17, (byte)147, (byte)205, (byte)194, (byte)106, (byte)8, (byte)131, (byte)82, (byte)92, (byte)77, (byte)235, (byte)97, (byte)115, (byte)52, (byte)74, (byte)187, (byte)230, (byte)59, (byte)114, (byte)90, (byte)87, (byte)204, (byte)28, (byte)62, (byte)144, (byte)76, (byte)5, (byte)99, (byte)216, (byte)247, (byte)108, (byte)141, (byte)19, (byte)112, (byte)134, (byte)248, (byte)237, (byte)18, (byte)184, (byte)133, (byte)126, (byte)4, (byte)199, (byte)197, (byte)135, (byte)143, (byte)140, (byte)91, (byte)67, (byte)38, (byte)46, (byte)185, (byte)212, (byte)213, (byte)58, (byte)50, (byte)81, (byte)47, (byte)9, (byte)72, (byte)172, (byte)119, (byte)120, (byte)75, (byte)152, (byte)167, (byte)6, (byte)86, (byte)142, (byte)183, (byte)104, (byte)226, (byte)16, (byte)61, (byte)175, (byte)110, (byte)125, (byte)223, (byte)7, (byte)56, (byte)192, (byte)176, (byte)89, (byte)65, (byte)85, (byte)179, (byte)121, (byte)116, (byte)48, (byte)95, (byte)238, (byte)21, (byte)32, (byte)182, (byte)181, (byte)198, (byte)30, (byte)57, (byte)45, (byte)24, (byte)2, (byte)202, (byte)174, (byte)162, (byte)224, (byte)26, (byte)157, (byte)161, (byte)196, (byte)94, (byte)252, (byte)63, (byte)195, (byte)25, (byte)88, (byte)225, (byte)222, (byte)234, (byte)42, (byte)122, (byte)83, (byte)239, (byte)64, (byte)178, (byte)43, (byte)37, (byte)130, (byte)217, (byte)111, (byte)105, (byte)84, (byte)137, (byte)78, (byte)139, (byte)138, (byte)244, (byte)218, (byte)132, (byte)163, (byte)249, (byte)177, (byte)241, (byte)158, (byte)124, (byte)201, (byte)23, (byte)22, (byte)154, (byte)71, (byte)129, (byte)113, (byte)156, (byte)15, (byte)14, (byte)215, (byte)186, (byte)128, (byte)68, (byte)209, (byte)221, (byte)166, (byte)245, (byte)136, (byte)34, (byte)73, (byte)193, (byte)169, (byte)101, (byte)93, (byte)100, (byte)102, (byte)79, (byte)54, (byte)33, (byte)66, (byte)191, (byte)10, (byte)211, (byte)208, (byte)228, (byte)29, (byte)107, (byte)160, (byte)165, (byte)190, (byte)253, (byte)118, (byte)103, (byte)149},
		{(byte)145, (byte)182, (byte)137, (byte)41, (byte)98, (byte)54, (byte)107, (byte)71, (byte)16, (byte)19, (byte)58, (byte)21, (byte)180, (byte)162, (byte)174, (byte)178, (byte)146, (byte)183, (byte)1, (byte)88, (byte)103, (byte)84, (byte)231, (byte)235, (byte)208, (byte)66, (byte)130, (byte)95, (byte)136, (byte)116, (byte)249, (byte)245, (byte)152, (byte)127, (byte)139, (byte)193, (byte)242, (byte)169, (byte)59, (byte)108, (byte)218, (byte)33, (byte)252, (byte)246, (byte)202, (byte)93, (byte)144, (byte)3, (byte)27, (byte)81, (byte)69, (byte)228, (byte)229, (byte)154, (byte)18, (byte)226, (byte)179, (byte)39, (byte)65, (byte)2, (byte)74, (byte)238, (byte)212, (byte)15, (byte)204, (byte)191, (byte)101, (byte)171, (byte)213, (byte)140, (byte)44, (byte)80, (byte)86, (byte)32, (byte)250, (byte)29, (byte)153, (byte)155, (byte)240, (byte)78, (byte)87, (byte)70, (byte)255, (byte)36, (byte)205, (byte)151, (byte)197, (byte)230, (byte)37, (byte)166, (byte)147, (byte)60, (byte)251, (byte)220, (byte)124, (byte)181, (byte)30, (byte)188, (byte)34, (byte)237, (byte)51, (byte)149, (byte)236, (byte)138, (byte)55, (byte)160, (byte)115, (byte)43, (byte)216, (byte)99, (byte)168, (byte)198, (byte)186, (byte)215, (byte)96, (byte)50, (byte)133, (byte)142, (byte)207, (byte)118, (byte)77, (byte)25, (byte)111, (byte)45, (byte)42, (byte)209, (byte)234, (byte)201, (byte)61, (byte)83, (byte)9, (byte)217, (byte)75, (byte)248, (byte)156, (byte)62, (byte)102, (byte)31, (byte)94, (byte)134, (byte)170, (byte)161, (byte)109, (byte)72, (byte)219, (byte)110, (byte)211, (byte)206, (byte)125, (byte)57, (byte)120, (byte)8, (byte)11, (byte)247, (byte)97, (byte)13, (byte)14, (byte)49, (byte)119, (byte)105, (byte)35, (byte)5, (byte)143, (byte)164, (byte)68, (byte)159, (byte)63, (byte)46, (byte)241, (byte)82, (byte)56, (byte)232, (byte)122, (byte)85, (byte)150, (byte)172, (byte)48, (byte)10, (byte)26, (byte)24, (byte)227, (byte)17, (byte)165, (byte)221, (byte)223, (byte)173, (byte)22, (byte)12, (byte)184, (byte)73, (byte)129, (byte)194, (byte)243, (byte)28, (byte)210, (byte)253, (byte)135, (byte)128, (byte)131, (byte)64, (byte)195, (byte)106, (byte)200, (byte)192, (byte)189, (byte)196, (byte)112, (byte)224, (byte)214, (byte)104, (byte)157, (byte)187, (byte)89, (byte)121, (byte)244, (byte)190, (byte)92, (byte)67, (byte)203, (byte)167, (byte)4, (byte)163, (byte)47, (byte)114, (byte)76, (byte)225, (byte)91, (byte)7, (byte)239, (byte)20, (byte)53, (byte)38, (byte)0, (byte)199, (byte)132, (byte)100, (byte)175, (byte)158, (byte)90, (byte)79, (byte)177, (byte)117, (byte)123, (byte)40, (byte)126, (byte)6, (byte)233, (byte)52, (byte)113, (byte)185, (byte)254, (byte)148, (byte)222, (byte)23, (byte)176, (byte)141},
		{(byte)138, (byte)162, (byte)235, (byte)7, (byte)111, (byte)16, (byte)92, (byte)83, (byte)177, (byte)12, (byte)191, (byte)166, (byte)184, (byte)86, (byte)231, (byte)94, (byte)243, (byte)123, (byte)188, (byte)213, (byte)81, (byte)107, (byte)128, (byte)8, (byte)126, (byte)201, (byte)82, (byte)62, (byte)114, (byte)67, (byte)56, (byte)217, (byte)68, (byte)160, (byte)3, (byte)156, (byte)169, (byte)55, (byte)18, (byte)175, (byte)142, (byte)155, (byte)205, (byte)102, (byte)41, (byte)80, (byte)255, (byte)63, (byte)21, (byte)232, (byte)254, (byte)95, (byte)13, (byte)167, (byte)237, (byte)97, (byte)149, (byte)17, (byte)223, (byte)19, (byte)190, (byte)145, (byte)247, (byte)186, (byte)208, (byte)48, (byte)144, (byte)125, (byte)197, (byte)65, (byte)29, (byte)110, (byte)59, (byte)136, (byte)57, (byte)5, (byte)204, (byte)137, (byte)131, (byte)249, (byte)220, (byte)109, (byte)245, (byte)252, (byte)151, (byte)99, (byte)14, (byte)179, (byte)106, (byte)30, (byte)135, (byte)133, (byte)230, (byte)88, (byte)116, (byte)54, (byte)158, (byte)27, (byte)236, (byte)115, (byte)161, (byte)178, (byte)165, (byte)218, (byte)72, (byte)100, (byte)221, (byte)193, (byte)241, (byte)121, (byte)28, (byte)253, (byte)122, (byte)25, (byte)58, (byte)113, (byte)139, (byte)248, (byte)47, (byte)129, (byte)240, (byte)119, (byte)214, (byte)42, (byte)246, (byte)75, (byte)171, (byte)20, (byte)206, (byte)209, (byte)239, (byte)150, (byte)2, (byte)31, (byte)132, (byte)43, (byte)52, (byte)24, (byte)154, (byte)185, (byte)157, (byte)120, (byte)79, (byte)6, (byte)15, (byte)40, (byte)105, (byte)117, (byte)196, (byte)0, (byte)242, (byte)211, (byte)112, (byte)199, (byte)33, (byte)215, (byte)101, (byte)10, (byte)159, (byte)90, (byte)200, (byte)89, (byte)229, (byte)238, (byte)46, (byte)76, (byte)234, (byte)228, (byte)202, (byte)26, (byte)180, (byte)176, (byte)222, (byte)172, (byte)66, (byte)108, (byte)194, (byte)73, (byte)152, (byte)148, (byte)173, (byte)198, (byte)164, (byte)170, (byte)147, (byte)103, (byte)1, (byte)22, (byte)216, (byte)174, (byte)141, (byte)227, (byte)98, (byte)210, (byte)203, (byte)219, (byte)11, (byte)23, (byte)34, (byte)60, (byte)35, (byte)143, (byte)53, (byte)146, (byte)74, (byte)134, (byte)212, (byte)225, (byte)182, (byte)250, (byte)38, (byte)39, (byte)84, (byte)244, (byte)85, (byte)49, (byte)168, (byte)70, (byte)69, (byte)187, (byte)91, (byte)36, (byte)224, (byte)127, (byte)45, (byte)207, (byte)130, (byte)9, (byte)87, (byte)78, (byte)124, (byte)181, (byte)118, (byte)226, (byte)195, (byte)93, (byte)71, (byte)64, (byte)77, (byte)251, (byte)183, (byte)51, (byte)233, (byte)140, (byte)32, (byte)163, (byte)153, (byte)61, (byte)96, (byte)44, (byte)50, (byte)4, (byte)192, (byte)189, (byte)37, (byte)104},
		{(byte)9, (byte)203, (byte)236, (byte)188, (byte)103, (byte)168, (byte)36, (byte)247, (byte)87, (byte)16, (byte)148, (byte)82, (byte)244, (byte)86, (byte)74, (byte)14, (byte)119, (byte)105, (byte)32, (byte)22, (byte)197, (byte)137, (byte)224, (byte)11, (byte)116, (byte)46, (byte)164, (byte)187, (byte)95, (byte)40, (byte)91, (byte)207, (byte)166, (byte)60, (byte)252, (byte)44, (byte)150, (byte)147, (byte)178, (byte)138, (byte)102, (byte)232, (byte)43, (byte)10, (byte)185, (byte)30, (byte)226, (byte)214, (byte)57, (byte)186, (byte)199, (byte)229, (byte)90, (byte)96, (byte)71, (byte)66, (byte)218, (byte)130, (byte)143, (byte)225, (byte)24, (byte)124, (byte)212, (byte)114, (byte)112, (byte)26, (byte)19, (byte)62, (byte)80, (byte)53, (byte)146, (byte)118, (byte)171, (byte)145, (byte)239, (byte)2, (byte)89, (byte)173, (byte)6, (byte)3, (byte)192, (byte)169, (byte)49, (byte)122, (byte)240, (byte)107, (byte)193, (byte)174, (byte)0, (byte)108, (byte)183, (byte)237, (byte)1, (byte)220, (byte)201, (byte)120, (byte)159, (byte)133, (byte)234, (byte)221, (byte)198, (byte)97, (byte)243, (byte)123, (byte)170, (byte)35, (byte)79, (byte)219, (byte)223, (byte)139, (byte)106, (byte)4, (byte)235, (byte)69, (byte)184, (byte)64, (byte)61, (byte)231, (byte)160, (byte)155, (byte)213, (byte)153, (byte)113, (byte)144, (byte)230, (byte)151, (byte)222, (byte)29, (byte)76, (byte)18, (byte)94, (byte)99, (byte)189, (byte)23, (byte)194, (byte)165, (byte)177, (byte)45, (byte)28, (byte)132, (byte)200, (byte)180, (byte)162, (byte)246, (byte)172, (byte)55, (byte)13, (byte)190, (byte)104, (byte)51, (byte)204, (byte)157, (byte)142, (byte)128, (byte)227, (byte)233, (byte)210, (byte)248, (byte)93, (byte)37, (byte)245, (byte)56, (byte)254, (byte)191, (byte)72, (byte)127, (byte)75, (byte)110, (byte)38, (byte)42, (byte)15, (byte)54, (byte)67, (byte)216, (byte)77, (byte)121, (byte)58, (byte)31, (byte)21, (byte)217, (byte)109, (byte)101, (byte)131, (byte)84, (byte)50, (byte)156, (byte)208, (byte)8, (byte)152, (byte)73, (byte)175, (byte)176, (byte)7, (byte)17, (byte)59, (byte)88, (byte)167, (byte)39, (byte)196, (byte)205, (byte)41, (byte)181, (byte)5, (byte)206, (byte)228, (byte)242, (byte)126, (byte)34, (byte)249, (byte)251, (byte)134, (byte)100, (byte)33, (byte)238, (byte)63, (byte)68, (byte)70, (byte)149, (byte)125, (byte)158, (byte)161, (byte)85, (byte)115, (byte)135, (byte)195, (byte)154, (byte)129, (byte)27, (byte)163, (byte)81, (byte)78, (byte)250, (byte)209, (byte)211, (byte)215, (byte)141, (byte)52, (byte)140, (byte)98, (byte)117, (byte)255, (byte)179, (byte)92, (byte)47, (byte)48, (byte)20, (byte)182, (byte)202, (byte)12, (byte)241, (byte)253, (byte)83, (byte)111, (byte)136, (byte)25, (byte)65},
		{(byte)177, (byte)32, (byte)103, (byte)196, (byte)108, (byte)119, (byte)9, (byte)145, (byte)54, (byte)186, (byte)114, (byte)165, (byte)172, (byte)28, (byte)107, (byte)159, (byte)11, (byte)24, (byte)7, (byte)180, (byte)222, (byte)192, (byte)26, (byte)71, (byte)38, (byte)106, (byte)76, (byte)50, (byte)27, (byte)41, (byte)157, (byte)78, (byte)254, (byte)85, (byte)92, (byte)83, (byte)253, (byte)120, (byte)229, (byte)182, (byte)8, (byte)251, (byte)209, (byte)82, (byte)48, (byte)18, (byte)110, (byte)131, (byte)220, (byte)240, (byte)155, (byte)214, (byte)227, (byte)144, (byte)60, (byte)189, (byte)154, (byte)206, (byte)3, (byte)31, (byte)65, (byte)238, (byte)221, (byte)231, (byte)239, (byte)243, (byte)104, (byte)150, (byte)77, (byte)111, (byte)200, (byte)121, (byte)164, (byte)128, (byte)100, (byte)63, (byte)174, (byte)49, (byte)123, (byte)135, (byte)175, (byte)29, (byte)244, (byte)140, (byte)168, (byte)228, (byte)73, (byte)102, (byte)132, (byte)43, (byte)230, (byte)2, (byte)25, (byte)179, (byte)53, (byte)46, (byte)148, (byte)87, (byte)88, (byte)134, (byte)170, (byte)115, (byte)5, (byte)153, (byte)105, (byte)197, (byte)152, (byte)193, (byte)19, (byte)249, (byte)117, (byte)84, (byte)116, (byte)113, (byte)42, (byte)40, (byte)81, (byte)79, (byte)98, (byte)226, (byte)195, (byte)156, (byte)12, (byte)122, (byte)58, (byte)236, (byte)201, (byte)213, (byte)171, (byte)237, (byte)94, (byte)139, (byte)162, (byte)52, (byte)147, (byte)199, (byte)142, (byte)217, (byte)36, (byte)14, (byte)74, (byte)45, (byte)95, (byte)188, (byte)35, (byte)44, (byte)21, (byte)47, (byte)187, (byte)20, (byte)218, (byte)141, (byte)57, (byte)67, (byte)66, (byte)247, (byte)224, (byte)223, (byte)161, (byte)246, (byte)235, (byte)203, (byte)68, (byte)255, (byte)112, (byte)169, (byte)215, (byte)232, (byte)34, (byte)143, (byte)173, (byte)133, (byte)80, (byte)72, (byte)151, (byte)225, (byte)10, (byte)59, (byte)4, (byte)158, (byte)248, (byte)51, (byte)62, (byte)56, (byte)146, (byte)219, (byte)138, (byte)194, (byte)6, (byte)149, (byte)208, (byte)167, (byte)250, (byte)70, (byte)234, (byte)109, (byte)166, (byte)125, (byte)93, (byte)55, (byte)17, (byte)89, (byte)75, (byte)101, (byte)64, (byte)184, (byte)136, (byte)205, (byte)30, (byte)211, (byte)22, (byte)212, (byte)160, (byte)37, (byte)245, (byte)97, (byte)124, (byte)90, (byte)118, (byte)69, (byte)202, (byte)185, (byte)39, (byte)241, (byte)181, (byte)16, (byte)33, (byte)204, (byte)99, (byte)191, (byte)126, (byte)137, (byte)130, (byte)216, (byte)127, (byte)13, (byte)190, (byte)210, (byte)61, (byte)129, (byte)96, (byte)252, (byte)242, (byte)176, (byte)1, (byte)163, (byte)15, (byte)198, (byte)23, (byte)183, (byte)91, (byte)0, (byte)86, (byte)233, (byte)207, (byte)178}
	};

	public static byte byteHash(final long a, final byte[] lut) {

		int b = lut[(int)a & 0xff] & 0xff;
		b = lut[((int)(a >> 8) & 0xff) ^ b] & 0xff;
		b = lut[((int)(a >> 16) & 0xff) ^ b] & 0xff;
		b = lut[((int)(a >> 24) & 0xff) ^ b] & 0xff;
		b = lut[((int)(a >> 32) & 0xff) ^ b] & 0xff;
		b = lut[((int)(a >> 40) & 0xff) ^ b] & 0xff;
		b = lut[((int)(a >> 48) & 0xff) ^ b] & 0xff;
		b = lut[((int)(a >> 56) & 0xff) ^ b] & 0xff;

		return (byte)b;
	}

	public static byte byteHash(final int a, final byte[] lut) {

		int b = lut[a & 0xff] & 0xff;
		b = lut[((a >> 8) & 0xff) ^ b] & 0xff;
		b = lut[((a >> 16) & 0xff) ^ b] & 0xff;
		b = lut[((a >> 24) & 0xff) ^ b] & 0xff;

		return (byte)b;
	}

	public static byte byteHash(final short a, final byte[] lut) {

		int b = lut[a & 0xff] & 0xff;
		b = lut[((a >> 8) & 0xff) ^ b] & 0xff;

		return (byte)b;
	}

	public static byte byteHash(final byte a, final byte[] lut) {

		return lut[a & 0xff];
	}

	public static short byteHash(final long a) {

		return byteHash(a, PEARSON_HASH_LUT[0]);
	}

	public static short byteHash(final int a) {

		return byteHash(a, PEARSON_HASH_LUT[0]);
	}

	public static short byteHash(final short a) {

		return byteHash(a, PEARSON_HASH_LUT[0]);
	}

	public static short byteHash(final byte a) {

		return byteHash(a, PEARSON_HASH_LUT[0]);
	}


	public static short shortHash(final long a) {

		int b = (byteHash(a, PEARSON_HASH_LUT[0]) & 0xff);
		b |= (byteHash(a, PEARSON_HASH_LUT[1]) & 0xff) << 8;
		return (short)b;
	}

	public static short shortHash(final int a) {

		int b = (byteHash(a, PEARSON_HASH_LUT[0]) & 0xff);
		b |= (byteHash(a, PEARSON_HASH_LUT[1]) & 0xff) << 8;
		return (short)b;
	}

	public static short shortHash(final short a) {

		int b = (byteHash(a, PEARSON_HASH_LUT[0]) & 0xff);
		b |= (byteHash(a, PEARSON_HASH_LUT[1]) & 0xff) << 8;
		return (short)b;
	}

	public static short shortHash(final byte a) {

		int b = (byteHash(a, PEARSON_HASH_LUT[0]) & 0xff);
		b |= (byteHash(a, PEARSON_HASH_LUT[1]) & 0xff) << 8;
		return (short)b;
	}


	public static int rgbHash(final long a) {

		int b = (byteHash(a, PEARSON_HASH_LUT[0]) & 0xff);
		b |= (byteHash(a, PEARSON_HASH_LUT[1]) & 0xff) << 8;
		b |= (byteHash(a, PEARSON_HASH_LUT[2]) & 0xff) << 16;
		return b;
	}

	public static int rgbHash(final int a) {

		int b = (byteHash(a, PEARSON_HASH_LUT[0]) & 0xff);
		b |= (byteHash(a, PEARSON_HASH_LUT[1]) & 0xff) << 8;
		b |= (byteHash(a, PEARSON_HASH_LUT[2]) & 0xff) << 16;
		return b;
	}

	public static int rgbHash(final short a) {

		int b = (byteHash(a, PEARSON_HASH_LUT[0]) & 0xff);
		b |= (byteHash(a, PEARSON_HASH_LUT[1]) & 0xff) << 8;
		b |= (byteHash(a, PEARSON_HASH_LUT[2]) & 0xff) << 16;
		return b;
	}

	public static int rgbHash(final byte a) {

		int b = (byteHash(a, PEARSON_HASH_LUT[0]) & 0xff);
		b |= (byteHash(a, PEARSON_HASH_LUT[1]) & 0xff) << 8;
		b |= (byteHash(a, PEARSON_HASH_LUT[2]) & 0xff) << 16;
		return b;
	}


	public static <T extends AbstractIntegerType<T>> RandomAccessibleInterval<UnsignedByteType> uint8Hash(
			final RandomAccessibleInterval<T> src) {

		final T t = Util.getTypeFromInterval(src);
		if (t instanceof GenericLongType)
			return Converters.convert(
					(RandomAccessibleInterval<GenericLongType<?>>)(src),
					(a, b) -> b.set(byteHash(a.getLong())),
					new UnsignedByteType());
		else if (t instanceof GenericIntType)
			return Converters.convert(
					(RandomAccessibleInterval<GenericIntType<?>>)(src),
					(a, b) -> b.set(byteHash(a.getInt())),
					new UnsignedByteType());
		else if (t instanceof GenericShortType)
			return Converters.convert(
					(RandomAccessibleInterval<GenericShortType<?>>)(src),
					(a, b) -> b.set(byteHash(a.getShort())),
					new UnsignedByteType());
		else if (t instanceof GenericByteType)
			return Converters.convert(
					(RandomAccessibleInterval<GenericByteType<?>>)(src),
					(a, b) -> b.set(byteHash(a.getByte())),
					new UnsignedByteType());
		else
			return null;
	}
}
