import ece454750s15a1.*;

import org.apache.thrift.TException;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.*;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import org.apache.thrift.TProcessor;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class BEServer extends Server {
	public static void main(String[] args) {
		cla = new CommandLineArgs(args);
		//System.out.printf("Starting backend server -- mport:%d, pport:%d\n", cla.getMPort(), cla.getPPort());

		// add all the seeds to the feservers
		addToFeServers(cla.getSeeds());
		addToSeedServers(cla.getSeeds());
		//System.out.printf("seeds: %s\n", feServers.toString());

		// add itself to the beservers
		addToBeServers(cla.getHost(), cla.getMPort(), cla.getPPort());

		try {
			// process request using bcrypt
			passHandler = new A1PasswordHandler();
			passProcessor = new A1Password.Processor(passHandler);
			manHandler = new A1ManagementHandler(cla.getHost(), cla.getMPort(), cla.getPPort(), NodeType.BE, cla.getNCores());
			manProcessor = new A1Management.Processor(manHandler);

			Runnable passRun = new Runnable() {
				public void run() {
					threadedSelectorServer(passProcessor, cla.getPPort());
				}
			};
			Runnable manRun = new Runnable() {
				public void run() {
					TTransport transport = null;
					try {

						for (Map.Entry<String, Server> entry : feServers.entrySet()) {
							transport = new TFramedTransport(new TSocket(entry.getValue().ip, entry.getValue().mport));
							transport.open();
							//System.out.println("run(): gossiping to " + entry.getValue().ip + " " + entry.getValue().mport);
							TBinaryProtocol protocol = new TBinaryProtocol(transport);
							A1Management.Client client = new A1Management.Client(protocol);

							Map<String, String> msg = new HashMap<String, String>();

							msg.put("ip", cla.getHost());
							msg.put("mport", cla.getMPort().toString());
							msg.put("pport", cla.getPPort().toString());
							msg.put("nodetype", NodeType.BE.toString());
							msg.put("ncore", cla.getNCores().toString());

							// add source event information
							msg.put("src_ip", cla.getHost());
							msg.put("src_mport", cla.getMPort().toString());
							msg.put("src_pport", cla.getPPort().toString());
							msg.put("src_nodetype", NodeType.BE.toString());

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

			new Thread(passRun).start();
			new Thread(manRun).start();

			while (true) {
				//Thread.sleep(10); // testing only

				//System.out.printf("feservers: %s\n", feServers.toString());
				//System.out.printf("beservers: %s\n", beServers.toString());
				//System.out.println(statistics);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}













