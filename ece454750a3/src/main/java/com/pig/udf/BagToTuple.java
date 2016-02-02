package com.pig.udf;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;

import java.io.IOException;
import java.util.*;

public class BagToTuple extends EvalFunc<Tuple> {
	public Tuple exec(Tuple input) throws IOException {
		try {
			List<Object> objects = new ArrayList<Object>();

			for (int i = 0; i < input.size(); i++) {
				final Object object = input.get(i);
				if (object instanceof DataBag) {
					DataBag bag = (DataBag) object;
					Iterator<Tuple> iter = bag.iterator();

					while (iter.hasNext()) {
						Object curr = iter.next();

						if(curr instanceof Tuple) {
							for(int j = 0; j < ((Tuple) curr).size(); j++) {
								objects.addAll(((Tuple) curr).getAll());
							}
						} else {
							objects.add(curr);
						}
					}

				} else {
					throw new IOException("Illegal arguments: expecting bag");
				}
			}

			Tuple tuple = TupleFactory.getInstance().newTuple(objects);

			return tuple;
		} catch (Exception ee) {
			throw new RuntimeException("Error while creating a tuple", ee);
		}
	}
}
