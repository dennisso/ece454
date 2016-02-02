import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.mindrot.jbcrypt.BCrypt;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ece454750s15a1.*;

public class A1ManagementHandler implements A1Management.Iface {
	String ownIp;
	Integer ownMport;
	Integer ownPport;
	Integer ownNcore;
	NodeType ownNodeType;

	public A1ManagementHandler() {
		this("", 0, 0, null, Runtime.getRuntime().availableProcessors());
	}

	public A1ManagementHandler(String ownIp, Integer ownMport, Integer ownPport, NodeType ownNodeType, Integer ownNcore) {
		this.ownIp = ownIp;
		this.ownMport = ownMport;
		this.ownPport = ownPport;
		this.ownNodeType = ownNodeType;
		this.ownNcore = ownNcore;
	}

	public List<String> getGroupMembers() {
		return Arrays.asList("Blaise Leskowsky:20434269", "Lai Kit So:20432346");
	}

	public PerfCounters getPerfCounters() {
		return Server.statistics.getPerfCounters();
	}

	public void gossip(Event e) {
		try {
			//System.out.println("gossip(): Received " + e.toString());

			String msgIp = e.getMsg().get("ip");
			int msgMport = Integer.parseInt(e.getMsg().get("mport"));
			int msgPport = Integer.parseInt((e.getMsg().get("pport")));
			int msgNcore = Integer.parseInt(e.getMsg().get("ncore"));
			NodeType msgNodeType = NodeType.valueOf(e.getMsg().get("nodetype"));

			String srcIp = e.getMsg().get("src_ip");
			int srcMport = Integer.parseInt(e.getMsg().get("src_mport"));
			int srcPport = Integer.parseInt((e.getMsg().get("src_pport")));
			NodeType srcNodeType = NodeType.valueOf(e.getMsg().get("src_nodetype"));

			if (e.getStatus() == MessageType.ARRIVAL) {
				if (msgNodeType.equals(NodeType.BE) && Server.beServers.containsValue(new Server(msgIp, msgMport, msgPport, msgNcore))) {
					return;
				} else if (msgNodeType.equals(NodeType.FE) && Server.feServers.containsValue(new Server(msgIp, msgMport, msgPport, msgNcore))) {
					return;
				}
			}

			if (e.getStatus() == MessageType.ARRIVAL) {
				if (msgNodeType.equals(NodeType.BE)) {
					Server.addToBeServers(msgIp, msgMport, msgPport, msgNcore);
				} else if (msgNodeType.equals(NodeType.FE)) {
					Server.addToFeServers(msgIp, msgMport, msgPport, msgNcore);
				}
			} else if (e.getStatus() == MessageType.DEAD) {
				if (msgNodeType.equals(NodeType.BE)) {
					Server.removeFromBeServers(msgIp, msgMport, msgPport);
				} else if (msgNodeType.equals(NodeType.FE)) {
					Server.removeFromFeServers(msgIp, msgMport, msgPport);
				}
			}

			// gossip to known FEs and BEs
			ConcurrentMap<String, Server> servers = new ConcurrentHashMap<String, Server>();
			servers.putAll(Server.feServers);
			servers.putAll(Server.beServers);

			for (Map.Entry<String, Server> entry : servers.entrySet()) {
				if ((entry.getValue().ip.equals(ownIp) && entry.getValue().mport.equals(ownMport)) ||
						(entry.getValue().ip.equals(srcIp)) && entry.getValue().mport.equals(srcMport))
					continue;

				TTransport transport = null;
				try {
					//System.out.printf("gossip(): trying to gossip to %s:%d\n", entry.getValue().ip, entry.getValue().mport);
					transport = new TFramedTransport(new TSocket(entry.getValue().ip, entry.getValue().mport));
					transport.open();
					TProtocol protocol = new TBinaryProtocol(transport);
					A1Management.Client manClient = new A1Management.Client(protocol);

					// update src_ip, src_mport, src__pport, src_nodetype
					e.getMsg().put("src_ip", ownIp);
					e.getMsg().put("src_mport", ownMport.toString());
					e.getMsg().put("src_pport", ownPport.toString());
					e.getMsg().put("src_nodetype", ownNodeType.toString());

					manClient.gossip(e);

					// gossip about itself
					Map<String, String> selfMsg = new HashMap<String, String>();
					selfMsg.put("ip", ownIp);
					selfMsg.put("src_ip", ownIp);
					selfMsg.put("mport", ownMport.toString());
					selfMsg.put("src_mport", ownMport.toString());
					selfMsg.put("pport", ownPport.toString());
					selfMsg.put("src_pport", ownPport.toString());
					selfMsg.put("nodetype", ownNodeType.toString());
					selfMsg.put("src_nodetype", ownNodeType.toString());
					selfMsg.put("ncore", ownNcore.toString());

					Event selfEvent = new Event(MessageType.ARRIVAL, selfMsg, 0);
					manClient.gossip(selfEvent);
					// might need to remove
					//Thread.sleep(25);
					//System.out.println("gossip(): finished gossip");
				} catch (Exception ex) {
					//ex.printStackTrace();
					for (Map.Entry<String, Server> seed : Server.seedServers.entrySet()) {
						try {
							transport = new TFramedTransport(new TSocket(seed.getValue().ip, seed.getValue().mport));
							transport.open();
							//System.out.println("gossip exception(): gossiping to " + entry.getValue().ip + " " + entry.getValue().mport);
							TBinaryProtocol protocol = new TBinaryProtocol(transport);
							A1Management.Client client = new A1Management.Client(protocol);
							// gossip about itself
							Map<String, String> msg = new HashMap<String, String>();

							msg.put("ip", entry.getValue().ip);
							msg.put("mport", entry.getValue().mport.toString());
							msg.put("pport", entry.getValue().pport.toString());
							msg.put("nodetype", entry.getValue().nodeType.toString());
							msg.put("ncore", entry.getValue().ncore.toString());

							// add source event information
							msg.put("src_ip", ownIp);
							msg.put("src_mport", ownMport.toString());
							msg.put("src_pport", ownPport.toString());
							msg.put("src_nodetype", ownNodeType.toString());
							Event event = new Event(MessageType.DEAD, msg, 0);
							//System.out.printf("send dead event %s\n", event.toString());
							client.gossip(event);
						} catch (Exception exc) {
						}
					}
				} finally {
					if (transport != null) {
						transport.close();
					}
				}
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}

