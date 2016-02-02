package com.pig.udf;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;

import java.io.IOException;
import java.util.Iterator;

public class DotProductOfTwoBags extends EvalFunc<Double>{
	public Double exec(Tuple input) throws IOException {
		if (input == null || input.size() != 2)
			return 0.0;

		DataBag bag_1, bag_2;

		try {
			final Object object_1 = input.get(0);
			if (object_1 instanceof DataBag) {
				bag_1 = (DataBag) object_1;
			} else {
				throw new IOException("First args is not Databag");
			}

			final Object object_2 = input.get(1);
			if (object_2 instanceof DataBag) {
				bag_2 = (DataBag) object_2;
			} else {
				throw new IOException("First args is not Databag");
			}

			if (bag_1.size() != bag_2.size())
				return 0.0;

			Iterator<Tuple> iter_1 = bag_1.iterator();
			Iterator<Tuple> iter_2 = bag_2.iterator();
			Double dot_product = 0.0;

			while(iter_1.hasNext() && iter_2.hasNext()) {
				Double value_1 = Double.parseDouble(iter_1.next().get(0).toString());
				Double value_2 = Double.parseDouble(iter_2.next().get(0).toString());
				dot_product += value_1 * value_2;
			}

			return dot_product;
		} catch (Exception e) {
			throw new IOException("Caught exception processing input row ", e);
		}
	}
}
