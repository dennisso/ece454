/**
 * ECE 454/750: Distributed Computing
 *
 * Instead of using an ArrayList within an ArrayList, we used integer matrix.
 *
 */

package ece454750s15a2;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TriangleCountImpl {
	private byte[] input;
	private int numCores;

	private static int currentVertex = 0;
	private static int[][] adjacencyList;

	public TriangleCountImpl(byte[] input, int numCores) {
		this.input = input;
		this.numCores = numCores;
	}

	/**
	 * Get group members.
	 * @return List of group members.
	 */
	public List<String> getGroupMembers() {
		ArrayList<String> ret = new ArrayList<String>();
		ret.add("j7jeong");
		ret.add("bjleskow");
		ret.add("lkso");
		return ret;
	}

	/**
	 * Get current vertex and increment.
	 * @return current vertex.
	 */
	public static synchronized int getCurrentVertex() {
		return currentVertex++;
	}

	public static class VertexRunnable implements Runnable {
		private List<Triangle> list;
		private int currentVertex;

		public VertexRunnable(List<Triangle> list){
			this.list = list;
		}

		public void run() {
			while (this.currentVertex < adjacencyList.length) {
				this.currentVertex = getCurrentVertex();
				processVertex(adjacencyList, list, this.currentVertex);
			}
		}
	}

	public List<Triangle> enumerateTriangles() throws IOException {

		adjacencyList = getAdjacencyList(input);

		List<Triangle> ret;

		if (numCores == 1) {
			ret = new ArrayList<Triangle>();
			for (int currentVertex = 0; currentVertex < adjacencyList.length; currentVertex++) {
				processVertex(adjacencyList, ret, currentVertex);
			}
		} else {
			// List is thread-safe.
			ret = Collections.synchronizedList(new ArrayList<Triangle>());

			// Start pool of threads.
			ExecutorService pool = Executors.newFixedThreadPool(numCores);
			for (int i=0; i<numCores; i++) {
				pool.submit(new VertexRunnable(ret));
			}

			// Shutdown the pool.
			pool.shutdown();

			try {
				pool.awaitTermination(1, TimeUnit.DAYS);
			} catch (InterruptedException e) {
				System.exit(1);
			}
		}
		
		return ret;
	}

	/**
	 * Process each vertex and put inside the list.
	 * @param adjacencyList integer matrix.
	 * @param ret The list of insertion.
	 * @param nodeA Vertex of inspection.
	 */
	private static void processVertex(int[][] adjacencyList, List<Triangle> ret, int nodeA) {
		HashSet<Integer> collision = new HashSet<Integer>();

		int[] nodeAList = adjacencyList[nodeA];
		if (nodeAList.length < 2) return;

		for (int nodeB : nodeAList) {
			collision.add(nodeB);
		}

		for (int nodeB : nodeAList) {
			if (nodeA >= nodeB) continue;

			int[] nodeBList = adjacencyList[nodeB];
			if (nodeBList.length < 2) continue;

			for (int nodeC : nodeBList) {
				if (nodeB >= nodeC) continue;
				if (collision.contains(nodeC)) {
					ret.add(new Triangle(nodeA, nodeB, nodeC));
				}
			}
		}
	}

	public int[][] getAdjacencyList(byte[] data) throws IOException {
		InputStream istream = new ByteArrayInputStream(data);
		BufferedReader br = new BufferedReader(new InputStreamReader(istream));
		String strLine = br.readLine();
		if (!strLine.contains("vertices") || !strLine.contains("edges")) {
			System.err.println("Invalid graph file format. Offending line: " + strLine);
			System.exit(-1);
		}
		String parts[] = strLine.split(", ");
		int numVertices = Integer.parseInt(parts[0].split(" ")[0]);
		int numEdges = Integer.parseInt(parts[1].split(" ")[0]);
		System.out.println("Found graph with " + numVertices + " vertices and " + numEdges + " edges");

		int[][] adjacencyList = new int[numVertices][];
		while ((strLine = br.readLine()) != null && !strLine.equals(""))   {
			parts = strLine.split(": ");
			int vertex = Integer.parseInt(parts[0]);
			if (parts.length > 1) {
				parts = parts[1].split(" +");
				int[] vertices = new int[parts.length];
				for (int i = 0; i < parts.length; i++) {
					vertices[i] = Integer.valueOf(parts[i]);
				}
				adjacencyList[vertex] = vertices;
			}
			else {
				adjacencyList[vertex] = new int[0];
			}
		}
		br.close();
		return adjacencyList;
	}
}