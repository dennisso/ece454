package com.pig.udf;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.Tuple;

import java.io.IOException;

public class SampleToInt extends EvalFunc<Integer> {
	public Integer exec(Tuple input) throws IOException {
		if (input == null || input.size() == 0)
			return null;

		try {
			String str = input.get(0).toString();
			return Integer.parseInt(str.substring(7));
		} catch (Exception e) {
			throw new IOException("Caught exception processing input row ", e);
		}
	}
}
