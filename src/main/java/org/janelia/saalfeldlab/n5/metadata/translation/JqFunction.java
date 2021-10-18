package org.janelia.saalfeldlab.n5.metadata.translation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.Scope;
import net.thisptr.jackson.jq.Versions;
import net.thisptr.jackson.jq.exception.JsonQueryException;

public class JqFunction<S,T> implements Function<S,T>{

	private final Scope scope;

	private final ObjectMapper objMapper;

	private final Gson gson;

	private final JsonQuery query;

	private Class<T> clazz;

	public JqFunction(final String translation, final Gson gson, Class<T> clazz) {
		this.gson = gson;
		this.clazz = clazz;

		scope = JqUtils.buildRootScope();
		objMapper = new ObjectMapper();

		JsonQuery qTmp = null;
		try {
			qTmp = JsonQuery.compile(JqUtils.resolveImports(translation), Versions.JQ_1_6);
		} catch (JsonQueryException e) {
			e.printStackTrace();
		}
		query = qTmp;
	}

	@SuppressWarnings("unchecked")
	public JqFunction(final String translation, final Gson gson, T t ) {
		this( translation, gson, (Class<T>) t.getClass());
	}

	@Override
	public T apply( S src )
	{
		if( query == null )
			return null;

		JsonNode jsonNode;
		try {
			jsonNode = objMapper.readTree(gson.toJson(src));

			final List<JsonNode> out = new ArrayList<>();
			query.apply(scope, jsonNode, out::add);

			final StringBuffer stringOutput = new StringBuffer();
			for (final JsonNode n : out)
				stringOutput.append(n.toString() + "\n");

			return gson.fromJson(stringOutput.toString(), clazz);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
