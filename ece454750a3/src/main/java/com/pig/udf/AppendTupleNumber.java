package com.pig.udf;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;

import java.io.IOException;
import java.util.Iterator;

/**
 * This class takes a bag and for each tuple in the bag, appends the tuple number.
 *
 * Example:
 * a_with_tuple_number = FOREACH a GENERATE AppendTupleNumber(mybag);
 *
 */

public class AppendTupleNumber extends EvalFunc<DataBag> {

	public DataBag exec(Tuple tuple) throws IOException {
		if (tuple == null || tuple.size() != 1) {
			throw new IOException("Illegal arguments: expecting bag, int");
		}

		if (tuple.get(0) instanceof DataBag) {
			DataBag bag = (DataBag) tuple.get(0);
			Iterator<Tuple> iter = bag.iterator();

			try {
				DataBag newBag = BagFactory.getInstance().newDefaultBag();
				int size = 0;

				while (iter.hasNext()) {
					Tuple tp = TupleFactory.getInstance().newTuple(2);
					tp.set(0, "gene_" + (size + 1));
					tp.set(1, Double.parseDouble(iter.next().get(0).toString()));
					newBag.add(tp);
					size++;
				}

				return newBag;
			}
			catch (Exception ee) {
				throw new RuntimeException("Error while creating a bag", ee);
			}

		} else {
			throw new IOException("Illegal arguments: expecting bag, int");
		}
	}
}
