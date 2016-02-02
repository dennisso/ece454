import ece454750s15a1.A1Management;
import ece454750s15a1.A1Password;
import ece454750s15a1.NodeType;
import org.apache.thrift.TProcessor;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.*;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Server extends Object {
	public String ip;
	public Integer pport;
	public Integer mport;
	public Integer ncore;
	public NodeType nodeType;

	public static CommandLineArgs cla;

	public static A1PasswordHandler passHandler;
	public static A1Password.Processor passProcessor;

	public static A1ManagementHandler manHandler;
	public static A1Management.Processor manProcessor;
	public static Statistics statistics = new Statistics();

	public static ConcurrentMap<String, Server> feServers = new ConcurrentHashMap<String, Server>();
	public static ConcurrentMap<String, Server> beServers = new ConcurrentHashMap<String, Server>();
	public static ConcurrentMap<String, Server> seedServers = new ConcurrentHashMap<String, Server>();

	public Server() {
		this("localhost", 0, 0);
	}

	public Server(String ip, Integer mport, Integer pport) {
		this.ip = ip;
		this.mport = mport;
		this.pport = pport;
	}

	public Server(String ip, Integer mport, Integer pport, NodeType nodeType) {
		this(ip, mport, pport);
		this.nodeType = nodeType;
	}

	public Server(String ip, Integer mport, Integer pport, Integer ncore) {
		this(ip, mport, pport);
		this.ncore = ncore;
	}

	public Server(String ip, Integer mport, Integer pport, Integer ncore, NodeType nodeType) {
		this(ip, mport, pport, ncore);
		this.nodeType = nodeType;
	}


	@Override
	public String toString() {
		return this.ip + "_" + this.mport;
	}

	@Override
	public boolean equals(Object that) {
		if (that == null) {
			return false;
		}
		if (that instanceof Server) {
			Server o = (Server) that;
			return (this.ip.equals(o.ip)
					&& this.mport.equals(o.mport));
		}
		return false;
	}

	public static void addToFeServers(String ip, Integer mport, Integer pport) {
		Server server = new Server(ip, mport, pport, NodeType.FE);
		feServers.putIfAbsent(server.toString(), server);
	}

	public static void addToFeServers(String ip, Integer mport, Integer pport, Integer ncore) {
		Server server = new Server(ip, mport, pport, ncore, NodeType.FE);
		feServers.putIfAbsent(server.toString(), server);
	}

	public static void removeFromFeServers(String ip, Integer mport, Integer pport) {
		Server server = new Server(ip, mport, pport, NodeType.FE);
		feServers.remove(server.toString(), server);
	}

	public static void addToFeServers(Map<String, Integer> seedMap) {
		for (Map.Entry<String, Integer> entry : seedMap.entrySet()) {
			Server server = new Server(entry.getKey(), entry.getValue(), 0, NodeType.FE);
			feServers.putIfAbsent(server.toString(), server);
		}
	}

	public static void removeFromFeServers(Map<String, Integer> seedMap) {
		for (Map.Entry<String, Integer> entry : seedMap.entrySet()) {
			Server server = new Server(entry.getKey(), entry.getValue(), 0, NodeType.FE);
			feServers.remove(server.toString(), server);
		}
	}

	public static void addToBeServers(String ip, Integer mport, Integer pport) {
		Server server = new Server(ip, mport, pport, NodeType.BE);
		beServers.putIfAbsent(server.toString(), server);
	}

	public static void addToSeedServers(Map<String, Integer> seedMap) {
		for (Map.Entry<String, Integer> entry : seedMap.entrySet()) {
			Server server = new Server(entry.getKey(), entry.getValue(), 0, NodeType.FE);
			seedServers.putIfAbsent(server.toString(), server);
		}
	}

	public static void addToBeServers(String ip, Integer mport, Integer pport, Integer ncore) {
		Server server = new Server(ip, mport, pport, ncore, NodeType.BE);
		beServers.putIfAbsent(server.toString(), server);
	}

	public static void removeFromBeServers(String ip, Integer mport, Integer pport) {
		Server server = new Server(ip, mport, pport);
		beServers.remove(server.toString(), server);
	}

	public static void addToBeServers(Map<String, Integer> seedMap) {
		for (Map.Entry<String, Integer> entry : seedMap.entrySet()) {
			Server server = new Server(entry.getKey(), entry.getValue(), 0);
			beServers.putIfAbsent(server.toString(), server);
		}
	}

	public static void removeBeServers(Map<String, Integer> seedMap) {
		for (Map.Entry<String, Integer> entry : seedMap.entrySet()) {
			Server server = new Server(entry.getKey(), entry.getValue(), 0);
			beServers.remove(server.toString(), server);
		}
	}

	public static void simple(TProcessor proc, Integer socket) {
		try {
			TServerTransport serverTransport = new TServerSocket(socket);
			TServer server = new TSimpleServer(
					new TServer.Args(serverTransport).processor(proc));
			server.serve();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void hsha(TProcessor proc, Integer sock) {
		try {
			TNonblockingServerSocket socket = new TNonblockingServerSocket(sock);
			THsHaServer.Args arg = new THsHaServer.Args(socket);
			arg.protocolFactory(new TBinaryProtocol.Factory());
			arg.transportFactory(new TFramedTransport.Factory());
			arg.processorFactory(new TProcessorFactory(proc));
			arg.workerThreads(5);

			TServer server = new THsHaServer(arg);
			server.serve();
		} catch (TTransportException e) {
			e.printStackTrace();
		}
	}

	public static void threadedSelectorServer(TProcessor processor, Integer socket) {
		try {
			TNonblockingServerTransport trans = new TNonblockingServerSocket(socket);
			TThreadedSelectorServer.Args args = new TThreadedSelectorServer.Args(trans);
			args.transportFactory(new TFramedTransport.Factory());
			args.protocolFactory(new TBinaryProtocol.Factory());
			args.processor(processor);
			args.selectorThreads(4);
			args.workerThreads(32);
			TServer server = new TThreadedSelectorServer(args);
			server.serve();
		} catch (TTransportException e) {
			e.printStackTrace();
		}
	}
}
