import ece454750s15a1.*;

import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.*;

import org.apache.thrift.TException;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.TProcessor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class FEServer extends Server {
	public static CommandLineArgs cla;
	public static Server selectedBackendServer;
	public static A1PasswordForwarder passForwarder;
	public static A1Password.Processor passProcessor;
	public static A1ManagementHandler manHandler;
	public static A1Management.Processor manProcessor;

	public static void main(String[] args) {
		cla = new CommandLineArgs(args);
		//System.out.printf("Starting frontend server -- mport:%d, pport:%d\n", cla.getMPort(), cla.getPPort());
		// add itself to the feservers
		addToFeServers(cla.getHost(), cla.getMPort(), cla.getPPort());
		// add all the seeds to the feservers
		addToFeServers(cla.getSeeds());
		addToSeedServers(cla.getSeeds());

		//System.out.printf("seeds with itself: %s\n", feServers.toString());

		try {
			manHandler = new A1ManagementHandler(cla.getHost(), cla.getMPort(), cla.getPPort(), NodeType.FE, cla.getNCores());
			manProcessor = new A1Management.Processor(manHandler);

			Runnable manRun = new Runnable() {
				public void run() {
					TTransport transport = null;
					try {
						for (Map.Entry<String, Server> entry : feServers.entrySet()) {
							if (entry.getValue().ip.equals(cla.getHost()) && entry.getValue().mport.equals(cla.getMPort()))
								continue;

							transport = new TFramedTransport(new TSocket(entry.getValue().ip, entry.getValue().mport));
							transport.open();
							//System.out.println("run(): gossiping to " + entry.getValue().ip + " " + entry.getValue().mport);
							TBinaryProtocol protocol = new TBinaryProtocol(transport);
							A1Management.Client client = new A1Management.Client(protocol);
							// gossip about itself
							Map<String, String> msg = new HashMap<String, String>();

							msg.put("ip", cla.getHost());
							msg.put("mport", cla.getMPort().toString());
							msg.put("pport", cla.getPPort().toString());
							msg.put("nodetype", NodeType.FE.toString());
							msg.put("ncore", cla.getNCores().toString());

							// add source event information
							msg.put("src_ip", cla.getHost());
							msg.put("src_mport", cla.getMPort().toString());
							msg.put("src_pport", cla.getPPort().toString());
							msg.put("src_nodetype", NodeType.FE.toString());

							Event e = new Event(MessageType.ARRIVAL, msg, 0);

							client.gossip(e);
						}

					} catch (TException e) {
						e.printStackTrace();
					} finally {
						if (transport != null) {
							transport.close();
						}
					}
					threadedSelectorServer(manProcessor, cla.getMPort());
				}
			};

			new Thread(manRun).start();

			startPasswordForwardServer();

			while(true) {
				//Thread.sleep(10); // testing only

				//System.out.printf("feservers: %s\n", feServers.toString());
				//System.out.printf("beservers: %s\n", beServers.toString());
				//System.out.println(statistics);

				// switching between BE client connections
				selectedBackendServer = selectBE();
				//System.out.println(selectedBackendServer);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// weighted RV using list of ncores
	// http://stackoverflow.com/questions/6737283/weighted-randomness-in-java
	public static Server selectBE() {
		if (beServers.isEmpty()) {
			return null;
		}
		else {
			Integer totalWeight = 0;
			for (Map.Entry<String, Server> entry : beServers.entrySet()) {
				totalWeight += entry.getValue().ncore;
			}
			Server randomServer = null;
			double random = Math.random() * totalWeight;
			for (Map.Entry<String, Server> entry : beServers.entrySet()) {
				random -= entry.getValue().ncore;
				if (random <= 0.0d) {
					randomServer = entry.getValue();
					break;
				}
			}
			return randomServer;
		}
	}

	public static void startPasswordForwardServer() {
		try {
			passForwarder = new A1PasswordForwarder(cla.getHost(), cla.getMPort(), cla.getPPort(), NodeType.FE, cla.getNCores());
			passProcessor = new A1Password.Processor(passForwarder);

			Runnable passRun = new Runnable() {
				public void run() {
					if (passProcessor != null) {
						threadedSelectorServer(passProcessor, cla.getPPort());
					}
				}
			};
			new Thread(passRun).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}




