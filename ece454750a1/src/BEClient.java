import ece454750s15a1.*;
import java.util.*;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;

public class BEClient {
	public static CommandLineArgs cla;

	public static void main(String[] args) {
		cla = new CommandLineArgs(args);

		try {
			TTransport mTransport; // transport for communication with Management service
			TTransport pTransport; // transport for communication with Management service

			//System.out.printf("Starting client -- mport:%d, pport:%d\n", cla.getMPort(), cla.getPPort());
			pTransport = new TFramedTransport(new TSocket("localhost", cla.getPPort()));
			mTransport = new TFramedTransport(new TSocket("localhost", cla.getMPort()));
			pTransport.open();
			mTransport.open();
			//System.out.printf("transport opened\n");
			TProtocol pProtocol = new TBinaryProtocol(pTransport);
			TProtocol mProtocol = new TBinaryProtocol(mTransport);
			A1Password.Client passClient = new A1Password.Client(pProtocol);
			A1Management.Client manClient = new A1Management.Client(mProtocol);

			perform(passClient);
			getNames(manClient);
			pTransport.close();
			mTransport.close();
		} catch (TException e) {
			e.printStackTrace();
		}
	}

	private static void perform(A1Password.Client client) throws TException {
		String pass = "abcde";
		String hash = client.hashPassword(pass, (short) 12);
		System.out.println("Hash: " + hash);

		boolean matches = client.checkPassword(pass, hash);
		System.out.println("Matches: " + matches);
	}

	private static void getNames(A1Management.Client client) throws TException {
		List<String> names = client.getGroupMembers();
		for (String name : names) {
			System.out.println(name);
		}

		System.out.println(client.getPerfCounters());
	}
}




