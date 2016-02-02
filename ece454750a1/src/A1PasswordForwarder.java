import org.apache.thrift.TException;
import org.apache.thrift.transport.*;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;

import ece454750s15a1.*;

import java.util.HashMap;
import java.util.Map;

public class A1PasswordForwarder implements A1Password.Iface {
	public TTransport transport;

	String ownIp;
	Integer ownMport;
	Integer ownPport;
	Integer ownNcore;
	NodeType ownNodeType;

	public A1PasswordForwarder() {
		this("", 0, 0, null, Runtime.getRuntime().availableProcessors());
	}

	public A1PasswordForwarder(String ownIp, Integer ownMport, Integer ownPport, NodeType ownNodeType, Integer ownNcore) {
		this.ownIp = ownIp;
		this.ownMport = ownMport;
		this.ownPport = ownPport;
		this.ownNodeType = ownNodeType;
		this.ownNcore = ownNcore;
	}

	public String hashPassword(String password, short logRounds) throws ServiceUnavailableException {
		try {
			// establish BE client connection
			if (FEServer.selectedBackendServer == null ||
					FEServer.selectedBackendServer.ip.isEmpty() ||
					FEServer.selectedBackendServer.pport.equals(0)) {
				throw new ServiceUnavailableException("BE not selected");
			}
			String ip = FEServer.selectedBackendServer.ip;
			Integer pport = FEServer.selectedBackendServer.pport;

			transport = new TFramedTransport(new TSocket(ip, pport));
			transport.open();
			//System.out.println("hashPassword() : connected to " + ip + " " + pport);
			TProtocol protocol = new TBinaryProtocol(transport);
			A1Password.Client client = new A1Password.Client(protocol);

			Server.statistics.incrementRequestsReceived();
			//System.out.println("hashPassword(): received request");
			String hash = client.hashPassword(password, logRounds);
			//System.out.println("hashPassword(): " + hash);
			Server.statistics.incrementRequestsCompleted();

			// close client connection
			transport.close();

			return hash;
		} catch (Exception e) {
			gossipOnFailure();
			throw new ServiceUnavailableException(e.getMessage());
		}
	}

	public void gossipOnFailure() throws ServiceUnavailableException {
		//System.out.println("gossipOnFailure()");
		TTransport transport = null;
		String ip = "";
		Integer mport = 0;
		Integer pport = 0;
		Integer ncore = 0;
		for (Map.Entry<String, Server> seed : Server.seedServers.entrySet()) {
			try {
				ip = seed.getValue().ip;
				mport = seed.getValue().mport;
				pport = seed.getValue().pport;
				ncore = seed.getValue().ncore;
				transport = new TFramedTransport(new TSocket(ip, mport));
				transport.open();
				//System.out.println("run(): gossiping to " + seed.getValue().ip + " " + seed.getValue().mport);
				TBinaryProtocol protocol = new TBinaryProtocol(transport);
				A1Management.Client client = new A1Management.Client(protocol);
				// gossip about itself
				Map<String, String> msg = new HashMap<String, String>();

				msg.put("ip", FEServer.selectedBackendServer.ip);
				msg.put("mport", FEServer.selectedBackendServer.mport.toString());
				msg.put("pport", FEServer.selectedBackendServer.pport.toString());
				msg.put("nodetype", NodeType.BE.toString());
				msg.put("ncore", FEServer.selectedBackendServer.ncore.toString());

				// add source event information
				msg.put("src_ip", ownIp);
				msg.put("src_mport", ownMport.toString());
				msg.put("src_pport", ownPport.toString());
				msg.put("src_nodetype", ownNodeType.toString());

				Event event = new Event(MessageType.DEAD, msg, 0);

				client.gossip(event);
			} catch (Exception e) {
			}

		}
	}

	public void gossipOnFEFailure(String ip, Integer mport, Integer pport, Integer ncore) throws ServiceUnavailableException {
		//System.out.println("gossipOnFEFailure()");
		TTransport transport = null;
		try {
			for (Map.Entry<String, Server> entry : Server.feServers.entrySet()) {
				if (entry.getValue().ip.equals(ip) && entry.getValue().mport.equals(mport)) {
					// skip dead node
					continue;
				}
				transport = new TFramedTransport(new TSocket(entry.getValue().ip, entry.getValue().mport));
				transport.open();
				//System.out.println("run(): gossiping to " + entry.getValue().ip + " " + entry.getValue().mport);
				TBinaryProtocol protocol = new TBinaryProtocol(transport);
				A1Management.Client client = new A1Management.Client(protocol);
				// gossip about itself
				Map<String, String> msg = new HashMap<String, String>();

				msg.put("ip", ip);
				msg.put("mport", mport.toString());
				msg.put("pport", pport.toString());
				msg.put("nodetype", NodeType.FE.toString());
				msg.put("ncore", ncore.toString());

				// add source event information
				msg.put("src_ip", ownIp);
				msg.put("src_mport", ownMport.toString());
				msg.put("src_pport", ownPport.toString());
				msg.put("src_nodetype", ownNodeType.toString());

				Event event = new Event(MessageType.DEAD, msg, 0);

				client.gossip(event);
			}
		} catch (Exception ex) {
			throw new ServiceUnavailableException(ex.getMessage());
		}
	}

	public boolean checkPassword(String password, String hash) throws ServiceUnavailableException {
		try {
			// establish BE client connection
			if (FEServer.selectedBackendServer == null ||
					FEServer.selectedBackendServer.ip.isEmpty() ||
					FEServer.selectedBackendServer.pport.equals(0)) {
				throw new ServiceUnavailableException("BE not selected");
			}
			String ip = FEServer.selectedBackendServer.ip;
			Integer pport = FEServer.selectedBackendServer.pport;

			transport = new TFramedTransport(new TSocket(ip, pport));
			transport.open();
			//System.out.println("hashPassword() : connected to " + ip + " " + pport);
			TProtocol protocol = new TBinaryProtocol(transport);
			A1Password.Client client = new A1Password.Client(protocol);

			Server.statistics.incrementRequestsReceived();
			//System.out.println("checkPassword(): received request");
			boolean matches = client.checkPassword(password, hash);
			Server.statistics.incrementRequestsCompleted();

			// close client connection
			transport.close();

			return matches;
		} catch (Exception e) {
			throw new ServiceUnavailableException(e.getMessage());
		}
	}
}

