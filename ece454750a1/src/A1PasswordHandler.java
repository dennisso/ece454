import org.apache.thrift.TException;
import org.mindrot.jbcrypt.BCrypt;

import ece454750s15a1.*;

public class A1PasswordHandler implements A1Password.Iface {
	public A1PasswordHandler() {
	}

	public String hashPassword(String password, short logRounds) throws ServiceUnavailableException {
		try {
			//System.out.println("received hash request");
			Server.statistics.incrementRequestsReceived();
			String result = BCrypt.hashpw(password, BCrypt.gensalt(logRounds));
			Server.statistics.incrementRequestsCompleted();
			return result;
		} catch (Exception e) {
			throw new ServiceUnavailableException(e.getMessage());
		}
	}

	public boolean checkPassword(String password, String hash) throws ServiceUnavailableException {
		try {
			Server.statistics.incrementRequestsReceived();
			boolean result = BCrypt.checkpw(password, hash);
			Server.statistics.incrementRequestsCompleted();
			return result;
		} catch (Exception e) {
			throw new ServiceUnavailableException(e.getMessage());
		}
	}
}

